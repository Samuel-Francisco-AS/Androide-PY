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
