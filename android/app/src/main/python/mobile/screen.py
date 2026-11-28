"""Synchronous public API for Android screen interaction."""

from __future__ import annotations

import json
import math
import uuid

from mobile import _get_bridge


class ScreenError(RuntimeError):
    """Stable operational error returned by the native accessibility bridge."""

    def __init__(
        self,
        code: str,
        message: str,
        retryable: bool = False,
        request_id: str | None = None,
    ) -> None:
        super().__init__(message)
        self.code = code
        self.retryable = bool(retryable)
        self.request_id = request_id


_DEFAULT_TIMEOUT_MS = 10_000
_MIN_TIMEOUT_MS = 1
_MAX_TIMEOUT_MS = 120_000
_MAX_TEXT_CHARS = 10_000
_MAX_GESTURE_DURATION_MS = 10_000


def _integer(value, name: str, minimum: int, maximum: int) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise TypeError(f"{name} must be an integer.")
    if not minimum <= value <= maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}.")
    return value


def _number(value, name: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise TypeError(f"{name} must be a number.")
    result = float(value)
    if not math.isfinite(result):
        raise ValueError(f"{name} must be finite.")
    return result


def _timeout(timeout_ms: int) -> int:
    return _integer(timeout_ms, "timeout_ms", _MIN_TIMEOUT_MS, _MAX_TIMEOUT_MS)


def _coordinates(values, coordinate_space: str) -> tuple[float, ...]:
    if coordinate_space not in ("pixels", "normalized"):
        raise ValueError("coordinate_space must be 'pixels' or 'normalized'.")
    result = tuple(_number(value, "coordinate") for value in values)
    if any(value < 0 for value in result):
        raise ValueError("Coordinates cannot be negative.")
    if coordinate_space == "normalized" and any(value > 1 for value in result):
        raise ValueError("Normalized coordinates must be between 0 and 1.")
    return result


def _invoke(operation: str, payload: dict, timeout_ms: int) -> dict:
    bridge = _get_bridge()
    request_id = str(uuid.uuid4())
    request = {
        "schema_version": 1,
        "request_id": request_id,
        "operation": operation,
        "payload": payload,
        "timeout_ms": timeout_ms,
    }
    response = json.loads(
        bridge.executeAccessibilityOperationJson(
            json.dumps(request, ensure_ascii=False, separators=(",", ":"))
        )
    )
    if not isinstance(response, dict):
        raise ScreenError("INTERNAL_ERROR", "A Bridge retornou uma resposta inválida.", request_id=request_id)
    if not response.get("ok"):
        error = response.get("error") or {}
        raise ScreenError(
            code=str(error.get("code", "INTERNAL_ERROR")),
            message=str(error.get("message", "A operação de acessibilidade falhou.")),
            retryable=bool(error.get("retryable", False)),
            request_id=response.get("request_id") or request_id,
        )
    result = response.get("result")
    if not isinstance(result, dict):
        raise ScreenError("INTERNAL_ERROR", "A Bridge retornou um resultado inválido.", request_id=request_id)
    return result


def get_status() -> dict:
    """Return authorization and connection state without requesting access."""
    payload = json.loads(_get_bridge().getAccessibilityStatusJson())
    if not payload.get("ok"):
        error = payload.get("error") or {}
        raise ScreenError(
            str(error.get("code", "BRIDGE_NOT_CONFIGURED")),
            str(error.get("message", "A Bridge não está disponível.")),
        )
    return {
        "enabled": bool(payload["enabled"]),
        "connected": bool(payload["connected"]),
        "state": str(payload["state"]),
    }


def tap(
    x,
    y,
    *,
    coordinate_space: str = "pixels",
    timeout_ms: int = _DEFAULT_TIMEOUT_MS,
) -> dict:
    timeout = _timeout(timeout_ms)
    x_value, y_value = _coordinates((x, y), coordinate_space)
    return _invoke(
        "screen.tap",
        {"x": x_value, "y": y_value, "coordinate_space": coordinate_space},
        timeout,
    )


def type_text(text: str, *, timeout_ms: int = _DEFAULT_TIMEOUT_MS) -> dict:
    timeout = _timeout(timeout_ms)
    if not isinstance(text, str):
        raise TypeError("text must be a string.")
    if len(text) > _MAX_TEXT_CHARS:
        raise ValueError(f"text cannot exceed {_MAX_TEXT_CHARS} characters.")
    return _invoke("screen.type_text", {"text": text}, timeout)


def swipe(
    x1,
    y1,
    x2,
    y2,
    *,
    coordinate_space: str = "pixels",
    duration_ms: int = 350,
    timeout_ms: int = _DEFAULT_TIMEOUT_MS,
) -> dict:
    timeout = _timeout(timeout_ms)
    duration = _integer(duration_ms, "duration_ms", 1, _MAX_GESTURE_DURATION_MS)
    coordinates = _coordinates((x1, y1, x2, y2), coordinate_space)
    return _invoke(
        "screen.swipe",
        {
            "x1": coordinates[0],
            "y1": coordinates[1],
            "x2": coordinates[2],
            "y2": coordinates[3],
            "coordinate_space": coordinate_space,
            "duration_ms": duration,
        },
        timeout,
    )


def scroll(
    direction: str = "down",
    *,
    distance: float = 0.7,
    duration_ms: int = 350,
    timeout_ms: int = _DEFAULT_TIMEOUT_MS,
) -> dict:
    timeout = _timeout(timeout_ms)
    if direction not in ("up", "down", "left", "right"):
        raise ValueError("direction must be up, down, left, or right.")
    distance_value = _number(distance, "distance")
    if not 0 < distance_value <= 1:
        raise ValueError("distance must be greater than 0 and at most 1.")
    duration = _integer(duration_ms, "duration_ms", 1, _MAX_GESTURE_DURATION_MS)
    return _invoke(
        "screen.scroll",
        {"direction": direction, "distance": distance_value, "duration_ms": duration},
        timeout,
    )


_CAPTURE_MAX_DELAY_MS = 120_000
_CAPTURE_MIN_TIMEOUT_MS = 1_000
_CAPTURE_MAX_TIMEOUT_MS = 300_000


class CaptureHandle:
    """Opaque Python handle for a native coordinate-capture session."""

    def __init__(self, bridge, capture_id: str) -> None:
        self._bridge = bridge
        self.capture_id = capture_id

    def status(self) -> dict:
        """Return the current capture state without waiting."""
        try:
            response = json.loads(
                self._bridge.getCoordinateCaptureStatusJson(self.capture_id)
            )
        except AttributeError as error:
            raise ScreenError(
                "BRIDGE_NOT_CONFIGURED",
                "A Bridge não oferece suporte à consulta de capturas.",
            ) from error
        if not isinstance(response, dict) or not response.get("ok"):
            error = (response.get("error") or {}) if isinstance(response, dict) else {}
            raise ScreenError(
                str(error.get("code", "INTERNAL_ERROR")),
                str(error.get("message", "Não foi possível consultar a captura.")),
                bool(error.get("retryable", False)),
                self.capture_id,
            )
        result = response.get("result")
        if not isinstance(result, dict):
            raise ScreenError("INTERNAL_ERROR", "Estado de captura inválido.", request_id=self.capture_id)
        return result

    def result(self, *, timeout_ms: int | None = None) -> dict:
        """Wait explicitly for a terminal result on the Python worker."""
        timeout = None if timeout_ms is None else _integer(
            timeout_ms, "timeout_ms", _MIN_TIMEOUT_MS, _CAPTURE_MAX_TIMEOUT_MS
        )
        deadline = None if timeout is None else time.monotonic() + timeout / 1000
        while True:
            state = self.status()
            status = state.get("state")
            if status == "completed":
                result = state.get("result")
                if not isinstance(result, dict):
                    raise ScreenError("INTERNAL_ERROR", "A captura terminou sem mapa.", request_id=self.capture_id)
                return result
            if status in ("cancelled", "expired", "failed"):
                raise ScreenError(
                    str(state.get("error_code") or ("CANCELLED" if status == "cancelled" else "TIMEOUT" if status == "expired" else "INTERNAL_ERROR")),
                    str(state.get("message") or "A captura não foi concluída."),
                    request_id=self.capture_id,
                )
            if deadline is not None and time.monotonic() >= deadline:
                raise ScreenError("TIMEOUT", "O prazo local de espera pelo resultado expirou.", True, self.capture_id)
            time.sleep(0.1)

    def cancel(self) -> dict:
        """Request native cancellation; terminal handles remain idempotent."""
        try:
            response = json.loads(
                self._bridge.cancelCoordinateCaptureJson(self.capture_id)
            )
        except AttributeError as error:
            raise ScreenError(
                "BRIDGE_NOT_CONFIGURED",
                "A Bridge não oferece suporte ao cancelamento de capturas.",
            ) from error
        if not isinstance(response, dict) or not response.get("ok"):
            error = (response.get("error") or {}) if isinstance(response, dict) else {}
            raise ScreenError(
                str(error.get("code", "INTERNAL_ERROR")),
                str(error.get("message", "Não foi possível cancelar a captura.")),
                bool(error.get("retryable", False)),
                self.capture_id,
            )
        return response.get("result") or {"capture_id": self.capture_id, "cancelled": True}


def capture_coordinates(
    name: str,
    *,
    delay_ms: int = 30_000,
    timeout_ms: int = 120_000,
) -> CaptureHandle:
    """Start a native capture and return immediately with an asynchronous handle."""
    if not isinstance(name, str):
        raise TypeError("name must be a string.")
    normalized_name = name.strip()
    if not normalized_name:
        raise ValueError("name cannot be empty.")
    if len(normalized_name) > 80:
        raise ValueError("name cannot exceed 80 characters.")
    delay = _integer(delay_ms, "delay_ms", 0, _CAPTURE_MAX_DELAY_MS)
    timeout = _integer(
        timeout_ms, "timeout_ms", _CAPTURE_MIN_TIMEOUT_MS, _CAPTURE_MAX_TIMEOUT_MS
    )
    if delay >= timeout:
        raise ValueError("delay_ms must be shorter than timeout_ms.")
    bridge = _get_bridge()
    request_id = str(uuid.uuid4())
    request = {
        "schema_version": 1,
        "request_id": request_id,
        "name": normalized_name,
        "delay_ms": delay,
        "timeout_ms": timeout,
    }
    try:
        response = json.loads(
            bridge.startCoordinateCaptureJson(
                json.dumps(request, ensure_ascii=False, separators=(",", ":"))
            )
        )
    except AttributeError as error:
        raise ScreenError(
            "BRIDGE_NOT_CONFIGURED",
            "A Bridge não oferece suporte à captura de coordenadas.",
        ) from error
    if not isinstance(response, dict) or not response.get("ok"):
        error = (response.get("error") or {}) if isinstance(response, dict) else {}
        raise ScreenError(
            str(error.get("code", "INTERNAL_ERROR")),
            str(error.get("message", "Não foi possível iniciar a captura.")),
            bool(error.get("retryable", False)),
            response.get("request_id", request_id) if isinstance(response, dict) else request_id,
        )
    result = response.get("result") or {}
    capture_id = result.get("capture_id")
    if not isinstance(capture_id, str) or not capture_id:
        raise ScreenError("INTERNAL_ERROR", "A Bridge não retornou um identificador de captura.", request_id=request_id)
    return CaptureHandle(bridge, capture_id)
