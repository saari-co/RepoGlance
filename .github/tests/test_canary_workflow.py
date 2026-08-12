#!/usr/bin/env python3
"""Static safety contract for the canary publication workflow."""

from pathlib import Path
import re
import unittest


WORKFLOW = Path(__file__).parents[1] / "workflows" / "canary.yml"


class CanaryWorkflowContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.text = WORKFLOW.read_text(encoding="utf-8")

    def test_actions_are_commit_pinned(self) -> None:
        uses = re.findall(r"^\s*uses:\s*([^\s#]+)", self.text, re.MULTILINE)
        self.assertTrue(uses)
        for action in uses:
            self.assertRegex(action, r"^[^@]+@[0-9a-f]{40}$")

    def test_capabilities_are_split(self) -> None:
        self.assertIn("  build:\n", self.text)
        self.assertIn("  sign:\n", self.text)
        self.assertIn("  publish:\n", self.text)
        self.assertIn("      contents: read", self.text)
        self.assertIn("      contents: none", self.text)
        self.assertEqual(self.text.count("      contents: write"), 1)
        self.assertIn("persist-credentials: false", self.text)

    def test_signing_fails_closed_and_checks_continuity(self) -> None:
        for name in (
            "REPOGLANCE_CANARY_KEYSTORE_B64",
            "REPOGLANCE_CANARY_KEY_ALIAS",
            "REPOGLANCE_CANARY_KEY_PASSWORD",
            "REPOGLANCE_CANARY_STORE_PASSWORD",
            "REPOGLANCE_CANARY_SIGNING_CERT_SHA256",
        ):
            self.assertIn(name, self.text)
        self.assertIn("Required canary signing input", self.text)
        self.assertIn("apksigner\" sign", self.text)
        self.assertIn("SIGNING_CERT_SHA256", self.text)
        self.assertNotRegex(self.text, r"echo\s+[^\n]*(KEYSTORE_B64|KEY_PASSWORD|STORE_PASSWORD)")

    def test__only_exact_current_main_can_publish(self) -> None:
        self.assertIn("branches: [main]", self.text)
        self.assertGreaterEqual(self.text.count("github.ref == 'refs/heads/main'"), 3)
        self.assertGreaterEqual(self.text.count("assert_current_main"), 4)
        self.assertIn("git/ref/heads/main", self.text)

    def test_payload_is_immutable_and_pointer_moves_last(self) -> None:
        self.assertIn('IMMUTABLE_TAG="canary-${GITHUB_SHA}"', self.text)
        self.assertIn('APK="repoglance-${GITHUB_SHA}.apk"', self.text)
        self.assertNotIn("gh release delete", self.text)
        self.assertNotIn("delete-asset", self.text)
        self.assertEqual(self.text.count("--clobber"), 1)

        apk_upload = self.text.index('ensure_asset canary "signed/$APK" yes')
        unique_manifest_upload = self.text.index(
            'ensure_asset canary "signed/$UNIQUE_MANIFEST" yes'
        )
        pointer_promotion = self.text.index(
            "gh release upload canary signed/version.json --clobber"
        )
        self.assertLess(apk_upload, pointer_promotion)
        self.assertLess(unique_manifest_upload, pointer_promotion)


if __name__ == "__main__":
    unittest.main()
