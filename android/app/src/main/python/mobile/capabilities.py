"""Introspection for capabilities implemented by the installed mobile package."""

LIBRARY_VERSION = "0.2.0"
API_VERSION = 1

_CAPABILITIES = (
    {
        "id": "device.get_info",
        "module": "mobile.device",
        "function": "get_info",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": False,
    },
    {
        "id": "apps.launch",
        "module": "mobile.apps",
        "function": "launch",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": False,
    },
)


def list_available() -> dict:
    """Return the stable, JSON-serializable catalog of implemented APIs.

    This describes implementation, not guaranteed runtime availability on
    every Android release or for every installed application.
    """
    return {
        "api_version": API_VERSION,
        "library_version": LIBRARY_VERSION,
        "capabilities": [dict(item) for item in _CAPABILITIES],
    }


def is_implemented(capability_id: str) -> bool:
    """Return whether a capability identifier has an implementation."""
    if not isinstance(capability_id, str):
        return False
    return any(item["id"] == capability_id for item in _CAPABILITIES)
