"""Host tests for the public mobile.screen facade and local validation."""

import json
import unittest

from mobile import configure
from mobile.screen import (CaptureHandle, ScreenError, capture_coordinates, get_status, scroll, swipe, tap, type_text)


class FakeBridge:
    def __init__(self):
        self.requests = []
        self.status = {
            "schema_version": 1,
            "ok": True,
            "enabled": True,
            "connected": True,
            "state": "connected",
        }
        self.response = None
        self.capture_status = {"state": "active", "capture_id": "capture-1"}
        self.capture_start_request = None
        self.capture_cancelled = False

    def getAccessibilityStatusJson(self):
        return json.dumps(self.status)

    def startCoordinateCaptureJson(self, request_json):
        self.capture_start_request = json.loads(request_json)
        return json.dumps({
            "ok": True,
            "request_id": self.capture_start_request["request_id"],
            "result": {"capture_id": "capture-1", "state": "scheduled"},
        })

    def getCoordinateCaptureStatusJson(self, capture_id):
        return json.dumps({"ok": True, "result": dict(self.capture_status)})

    def cancelCoordinateCaptureJson(self, capture_id):
        self.capture_cancelled = True
        return json.dumps({"ok": True, "result": {"capture_id": capture_id, "cancelled": True}})

    def executeAccessibilityOperationJson(self, request_json):
        request = json.loads(request_json)
        self.requests.append(request)
        if self.response is not None:
            return json.dumps(self.response)
        return json.dumps({
            "schema_version": 1,
            "request_id": request["request_id"],
            "ok": True,
            "result": {
                "request_id": request["request_id"],
                "operation": request["operation"],
                "completed": True,
            },
            "error": None,
        })


class ScreenApiTests(unittest.TestCase):
    def setUp(self):
        self.bridge = FakeBridge()
        configure(self.bridge)

    def tearDown(self):
        configure(None)

    def test_get_status_distinguishes_authorization_and_connection(self):
        status = get_status()
        self.assertEqual(status, {
            "enabled": True,
            "connected": True,
            "state": "connected",
        })

        self.bridge.status.update(enabled=True, connected=False, state="disconnected")
        self.assertEqual(get_status()["state"], "disconnected")

    def test_tap_builds_versioned_request_and_returns_result(self):
        result = tap(120, 340, coordinate_space="pixels")
        request = self.bridge.requests[-1]
        self.assertEqual(request["schema_version"], 1)
        self.assertEqual(request["operation"], "screen.tap")
        self.assertEqual(request["payload"]["x"], 120.0)
        self.assertEqual(request["payload"]["y"], 340.0)
        self.assertEqual(request["timeout_ms"], 10_000)
        self.assertTrue(result["completed"])

    def test_normalized_coordinates_are_validated_locally(self):
        tap(0.5, 1.0, coordinate_space="normalized")
        self.assertEqual(self.bridge.requests[-1]["payload"]["coordinate_space"], "normalized")
        with self.assertRaises(ValueError):
            tap(1.01, 0.5, coordinate_space="normalized")

    def test_non_finite_and_boolean_coordinates_are_rejected(self):
        with self.assertRaises(ValueError):
            tap(float("nan"), 0)
        with self.assertRaises(TypeError):
            tap(True, 0)

    def test_type_text_preserves_unicode_without_logging_or_mutating_request(self):
        value = "Olá, mundo — ação 🚀"
        type_text(value)
        request = self.bridge.requests[-1]
        self.assertEqual(request["operation"], "screen.type_text")
        self.assertEqual(request["payload"]["text"], value)

    def test_swipe_and_scroll_validate_gesture_parameters(self):
        swipe(1, 2, 100, 200, duration_ms=700)
        self.assertEqual(self.bridge.requests[-1]["payload"]["duration_ms"], 700)

        scroll("left", distance=0.4)
        self.assertEqual(self.bridge.requests[-1]["payload"]["direction"], "left")

        with self.assertRaises(ValueError):
            scroll("diagonal")
        with self.assertRaises(ValueError):
            swipe(0, 0, 1, 1, duration_ms=0)

    def test_native_error_becomes_screen_error_with_stable_fields(self):
        self.bridge.response = {
            "schema_version": 1,
            "request_id": "request-123",
            "ok": False,
            "result": None,
            "error": {
                "code": "SERVICE_DISCONNECTED",
                "message": "Disconnected.",
                "retryable": True,
            },
        }
        with self.assertRaises(ScreenError) as raised:
            tap(1, 1)
        self.assertEqual(raised.exception.code, "SERVICE_DISCONNECTED")
        self.assertTrue(raised.exception.retryable)
        self.assertEqual(raised.exception.request_id, "request-123")

    def test_invalid_arguments_do_not_reach_native_bridge(self):
        with self.assertRaises(TypeError):
            type_text(42)
        with self.assertRaises(ValueError):
            tap(-1, 0)
        with self.assertRaises(ValueError):
            swipe(0, 0, 1, 1, timeout_ms=120_001)
        with self.assertRaises(ValueError):
            scroll("down", distance=0)
        self.assertEqual(self.bridge.requests, [])

    def test_unconfigured_bridge_fails_clearly(self):
        configure(None)
        with self.assertRaisesRegex(RuntimeError, "Android Bridge is not initialized"):
            get_status()


    def test_capture_coordinates_returns_handle_without_waiting(self):
        handle = capture_coordinates("  Principal  ", delay_ms=0, timeout_ms=5_000)
        self.assertIsInstance(handle, CaptureHandle)
        self.assertEqual(handle.capture_id, "capture-1")
        self.assertEqual(self.bridge.capture_start_request["name"], "Principal")
        self.assertEqual(self.bridge.capture_start_request["delay_ms"], 0)
        self.assertEqual(handle.status()["state"], "active")

    def test_capture_handle_result_returns_completed_map(self):
        self.bridge.capture_status = {
            "state": "completed",
            "capture_id": "capture-1",
            "result": {"name": "Principal", "points": [{"name": "home"}]},
        }
        handle = capture_coordinates("Principal", delay_ms=0)
        self.assertEqual(handle.result(timeout_ms=1_000)["name"], "Principal")

    def test_capture_handle_cancel_requests_native_cancellation(self):
        handle = capture_coordinates("Principal", delay_ms=0)
        result = handle.cancel()
        self.assertTrue(self.bridge.capture_cancelled)
        self.assertTrue(result["cancelled"])

    def test_capture_arguments_are_validated_before_bridge(self):
        with self.assertRaises(ValueError):
            capture_coordinates("  ")
        with self.assertRaises(ValueError):
            capture_coordinates("x", delay_ms=5_000, timeout_ms=5_000)
        with self.assertRaises(TypeError):
            capture_coordinates(42)
        self.assertIsNone(self.bridge.capture_start_request)


if __name__ == "__main__":
    unittest.main()
