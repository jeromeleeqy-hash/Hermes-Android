"""Keep the app's bilingual release history and published changelogs in sync."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
entries = json.loads((ROOT / 'app/src/main/assets/release-history.json').read_text())
assert len({row['version'] for row in entries}) == len(entries)
for entry in entries:
    assert entry['zh'] and len(entry['zh']) == len(entry['en']), entry['version']

for lang, filename in [('zh', 'CHANGELOG.md'), ('en', 'CHANGELOG.en.md')]:
    header = '# Hermes 更新日志' if lang == 'zh' else '# Hermes release history'
    note = ('按版本倒序排列，包含现有发布说明能够核对的全部版本。未保存确切日期的早期版本不补写日期；没有发布记录的版本号不推测补写。历史记录描述当时的功能，当前行为以最新版说明为准。'
        if lang == 'zh' else 'All releases documented by the available release notes, newest first. Unknown early dates and undocumented version numbers are not invented. Historical entries describe behavior at that time; see the latest entry for current behavior.')
    lines = [header, '', note, '']
    for entry in entries:
        lines += ['## ' + entry['version'] + (' · ' + entry['date'] if entry['date'] else ''), '']
        lines += ['- ' + value for value in entry[lang]]
        lines += ['']
    (ROOT / filename).write_text('\n'.join(lines) + '\n')
print(f'Exported {len(entries)} bilingual release entries.')
