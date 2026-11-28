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
    {
        "id": "screen.get_status",
        "module": "mobile.screen",
        "function": "get_status",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": False,
    },
    {
        "id": "screen.tap",
        "module": "mobile.screen",
        "function": "tap",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.type_text",
        "module": "mobile.screen",
        "function": "type_text",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.swipe",
        "module": "mobile.screen",
        "function": "swipe",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.scroll",
        "module": "mobile.screen",
        "function": "scroll",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.capture_coordinates",
        "module": "mobile.screen",
        "function": "capture_coordinates",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.list_coordinate_maps",
        "module": "mobile.screen",
        "function": "list_coordinate_maps",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.get_coordinate_map",
        "module": "mobile.screen",
        "function": "get_coordinate_map",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.delete_coordinate_map",
        "module": "mobile.screen",
        "function": "delete_coordinate_map",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
    },
    {
        "id": "screen.tap_point",
        "module": "mobile.screen",
        "function": "tap_point",
        "implemented": True,
        "requires_android_bridge": True,
        "requires_user_authorization": True,
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
