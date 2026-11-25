"""High-level Python facade for Android capabilities."""

_bridge = None


def configure(bridge) -> None:
    """Install the Kotlin adapter for the current application process."""
    global _bridge
    _bridge = bridge


def _get_bridge():
    if _bridge is None:
        raise RuntimeError(
            "Android Bridge is not initialized. Run scripts through the Androide PY executor."
        )
    return _bridge
