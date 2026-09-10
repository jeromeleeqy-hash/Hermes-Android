import copy
import tempfile
import unittest
from pathlib import Path
import uuid
import assistant_workspace as a

class WorkspaceProtocolTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
    def tearDown(self): self.temp.cleanup()
    def record(self, **updates):
        value = dict(schema=1, revision=str(uuid.uuid4()), entity='focus', parents=[], kind='FOCUS', title='近期重点 -->',
            content='AI 短视频与私域获客\n先看真实数据', intent='', topic='', source_session='', source_message='', files=[],
            created_at=1, confirmed=True, archived=False, due_at=0, zone='', cron_id='', reminder_attempted=False)
        value.update(updates)
        return value
    def test_same_header_and_unicode_format_as_mobile(self):
        value = self.record()
        path = a.write(self.root, 'work', copy.deepcopy(value))
        loaded = a.decode(path); loaded.pop('_path')
        self.assertEqual(value, loaded)
        self.assertFalse(a.read_records(self.root, 'personal'))
    def test_immutable_files_cannot_be_overwritten(self):
        value = self.record()
        path = a.write(self.root, 'work', copy.deepcopy(value))
        before = path.read_bytes()
        with self.assertRaises(FileExistsError): a.write(self.root, 'work', copy.deepcopy(value))
        self.assertEqual(before, path.read_bytes())
    def test_concurrent_branches_remain_until_merged(self):
        base = self.record()
        one = self.record(parents=[base['revision']], content='手机补充', created_at=2)
        two = self.record(parents=[base['revision']], content='电脑补充', created_at=3)
        rows = [base, one, two]
        self.assertEqual({one['revision'], two['revision']}, {r['revision'] for r in a.current(rows)})
        merged = self.record(parents=[one['revision'], two['revision']], content='合并', created_at=4)
        self.assertEqual([merged], a.current(rows + [merged]))
    def test_foreign_entity_parent_does_not_remove_original(self):
        base = self.record()
        other = self.record(entity='other', parents=[base['revision']])
        self.assertEqual(2, len(a.current([base, other])))

if __name__ == '__main__': unittest.main()
