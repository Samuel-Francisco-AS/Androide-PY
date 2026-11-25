"""Host-side tests for the pure-Python script runner."""

import json
import unittest

from runner import MAX_CAPTURE_CHARS, execute


class RunnerTests(unittest.TestCase):
    def test_success_captures_stdout(self):
        result = json.loads(execute("print('hello')", "exec-success", "sample"))
        self.assertEqual(result["status"], "SUCCEEDED")
        self.assertEqual(result["stdout"], "hello\n")
        self.assertEqual(result["stderr"], "")
        self.assertIsNone(result["error"])
        self.assertEqual(result["execution_id"], "exec-success")
        self.assertEqual(result["script_id"], "sample")

    def test_stderr_is_captured_separately(self):
        result = json.loads(execute("import sys\nprint('warning', file=sys.stderr)", "exec-stderr"))
        self.assertEqual(result["status"], "SUCCEEDED")
        self.assertEqual(result["stdout"], "")
        self.assertEqual(result["stderr"], "warning\n")

    def test_python_exception_becomes_structured_failure(self):
        result = json.loads(execute("print('before failure')\nraise ValueError('boom')", "exec-error"))
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["stdout"], "before failure\n")
        self.assertEqual(result["error"]["type"], "ValueError")
        self.assertEqual(result["error"]["message"], "boom")
        self.assertIn("ValueError: boom", result["error"]["traceback"])

    def test_system_exit_does_not_escape_runner(self):
        result = json.loads(execute("raise SystemExit(7)", "exec-exit"))
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["error"]["type"], "SystemExit")

    def test_output_is_bounded(self):
        result = json.loads(execute(f"print('x' * {MAX_CAPTURE_CHARS + 100}, end='')", "exec-truncated"))
        self.assertTrue(result["stdout_truncated"])
        self.assertIn("output truncated", result["stdout"])
        self.assertLessEqual(
            len(result["stdout"]),
            MAX_CAPTURE_CHARS + len("\n[output truncated: 65536 character limit reached]"),
        )

    def test_script_receives_parameter_dictionary(self):
        result = json.loads(execute("print(params['greeting'])", "exec-params", "sample", '{"greeting":"hello"}'))
        self.assertEqual(result["status"], "SUCCEEDED")
        self.assertEqual(result["stdout"], "hello\n")

    def test_non_object_parameters_become_structured_failure(self):
        result = json.loads(execute("print('should not run')", "exec-bad-params", "sample", '["not", "an object"]'))
        self.assertEqual(result["status"], "FAILED")
        self.assertEqual(result["error"]["type"], "ValueError")


if __name__ == "__main__":
    unittest.main()
