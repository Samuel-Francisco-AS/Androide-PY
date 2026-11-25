"""Execute user-authored Python source and return a bounded JSON result."""

from __future__ import annotations

import contextlib
import io
import json
import time
import traceback
from typing import Any

MAX_CAPTURE_CHARS = 65_536
TRUNCATION_MARKER = "\\n[output truncated: 65536 character limit reached]"


class _LimitedCapture(io.StringIO):
    """StringIO which stores at most MAX_CAPTURE_CHARS characters."""

    def __init__(self) -> None:
        super().__init__()
        self.truncated = False

    def write(self, value: str) -> int:
        text = str(value)
        remaining = MAX_CAPTURE_CHARS - self.tell()
        if remaining > 0:
            super().write(text[:remaining])
        if len(text) > max(remaining, 0):
            self.truncated = True
        return len(text)

    def captured_text(self) -> str:
        value = super().getvalue()
        return value + TRUNCATION_MARKER if self.truncated else value


def _error_payload(error: BaseException) -> dict[str, str]:
    try:
        message = str(error)
    except BaseException:
        message = "The exception message could not be rendered."
    return {
        "type": type(error).__name__,
        "message": message,
        "traceback": traceback.format_exc(),
    }


def execute(
    source_code: str,
    execution_id: str,
    script_id: str = "scratch",
    parameters_json: str = "{}",
) -> str:
    """Execute a script with its input values exposed through the `params` dict."""
    started_at_ms = int(time.time() * 1000)
    stdout_capture = _LimitedCapture()
    stderr_capture = _LimitedCapture()
    error: dict[str, str] | None = None
    filename = f"<androide-py:{script_id}>"
    namespace: dict[str, Any] = {"__name__": "__main__", "__file__": filename}

    try:
        parameters = json.loads(parameters_json)
        if not isinstance(parameters, dict):
            raise ValueError("Script parameters must be a JSON object.")
        namespace["params"] = parameters

        with contextlib.redirect_stdout(stdout_capture):
            with contextlib.redirect_stderr(stderr_capture):
                compiled = compile(source_code, filename, "exec")
                exec(compiled, namespace, namespace)
    except BaseException as caught:
        error = _error_payload(caught)

    finished_at_ms = int(time.time() * 1000)
    result = {
        "schema_version": 1,
        "execution_id": execution_id,
        "script_id": script_id,
        "status": "FAILED" if error is not None else "SUCCEEDED",
        "stdout": stdout_capture.captured_text(),
        "stderr": stderr_capture.captured_text(),
        "error": error,
        "started_at_ms": started_at_ms,
        "finished_at_ms": finished_at_ms,
        "duration_ms": max(finished_at_ms - started_at_ms, 0),
        "stdout_truncated": stdout_capture.truncated,
        "stderr_truncated": stderr_capture.truncated,
    }
    return json.dumps(result, ensure_ascii=False)
