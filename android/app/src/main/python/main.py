"""Initial Python module for the Androide PY runtime smoke check."""

import platform


def runtime_info() -> str:
    """Return a compact description of the embedded Python interpreter."""
    return f"Python {platform.python_version()} ({platform.python_implementation()})"


def smoke_test() -> str:
    """Return a deterministic success message for the first integration check."""
    return f"Androide PY runtime OK — {runtime_info()}"
