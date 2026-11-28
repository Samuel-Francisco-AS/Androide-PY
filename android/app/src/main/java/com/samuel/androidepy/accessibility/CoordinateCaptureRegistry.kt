package com.samuel.androidepy.accessibility

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class CapturedCoordinatePoint(
    val name: String,
    val xPx: Int,
    val yPx: Int
)

/** Application-scoped, JSON-only rendezvous for asynchronous capture handles. */
class CoordinateCaptureRegistry {
    private data class Entry(
        val captureId: String,
        val name: String,
        var state: String = "scheduled",
        val points: MutableList<CapturedCoordinatePoint> = mutableListOf(),
        var result: JSONObject? = null,
        var errorCode: String? = null,
        var message: String? = null
    )

    private val lock = Any()
    private val entries = linkedMapOf<String, Entry>()

    internal fun create(name: String): String = synchronized(lock) {
        val id = UUID.randomUUID().toString()
        entries[id] = Entry(id, name)
        id
    }

    internal fun activate(id: String): Boolean = synchronized(lock) {
        val entry = entries[id] ?: return false
        if (entry.state != "scheduled") return false
        entry.state = "active"
        true
    }

    internal fun addPoint(id: String, name: String, x: Int, y: Int): Boolean =
        synchronized(lock) {
            val entry = entries[id] ?: return false
            if (entry.state != "active") return false
            val normalized = name.trim()
            if (normalized.isEmpty() || normalized.length > 80 ||
                entry.points.any { it.name.equals(normalized, ignoreCase = true) }
            ) return false
            entry.points += CapturedCoordinatePoint(normalized, x, y)
            true
        }

    internal fun removeLastPoint(id: String): Boolean = synchronized(lock) {
        val entry = entries[id] ?: return false
        if (entry.state != "active" || entry.points.isEmpty()) return false
        entry.points.removeAt(entry.points.lastIndex)
        true
    }

    internal fun points(id: String): List<CapturedCoordinatePoint> =
        synchronized(lock) { entries[id]?.points?.toList().orEmpty() }

    internal fun name(id: String): String? = synchronized(lock) { entries[id]?.name }

    internal fun complete(id: String, result: JSONObject) = synchronized(lock) {
        entries[id]?.let {
            if (it.state == "active") {
                it.state = "completed"
                it.result = JSONObject(result.toString())
            }
        }
    }

    internal fun fail(id: String, state: String, code: String, message: String) =
        synchronized(lock) {
            entries[id]?.let {
                if (it.state == "scheduled" || it.state == "active") {
                    it.state = state
                    it.errorCode = code
                    it.message = message
                }
            }
        }

    internal fun statusJson(id: String): String = synchronized(lock) {
        val entry = entries[id]
            ?: return JSONObject().put("ok", false).put(
                "error", JSONObject().put("code", "CAPTURE_NOT_FOUND")
                    .put("message", "A captura solicitada não existe.")
                    .put("retryable", false)
            ).toString()
        val result = JSONObject()
            .put("capture_id", entry.captureId)
            .put("name", entry.name)
            .put("state", entry.state)
            .put("point_count", entry.points.size)
        entry.result?.let { result.put("result", JSONObject(it.toString())) }
        entry.errorCode?.let { result.put("error_code", it) }
        entry.message?.let { result.put("message", it) }
        JSONObject().put("ok", true).put("result", result).toString()
    }

    internal fun cancelLocally(id: String): Boolean = synchronized(lock) {
        val entry = entries[id] ?: return false
        if (entry.state == "completed" || entry.state == "cancelled" ||
            entry.state == "expired" || entry.state == "failed"
        ) return true
        entry.state = "cancelled"
        entry.errorCode = "CANCELLED"
        entry.message = "A captura foi cancelada."
        entry.points.clear()
        true
    }

    internal fun isKnown(id: String): Boolean = synchronized(lock) { entries.containsKey(id) }

    internal fun summaryJson(items: List<JSONObject>): String =
        JSONObject().put("ok", true).put("result", JSONArray(items)).toString()
}
