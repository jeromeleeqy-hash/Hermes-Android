#!/usr/bin/env python3
"""Read/write the mobile assistant's immutable Markdown records using ordinary files.
No service, port, scheduler, model API, or dependencies. Run in the same Hermes profile/workspace.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import time
import uuid

MARKER = '<!-- hermes-assistant-v1 '
KINDS = ('MATERIAL', 'FOCUS', 'PLAN', 'REVIEW', 'FOLLOWUP', 'RESULT', 'PREFERENCES')

def prefix(profile: str) -> str:
    return 'hermes-assistant-' + hashlib.sha256(profile.encode()).hexdigest()[:12] + '-'

def decode(path: Path) -> dict:
    head, _, content = path.read_text(encoding='utf-8').partition('\n')
    if not head.startswith(MARKER) or not head.endswith(' -->'):
        raise ValueError(f'Unsupported assistant record: {path.name}')
    value = json.loads(head[len(MARKER):-4])
    if value.get('schema') != 1 or value.get('kind') not in KINDS:
        raise ValueError(f'Unsupported schema: {path.name}')
    uuid.UUID(value['revision'])
    value['content'] = content
    value['_path'] = str(path)
    return value

def read_records(root: Path, profile: str) -> list[dict]:
    return [decode(path) for path in sorted(root.glob(prefix(profile) + '*.md'))]

def current(records: list[dict]) -> list[dict]:
    by_id = {r['revision']: r for r in records}
    superseded = {p for r in records for p in r.get('parents', []) if by_id.get(p, {}).get('entity') == r['entity']}
    return sorted([r for r in records if r['revision'] not in superseded], key=lambda r: r['created_at'], reverse=True)

def write(root: Path, profile: str, record: dict) -> Path:
    content = record.pop('content')
    record.pop('_path', None)
    encoded = json.dumps(record, ensure_ascii=False, separators=(',', ':')).replace('-->', '--\\u003e')
    text = MARKER + encoded + ' -->\n' + content
    path = root / (prefix(profile) + record['revision'] + '.md')
    # Exclusive file creation protects immutable revisions against an accidental overwrite.
    import tempfile
    with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', dir=root, prefix='.assistant-write-', delete=False) as handle:
        temporary = Path(handle.name)
        handle.write(text)
        handle.flush()
        os.fsync(handle.fileno())
    try:
        os.link(temporary, path)  # atomic publication; fails if the destination already exists
    finally:
        temporary.unlink(missing_ok=True)
    return path

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True, help='Existing absolute workspace directory')
    parser.add_argument('--profile', required=True, help='Exact Hermes profile name')
    sub = parser.add_subparsers(dest='command', required=True)
    listing = sub.add_parser('list'); listing.add_argument('--all', action='store_true')
    show = sub.add_parser('show'); show.add_argument('revision')
    create = sub.add_parser('create')
    create.add_argument('--kind', choices=KINDS, required=True)
    create.add_argument('--title', required=True)
    create.add_argument('--content-file', type=Path, required=True)
    create.add_argument('--intent', default=''); create.add_argument('--topic', default='')
    create.add_argument('--source-session', default='')
    create.add_argument('--entity', help='Use focus, preferences, or plan-YYYY-MM-DD to revise a shared item')
    create.add_argument('--parent', action='append', default=[], help='Explicit parent revision; repeat to merge branches')
    create.add_argument('--confirmed', action='store_true', help='Only when the user has explicitly accepted this content')
    args = parser.parse_args()
    root = args.root
    if not root.is_absolute() or not root.is_dir() or root.resolve() == Path('/') or '..' in root.parts:
        parser.error('--root must be an existing, explicit workspace directory')
    if not args.profile.strip(): parser.error('--profile cannot be empty')
    records = read_records(root, args.profile)
    if args.command == 'list':
        rows = records if args.all else current(records)
        print(json.dumps(rows, ensure_ascii=False, indent=2)); return
    if args.command == 'show':
        matches = [r for r in records if r['revision'] == args.revision]
        if not matches: parser.error('revision not found in this profile/workspace')
        print(json.dumps(matches[0], ensure_ascii=False, indent=2)); return
    content = args.content_file.read_text(encoding='utf-8').strip()
    if not content or not args.title.strip(): parser.error('title and content cannot be empty')
    if len(content) > 200000: parser.error('content exceeds 200000 characters')
    entity = args.entity or str(uuid.uuid4())
    import re
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,100}', entity): parser.error('invalid entity')
    by_id = {r['revision']: r for r in records}
    for parent in args.parent:
        if parent not in by_id or by_id[parent]['entity'] != entity:
            parser.error('every parent must belong to this entity and profile')
    record = dict(schema=1, revision=str(uuid.uuid4()), entity=entity, parents=args.parent,
                  kind=args.kind, title=args.title.strip(), content=content, intent=args.intent, topic=args.topic,
                  source_session=args.source_session, source_message='', files=[], created_at=int(time.time()*1000),
                  confirmed=args.confirmed, archived=False, due_at=0, zone='', cron_id='', reminder_attempted=False)
    print(write(root, args.profile, record))

if __name__ == '__main__':
    main()
