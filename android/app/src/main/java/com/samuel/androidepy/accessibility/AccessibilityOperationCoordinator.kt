package com.samuel.androidepy.accessibility

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal data class AccessibilityOperationRequest(
    val requestId: String,
    val operation: String,
    val payload: JSONObject,
    val timeoutMs: Long
)

/**
 * Serializes native operations and keeps the Android service asynchronous.
 * Only the existing Python worker may wait for execute() to return.
 */
internal class AccessibilityOperationCoordinator {

    private data class Pending(
        val service: AndroidePyAccessibilityService,
        val request: AccessibilityOperationRequest,
        val future: CompletableFuture<JSONObject> = CompletableFuture()
    )

    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<Pending>()
    private var active: Pending? = null

    fun execute(
        service: AndroidePyAccessibilityService,
        request: AccessibilityOperationRequest
    ): JSONObject {
        check(Looper.myLooper() != Looper.getMainLooper()) {
            "Accessibility operations must not wait on the Android main thread."
        }

        val pending = Pending(service, request)
        val deadline = SystemClock.elapsedRealtime() + request.timeoutMs

        synchronized(lock) {
            if (queue.size + (if (active == null) 0 else 1) >= MAX_PENDING) {
                throw operationError(
                    AccessibilityErrorCode.QUEUE_FULL,
                    "A fila de operações de acessibilidade está cheia.",
                    request.requestId
                )
            }
            queue.addLast(pending)
            scheduleNextLocked()
        }

        return try {
            val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1L)
            pending.future.get(remaining, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            timeout(pending)
            throw operationError(
                AccessibilityErrorCode.TIMEOUT,
                "O prazo da operação de acessibilidade expirou.",
                request.requestId
            )
        } catch (interrupted: InterruptedException) {
            cancel(pending)
            Thread.currentThread().interrupt()
            throw operationError(
                AccessibilityErrorCode.CANCELLED,
                "A espera pela operação foi interrompida.",
                request.requestId
            )
        } catch (failure: ExecutionException) {
            val cause = failure.cause
            if (cause is AccessibilityOperationException) throw cause
            throw operationError(
                AccessibilityErrorCode.INTERNAL_ERROR,
                "Não foi possível concluir a operação de acessibilidade.",
                request.requestId
            )
        }
    }

    fun failForService(
        service: AndroidePyAccessibilityService,
        error: AccessibilityOperationException
    ) {
        synchronized(lock) {
            val current = active
            if (current?.service === service) {
                active = null
                current.future.completeExceptionally(error.withRequestId(current.request.requestId))
            }

            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val pending = iterator.next()
                if (pending.service === service) {
                    iterator.remove()
                    pending.future.completeExceptionally(
                        error.withRequestId(pending.request.requestId)
                    )
                }
            }
            scheduleNextLocked()
        }
    }

    private fun timeout(pending: Pending) {
        synchronized(lock) {
            if (queue.remove(pending)) {
                pending.future.completeExceptionally(
                    operationError(
                        AccessibilityErrorCode.TIMEOUT,
                        "O prazo da operação de acessibilidade expirou.",
                        pending.request.requestId
                    )
                )
            } else if (active === pending) {
                // A dispatched Android action cannot always be rolled back.
                // Keep the active slot until its callback or service teardown.
                pending.future.completeExceptionally(
                    operationError(
                        AccessibilityErrorCode.TIMEOUT,
                        "O prazo da operação de acessibilidade expirou.",
                        pending.request.requestId
                    )
                )
            }
        }
    }

    private fun cancel(pending: Pending) {
        synchronized(lock) {
            if (queue.remove(pending)) {
                pending.future.completeExceptionally(
                    operationError(
                        AccessibilityErrorCode.CANCELLED,
                        "A operação foi cancelada.",
                        pending.request.requestId
                    )
                )
            } else if (active === pending) {
                pending.future.completeExceptionally(
                    operationError(
                        AccessibilityErrorCode.CANCELLED,
                        "A operação foi cancelada.",
                        pending.request.requestId
                    )
                )
            }
        }
    }

    private fun scheduleNextLocked() {
        if (active != null) return
        val next = queue.pollFirst() ?: return
        active = next

        val accepted = mainHandler.post {
            val shouldDispatch = synchronized(lock) {
                if (active !== next) {
                    false
                } else if (next.future.isDone) {
                    active = null
                    scheduleNextLocked()
                    false
                } else {
                    true
                }
            }

            if (shouldDispatch) {
                next.service.dispatchAccessibilityOperation(next.request) { result, error ->
                    finish(next, result, error)
                }
            }
        }

        if (!accepted) {
            active = null
            next.future.completeExceptionally(
                operationError(
                    AccessibilityErrorCode.SERVICE_DISCONNECTED,
                    "O serviço de acessibilidade não está disponível.",
                    next.request.requestId
                )
            )
            scheduleNextLocked()
        }
    }

    private fun finish(
        pending: Pending,
        result: JSONObject?,
        error: AccessibilityOperationException?
    ) {
        synchronized(lock) {
            if (active !== pending) return
            active = null

            if (error != null) {
                pending.future.completeExceptionally(error.withRequestId(pending.request.requestId))
            } else {
                pending.future.complete(result ?: JSONObject())
            }
            scheduleNextLocked()
        }
    }

    private fun AccessibilityOperationException.withRequestId(id: String) =
        AccessibilityOperationException(code, message, id)

    private fun operationError(
        code: AccessibilityErrorCode,
        message: String,
        requestId: String
    ) = AccessibilityOperationException(code, message, requestId)

    private companion object {
        const val MAX_PENDING = 16
    }
}
