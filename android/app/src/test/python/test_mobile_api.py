"""Host-side tests for the Python Android Bridge facade."""

import json
import unittest

from mobile import API_VERSION, __version__, configure
from mobile.apps import launch
from mobile.capabilities import is_implemented, list_available
from mobile.device import get_info


class FakeBridge:
    def getDeviceInfoJson(self):
        return json.dumps({"manufacturer": "Test Manufacturer", "model": "Test Device", "api_level": 36})

    def launchAppJson(self, package_name):
        if package_name == "com.example.installed":
            payload = {"ok": True, "package_name": package_name, "error": None}
        else:
            payload = {
                "ok": False,
                "package_name": package_name,
                "error": {"code": "APP_NOT_FOUND_OR_NOT_VISIBLE", "message": "App not found."},
            }
        return json.dumps(payload)


class MobileApiTests(unittest.TestCase):
    def setUp(self):
        configure(FakeBridge())

    def tearDown(self):
        configure(None)

    def test_device_info_returns_python_dictionary(self):
        info = get_info()
        self.assertEqual(info["manufacturer"], "Test Manufacturer")
        self.assertEqual(info["model"], "Test Device")
        self.assertEqual(info["api_level"], 36)

    def test_launch_returns_success_dictionary(self):
        result = launch("com.example.installed")
        self.assertTrue(result["ok"])
        self.assertEqual(result["package_name"], "com.example.installed")
        self.assertIsNone(result["error"])

    def test_missing_app_returns_structured_error(self):
        result = launch("com.example.missing")
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["code"], "APP_NOT_FOUND_OR_NOT_VISIBLE")

    def test_non_string_package_is_rejected(self):
        result = launch(123)
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"]["code"], "INVALID_PACKAGE_NAME")

    def test_unconfigured_bridge_fails_clearly(self):
        configure(None)
        with self.assertRaisesRegex(RuntimeError, "Android Bridge is not initialized"):
            get_info()

    def test_capability_catalog_only_lists_implemented_apis(self):
        catalog = list_available()
        identifiers = {item["id"] for item in catalog["capabilities"]}
        self.assertEqual(catalog["api_version"], API_VERSION)
        self.assertEqual(catalog["library_version"], __version__)
        self.assertEqual(identifiers, {"device.get_info", "apps.launch"})
        self.assertTrue(is_implemented("apps.launch"))
        self.assertFalse(is_implemented("screen.tap"))
        self.assertFalse(is_implemented(123))


if __name__ == "__main__":
    unittest.main()
