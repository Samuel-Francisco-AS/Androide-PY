"""Device information capability."""

import json

from mobile import _get_bridge


def get_info() -> dict:
    """Return non-sensitive device and application information."""
    payload = _get_bridge().getDeviceInfoJson()
    return json.loads(payload)
