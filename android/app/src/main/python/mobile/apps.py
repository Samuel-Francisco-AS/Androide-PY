"""Installed application launch capability."""

import json

from mobile import _get_bridge


def launch(package_name: str) -> dict:
    """Launch an installed app by package name and return a structured result."""
    if not isinstance(package_name, str):
        return {
            "ok": False,
            "package_name": str(package_name),
            "error": {
                "code": "INVALID_PACKAGE_NAME",
                "message": "package_name must be a string.",
            },
        }

    payload = _get_bridge().launchAppJson(package_name)
    return json.loads(payload)
