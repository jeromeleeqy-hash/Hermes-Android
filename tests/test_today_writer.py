"""Data-preservation checks for the server-side writer, using isolated workspaces."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ASSETS = Path(__file__).resolve().parents[1] / "app/src/main/assets"
SPEC = importlib.util.spec_from_file_location("today_writer", ASSETS / "hermes-today-writer.py")
WRITER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(WRITER)


class WriterTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.brief = json.loads((ASSETS / "hermes-today-examples.json").read_text())
        self.brief.pop("example_only")
        self.target = self.root / WRITER.STORAGE_DIRECTORY / WRITER.NAME
        self.target.parent.mkdir(parents=True)
        self.source = self.root / "candidate.json"

    def encoded(self, value):
        return json.dumps(value, ensure_ascii=False).encode()

    def write(self, value=None, expected="missing", **kwargs):
        self.source.write_bytes(self.encoded(value or self.brief))
        return WRITER.write(self.root, self.source, expected, **kwargs)

    def test_readable_layouts_and_optional_decision_interaction(self):
        WRITER.validate(self.encoded(self.brief), self.root)
        self.brief["cards"][0]["kind"] = "decision"
        WRITER.validate(self.encoded(self.brief), self.root)
        self.brief["presentation_version"] = 3
        with self.assertRaisesRegex(ValueError, "interactive component"):
            WRITER.validate(self.encoded(self.brief), self.root)

    def test_interactive_v3_remains_valid_and_bad_new_layouts_fail(self):
        value = json.loads((ASSETS / "hermes-today-interactions.json").read_text())
        value["presentation_version"] = 3
        value["editorial_version"] = 1
        WRITER.validate(self.encoded(value), self.root)
        card = self.brief["cards"][0]
        for layout in ["schedule", "progress", "metrics", "auto_execute"]:
            card["presentation"]["layout"] = layout
            with self.subTest(layout=layout), self.assertRaises(ValueError):
                WRITER.validate(self.encoded(self.brief), self.root)

    def test_stale_candidate_never_overwrites_newer_information(self):
        digest = self.write()
        newer = copy.deepcopy(self.brief)
        newer["cards"][0]["title"] = "负责人已确认，准备安排时间"
        latest = self.write(newer, digest)
        with self.assertRaisesRegex(ValueError, "current file changed"):
            self.write(self.brief, digest)
        self.assertEqual(latest, hashlib.sha256(self.target.read_bytes()).hexdigest())
        self.assertEqual(newer, json.loads(self.target.read_bytes()))
        self.assertEqual(1, len(list((self.root / WRITER.STORAGE_DIRECTORY / "snapshots").glob("*.json"))))

    def test_original_receipt_can_be_completed_without_rolling_back_newer_card(self):
        digest = self.write()
        newer = copy.deepcopy(self.brief)
        newer["cards"][0]["title"] = "负责人已确认，准备安排时间"
        digest = self.write(newer, digest)
        card_id = newer["cards"][0]["id"]
        latest = self.write(newer, digest, action_id="original-action", card_id=card_id,
                            result_message="原选择已落实；保留后续进展。")
        result = json.loads(self.target.read_bytes())
        self.assertEqual(newer["cards"], result["cards"])
        self.assertEqual("original-action", result["action_receipts"][0]["operation_id"])
        # A reconnect retry is a no-op even if it still carries the earlier snapshot.
        same = self.write(self.brief, digest, action_id="original-action", card_id=card_id)
        self.assertEqual(latest, same)
        self.assertEqual(result, json.loads(self.target.read_bytes()))
        with self.assertRaisesRegex(ValueError, "different card"):
            self.write(newer, latest, action_id="original-action", card_id="unrelated")

    def test_cron_preserves_receipts_and_cannot_reopen_confirmed_closed_work(self):
        self.brief["cards"][0]["status"] = "done"
        card_id = self.brief["cards"][0]["id"]
        digest = self.write(action_id="confirmed", card_id=card_id)
        digest = self.write(self.brief, digest)
        self.assertEqual("confirmed", json.loads(self.target.read_bytes())["action_receipts"][0]["operation_id"])
        self.brief["cards"][0]["status"] = "open"
        with self.assertRaisesRegex(ValueError, "cannot reopen"):
            self.write(self.brief, digest)

    def test_examples_cannot_be_written_as_user_facts(self):
        self.brief["example_only"] = True
        with self.assertRaisesRegex(ValueError, "Example data"):
            self.write()
        self.assertFalse(self.target.exists())

    def test_external_sources_and_symlink_targets_are_rejected(self):
        for source in ["../secret.md", "https://example.com/a", "/etc/passwd"]:
            self.brief["cards"][0]["sources"] = [source]
            with self.subTest(source=source), self.assertRaises(ValueError):
                WRITER.validate(self.encoded(self.brief), self.root)
        self.brief["cards"][0]["sources"] = []
        self.target.symlink_to(self.root / "other.json")
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.write()

    def test_invalid_attention_metadata_is_not_persisted(self):
        for attention in [{"group": "guess"}, {"group": "focus", "priority": True},
                          {"group": "focus", "priority": 9}]:
            self.brief["cards"][0]["attention"] = attention
            with self.subTest(attention=attention), self.assertRaises(ValueError):
                self.write()
        self.assertFalse(self.target.exists())


if __name__ == "__main__":
    unittest.main()
