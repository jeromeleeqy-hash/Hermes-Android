"""Migration/recovery invariants; each test owns an isolated workspace."""
import copy
import fcntl
import hashlib
import json
from unittest.mock import patch
import unittest
import test_today_writer as baseline
WRITER = baseline.WRITER


class StorageTest(unittest.TestCase):
    setUp = baseline.WriterTest.setUp
    encoded = baseline.WriterTest.encoded
    write = baseline.WriterTest.write
    def legacy(self):
        value = copy.deepcopy(self.brief)
        value['action_receipts'] = [{'operation_id': 'original', 'card_id': value['cards'][0]['id'], 'status': 'applied'}]
        raw = self.encoded(value)
        (self.root / WRITER.NAME).write_bytes(raw)
        snapshots = self.root / '.snapshots/hermes-today'
        snapshots.mkdir(parents=True)
        (snapshots / 'original.json').write_bytes(b'historical snapshot bytes')
        return raw, snapshots

    def test_fresh_workspace_writes_only_new_storage(self):
        self.write()
        self.assertTrue(self.target.is_file())
        self.assertFalse((self.root / WRITER.NAME).exists())
        self.assertTrue((self.target.parent / '.hermes-today.lock').is_file())

    def test_unmigrated_installation_keeps_legacy_writer_location(self):
        raw, snapshots = self.legacy()
        self.write(expected=hashlib.sha256(raw).hexdigest())
        self.assertFalse(self.target.exists())
        self.assertTrue((self.root / WRITER.NAME).is_file())
        self.assertGreater(len(list(snapshots.iterdir())), 1)

    def test_migration_preserves_receipts_and_unrelated_snapshots(self):
        raw, old_snapshots = self.legacy()
        unrelated = self.root / '.snapshots/another-program'
        unrelated.mkdir()
        (unrelated / 'keep.txt').write_text('leave alone')
        receipt = WRITER.migrate_storage(self.root, True)
        self.assertEqual(raw, self.target.read_bytes())
        self.assertEqual(hashlib.sha256(raw).hexdigest(), receipt['sha256'])
        self.assertFalse((self.root / WRITER.NAME).exists())
        self.assertFalse((self.root / '.hermes-today.lock').exists())
        self.assertFalse(old_snapshots.exists())
        self.assertEqual('leave alone', (unrelated / 'keep.txt').read_text())
        self.assertEqual(b'historical snapshot bytes', (self.target.parent / 'snapshots/original.json').read_bytes())
        self.assertEqual(raw, next((self.target.parent / 'legacy-backup').glob('*.json')).read_bytes())
        WRITER.migrate_storage(self.root, True)
        self.assertEqual(raw, self.target.read_bytes())
        self.write(expected=receipt['sha256'])
        self.assertEqual('original', json.loads(self.target.read_bytes())['action_receipts'][0]['operation_id'])

    def test_requires_quiesced_writers_and_rejects_held_lock(self):
        raw, _ = self.legacy()
        with self.assertRaisesRegex(ValueError, 'idle'):
            WRITER.migrate_storage(self.root)
        with (self.root / '.hermes-today.lock').open('w') as held:
            fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with self.assertRaisesRegex(ValueError, 'active'):
                WRITER.migrate_storage(self.root, True)
        self.assertEqual(raw, (self.root / WRITER.NAME).read_bytes())
        self.assertFalse(self.target.exists())

    def test_conflicting_overviews_are_not_chosen_by_timestamp(self):
        old, _ = self.legacy()
        new = self.encoded(dict(self.brief, headline='Different meaning'))
        self.target.write_bytes(new)
        with self.assertRaisesRegex(ValueError, 'Conflicting'):
            WRITER.migrate_storage(self.root, True)
        self.assertEqual(old, (self.root / WRITER.NAME).read_bytes())
        self.assertEqual(new, self.target.read_bytes())

    def test_snapshot_conflict_preserves_both(self):
        old, snapshots = self.legacy()
        target = self.target.parent / 'snapshots/original.json'
        target.parent.mkdir()
        target.write_bytes(b'different')
        with self.assertRaisesRegex(ValueError, 'Conflicting snapshot'):
            WRITER.migrate_storage(self.root, True)
        self.assertEqual(old, (self.root / WRITER.NAME).read_bytes())
        self.assertEqual(b'different', target.read_bytes())
        self.assertTrue((snapshots / 'original.json').exists())

    def test_interrupted_copy_keeps_sources_and_can_resume(self):
        old, snapshots = self.legacy()
        real = WRITER.atomic_bytes
        def interrupt(path, raw):
            if path.name == 'storage-migration.json': raise OSError('simulated interruption')
            return real(path, raw)
        with patch.object(WRITER, 'atomic_bytes', interrupt):
            with self.assertRaises(OSError): WRITER.migrate_storage(self.root, True)
        self.assertEqual(old, self.target.read_bytes())
        self.assertEqual(old, (self.root / WRITER.NAME).read_bytes())
        self.assertTrue(snapshots.exists())
        WRITER.migrate_storage(self.root, True)
        self.assertFalse((self.root / WRITER.NAME).exists())
        self.assertEqual(old, self.target.read_bytes())

    def test_symlinked_snapshot_is_rejected(self):
        old, snapshots = self.legacy()
        (snapshots / 'link.json').symlink_to(self.root / 'candidate.json')
        with self.assertRaisesRegex(ValueError, 'symlink'):
            WRITER.migrate_storage(self.root, True)
        self.assertEqual(old, (self.root / WRITER.NAME).read_bytes())

    def test_refresh_receipt_is_idempotent_and_survives_cron(self):
        digest = self.write(refresh_id='refresh-once')
        result = json.loads(self.target.read_bytes())
        self.assertEqual('refresh-once', result['refresh_receipts'][0]['request_id'])
        self.assertEqual(digest, self.write(expected='stale', refresh_id='refresh-once'))
        self.write(expected=digest)
        self.assertEqual(result['refresh_receipts'], json.loads(self.target.read_bytes())['refresh_receipts'])
