#!/usr/bin/env python3
"""Validate and atomically replace an explicitly chosen workspace daily brief."""
import argparse
import datetime as dt
import fcntl
import hashlib
import json
import math
import os
from pathlib import Path
import re
import tempfile
import uuid

NAME = "hermes-today.json"

CARD_TYPES = {"clarification", "comparison", "priorities", "time_picker", "meeting_actions", "checklist", "milestones", "blocker", "execution", "deliverable", "metrics", "change", "timeline", "evidence", "revision", "triage", "memory_change", "person_followup", "quick_log", "reflection"}


def validate_interaction(value):
    if not isinstance(value, dict) or type(value.get("version")) is not int or value["version"] != 1 or value.get("type") not in CARD_TYPES:
        raise ValueError("Unsupported interaction version or type")
    kind = value["type"]
    ids = set()
    def text(obj, key, maximum, required=False):
        v = obj.get(key, "")
        if not isinstance(v, str) or len(v.encode("utf-16-le")) // 2 > maximum or (required and not v.strip()):
            raise ValueError("Invalid interaction." + key)
        return v
    def items(key, maximum):
        result = value.get(key, [])
        if not isinstance(result, list) or len(result) > maximum or any(not isinstance(x, dict) for x in result):
            raise ValueError("Invalid interaction." + key)
        return result
    def identity(row):
        id = text(row, "id", 64, True)
        if not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", id) or id in ids:
            raise ValueError("Invalid or duplicate interaction id")
        ids.add(id)
    rows = items("items", 12)
    for row in rows:
        identity(row)
        text(row, "title", 60, True)
        for key, limit in [("detail", 200), ("value", 120), ("owner", 60), ("due", 10)]:
            text(row, key, limit)
        if row.get("status", "open") not in {"open", "done", "waiting", "active"}:
            raise ValueError("Invalid interaction item status")
        if row.get("due"):
            if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", row["due"]):
                raise ValueError("Invalid due date")
            dt.date.fromisoformat(row["due"])
    fields = items("fields", 5)
    for field in fields:
        identity(field)
        text(field, "label", 60, True)
        input_kind = text(field, "kind", 16, True)
        if input_kind not in {"text", "number", "date", "time", "select"}:
            raise ValueError("Invalid field kind")
        if "required" in field and type(field["required"]) is not bool:
            raise ValueError("required must be boolean")
        choices = field.get("choices", [])
        if not isinstance(choices, list) or len(choices) > 8 or any(not isinstance(c, str) or not c.strip() or len(c.encode("utf-16-le")) // 2 > 60 for c in choices):
            raise ValueError("Invalid field choices")
        if len(set(choices)) != len(choices) or (input_kind == "select" and not choices) or ("choices" in field and not choices):
            raise ValueError("Empty or duplicate choices")
        low, high = field.get("min", 0), field.get("max", 100000)
        if any(type(x) not in (int, float) or not math.isfinite(x) for x in (low, high)) or not -1e9 <= low <= high <= 1e9:
            raise ValueError("Invalid numeric bounds")
        content = text(field, "value", 500)
        if content.strip():
            if input_kind == "number" and (not math.isfinite(float(content)) or not low <= float(content) <= high):
                raise ValueError("Numeric input outside bounds")
            if input_kind == "select" and content not in choices:
                raise ValueError("Unknown choice")
            if input_kind == "date":
                if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", content):
                    raise ValueError("Invalid date input")
                dt.date.fromisoformat(content)
            if input_kind == "time":
                if not re.fullmatch(r"[0-2][0-9]:[0-5][0-9]", content):
                    raise ValueError("Invalid time input")
                dt.time.fromisoformat(content)
    for point in items("series", 12):
        text(point, "label", 24, True)
        number = point.get("value")
        if type(number) not in (int, float) or not math.isfinite(number) or not -1e12 <= number <= 1e12:
            raise ValueError("Invalid series value")
    for key, limit in [("before", 160), ("after", 160), ("text", 800), ("note", 160), ("timezone", 80)]:
        text(value, key, limit)
    if value.get("timezone"):
        from zoneinfo import ZoneInfo
        ZoneInfo(value["timezone"])
    if kind not in {"metrics", "blocker", "memory_change", "quick_log", "reflection"} and not rows:
        raise ValueError(kind + " requires items")
    if kind in {"clarification", "comparison", "time_picker"} and not 2 <= len(rows) <= 4:
        raise ValueError(kind + " requires 2-4 items")
    if kind == "time_picker" and not value.get("timezone"):
        raise ValueError("Time selection requires timezone")
    if kind == "quick_log" and not fields:
        raise ValueError("Quick log requires fields")
    if kind in {"memory_change", "change"} and (not value.get("before", "").strip() or not value.get("after", "").strip()):
        raise ValueError("Change requires before and after")


def validate(raw, root):
    if len(raw) > 1024 * 1024:
        raise ValueError("Daily brief exceeds 1 MiB")
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("Duplicate JSON key: " + key)
            result[key] = value
        return result
    value = json.loads(raw, object_pairs_hook=unique_pairs)
    if not isinstance(value, dict) or type(value.get("schema_version")) is not int or value["schema_version"] != 1:
        raise ValueError("Expected schema_version 1")
    editorial = value.get("editorial_version") in (1, 2)
    if "editorial_version" in value and (type(value["editorial_version"]) is not int or not editorial):
        raise ValueError("Expected editorial_version 1 or 2")
    compact = value.get("presentation_version") in (1, 2, 3, 4)
    reading_first = value.get("presentation_version") == 4
    structured = value.get("presentation_version") in (3, 4)
    if "presentation_version" in value and (type(value["presentation_version"]) is not int or not compact):
        raise ValueError("Expected presentation_version 1, 2, 3 or 4")
    dt.date.fromisoformat(value["date"])
    timestamp = dt.datetime.fromisoformat(value["generated_at"].replace("Z", "+00:00"))
    if timestamp.tzinfo is None:
        raise ValueError("generated_at requires a timezone")
    def length(text):
        return len(text.encode("utf-16-le")) // 2  # Match Android's String.length.
    for key, limit in [("headline", 48 if compact else 180), ("summary", 240 if compact else 12000)]:
        if not isinstance(value.get(key), str) or length(value[key]) > limit:
            raise ValueError("Invalid " + key)
    if "coverage_note" in value and (not isinstance(value["coverage_note"], str) or length(value["coverage_note"]) > 160):
        raise ValueError("Invalid coverage_note")
    cards = value.get("cards")
    if not isinstance(cards, list) or len(cards) > 200:
        raise ValueError("cards must be a list with at most 200 items")
    seen = set()
    for card in cards:
        if not isinstance(card, dict):
            raise ValueError("Invalid card")
        identity = card.get("id", "")
        if not isinstance(identity, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,96}", identity) or identity in seen:
            raise ValueError("Invalid or duplicate id")
        seen.add(identity)
        if card.get("kind") not in {"decision", "schedule", "followup", "update"}:
            raise ValueError("Unknown kind")
        if card.get("status", "open") not in {"open", "waiting", "done", "archived"}:
            raise ValueError("Unknown status")
        if card.get("domain", "general") not in {"general", "work", "life", "learning", "information", "relationships", "health", "finance", "family", "interests", "travel"}:
            raise ValueError("Unknown domain")
        if "summary" not in card:
            raise ValueError(identity + ": missing summary")
        if editorial:
            if not isinstance(card.get("summary"), str) or not card["summary"].strip():
                raise ValueError(identity + ": a readable context summary is required")
            if not isinstance(card.get("title"), str):
                raise ValueError(identity + ": invalid title")
            if any(label in card.get("title", "") for label in ("明确缺少的信息", "缺的（不编）", "JSON 已落盘", "JSON已落盘")):
                raise ValueError(identity + ": processing notes belong in coverage_note, not a card")
        for key, limit in [("title", 48 if compact else 180), ("summary", 160 if compact else 8000), ("when", 60 if compact else 120), ("session_id", 180)]:
            text = card.get(key, "")
            if not isinstance(text, str) or length(text) > limit or (key == "title" and not text.strip()):
                raise ValueError(identity + ": invalid " + key + " (limit " + str(limit) + ")")
        if "attention" in card:
            attention = card["attention"]
            if not isinstance(attention, dict) or attention.get("group") not in {"focus", "followup", "reminder"}:
                raise ValueError(identity + ": invalid attention group")
            if type(attention.get("priority", 2)) is not int or not 1 <= attention.get("priority", 2) <= 3:
                raise ValueError(identity + ": priority must be 1, 2 or 3")
            if not isinstance(attention.get("reason", ""), str) or length(attention.get("reason", "")) > 100:
                raise ValueError(identity + ": invalid attention reason")
        presentation = card.get("presentation", {})
        if not isinstance(presentation, dict):
            raise ValueError(identity + ": presentation must be an object")
        if structured:
            layout = presentation.get("layout")
            allowed_layouts = {"interactive", "note", "metrics", "receipt"} | ({"schedule", "progress"} if reading_first else set())
            if layout not in allowed_layouts:
                raise ValueError(identity + ": choose presentation.layout explicitly")
            caption = presentation.get("caption")
            if not isinstance(caption, str) or not caption.strip() or length(caption) > 100:
                raise ValueError(identity + ": caption requires readable context, at most 100 characters")
            if (layout == "interactive") != ("interaction" in presentation):
                raise ValueError(identity + ": layout and interaction do not match")
            if not reading_first and card["kind"] in {"decision", "followup"} and layout != "interactive":
                raise ValueError(identity + ": a decision or follow-up needs an interactive component")
            if not reading_first and layout != "interactive" and (presentation.get("options") or presentation.get("steps")):
                raise ValueError(identity + ": actionable choices or steps require an interaction")
            if layout == "schedule" and not card.get("when", "").strip():
                raise ValueError(identity + ": schedule requires a known time")
            if layout == "progress" and not presentation.get("steps"):
                raise ValueError(identity + ": progress requires recorded steps")
            if layout in {"metrics", "receipt"} and not presentation.get("metrics"):
                raise ValueError(identity + ": numeric layout requires metrics")
        if "interaction" in presentation:
            validate_interaction(presentation["interaction"])
            if value.get("presentation_version") not in (2, 3, 4):
                raise ValueError("Interactive cards require presentation_version 2, 3 or 4")
        for name, maximum in [("question", 80), ("background", 8000), ("intent", 10)]:
            text = presentation.get(name, "")
            if not isinstance(text, str) or length(text) > maximum:
                raise ValueError(identity + ": invalid presentation." + name)
        if presentation.get("intent", "") not in {"", "clarify", "decide"}:
            raise ValueError(identity + ": invalid intent")
        if compact and not reading_first and card["kind"] == "decision" and (not presentation.get("question", "").strip() or presentation.get("intent") not in {"clarify", "decide"}):
            raise ValueError(identity + ": decision requires question and intent")
        facts = presentation.get("facts", [])
        if not isinstance(facts, list) or len(facts) > 3 or any(not isinstance(t, str) or not t.strip() or length(t) > 80 for t in facts):
            raise ValueError(identity + ": facts must contain at most 3 short strings")
        for name, maximum, fields, required in [
            ("options", 4, {"title": 60, "detail": 120}, {"title"}),
            ("steps", 12, {"title": 120, "status": 10}, {"title", "status"}),
            ("metrics", 3, {"label": 40, "value": 40, "unit": 20}, {"label", "value"}),
        ]:
            items = presentation.get(name, [])
            if not isinstance(items, list) or len(items) > maximum:
                raise ValueError(identity + ": invalid " + name)
            for item in items:
                if not isinstance(item, dict):
                    raise ValueError(identity + ": invalid " + name + " item")
                for field, limit in fields.items():
                    text = item.get(field, "")
                    if not isinstance(text, str) or length(text) > limit or (field in required and not text.strip()):
                        raise ValueError(identity + ": invalid " + name + "." + field)
                if name == "steps" and item["status"] not in {"open", "done"}:
                    raise ValueError(identity + ": step status must be open or done")
        sources = card.get("sources", [])
        if not isinstance(sources, list) or len(sources) > 20:
            raise ValueError("Invalid sources")
        for source in sources:
            if not isinstance(source, str) or not source.strip() or len(source) > 4096 or re.match(r"^[A-Za-z][A-Za-z0-9+.-]*:", source) or ".." in source.replace("\\", "/").split("/") or any(ord(c) < 32 for c in source) or "#" in source or source.startswith("[") or "](" in source:
                raise ValueError("Invalid source path")
            path = Path(source)
            resolved = (path if path.is_absolute() else root / path).resolve()
            if not resolved.is_relative_to(root):
                raise ValueError("Source escapes the workspace")
    return value



STORAGE_DIRECTORY = Path(".hermes-app/today")


def storage_directory(root):
    directory = root / STORAGE_DIRECTORY
    for part in [root / ".hermes-app", directory]:
        if part.is_symlink() or (part.exists() and not part.is_dir()):
            raise ValueError("Overview storage must be a real directory inside the workspace")
    directory.mkdir(parents=True, exist_ok=True)
    if not directory.resolve().is_relative_to(root):
        raise ValueError("Overview storage escapes the workspace")
    return directory


def target_for_workspace(root):
    modern = root / STORAGE_DIRECTORY / NAME
    legacy = root / NAME
    if modern.is_symlink() or legacy.is_symlink():
        raise ValueError("The daily brief must not be a symlink")
    if modern.exists() and legacy.exists():
        if modern.read_bytes() != legacy.read_bytes():
            raise ValueError("Conflicting old and new overviews; preserve both and resolve the conflict")
        raise ValueError("Storage migration is incomplete; finish --migrate-storage before writing")
    # Existing installations remain usable until the explicit migration completes.
    return legacy if legacy.exists() else storage_directory(root) / NAME


def atomic_bytes(path, raw):
    if path.is_symlink():
        raise ValueError("Refusing a symlink destination")
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=".hermes-today-", suffix=".tmp", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(raw)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
        fd = os.open(path.parent, os.O_RDONLY)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def migrate_storage(root, writers_stopped=False):
    """Explicit, restartable migration. The caller must first quiesce matching writers/Cron."""
    if not writers_stopped:
        raise ValueError("Verify matching Cron and all overview writers are idle before migration")
    root = root.resolve(strict=True)
    if root == Path("/") or not root.is_dir():
        raise ValueError("Choose an explicit workspace")
    directory = storage_directory(root)
    legacy, modern = root / NAME, directory / NAME
    old_snapshots, snapshots = root / ".snapshots/hermes-today", directory / "snapshots"
    backup = directory / "legacy-backup"
    for path in [legacy, modern, root / ".snapshots", old_snapshots, snapshots, backup]:
        if path.is_symlink() or not path.resolve().is_relative_to(root):
            raise ValueError("Migration paths must not be symlinks or leave the workspace")
    locks = []
    old_lock = root / ".hermes-today.lock"
    completed = False
    try:
        for path in [old_lock, directory / ".hermes-today.lock"]:
            fd = os.open(path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
            stream = os.fdopen(fd, "r+")
            locks.append(stream)
            try:
                fcntl.flock(stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError:
                raise ValueError("An overview writer is active; wait for it to finish")
        old = legacy.read_bytes() if legacy.exists() else None
        current = modern.read_bytes() if modern.exists() else None
        raw = old if old is not None else current
        if raw is not None:
            validate(raw, root)
        if old is not None and current is not None and old != current:
            raise ValueError("Conflicting old and new overviews; preserve both and resolve the conflict")
        copies = []
        if old_snapshots.exists():
            for source in sorted(old_snapshots.rglob("*")):
                if source.is_symlink():
                    raise ValueError("A snapshot is a symlink; migration stopped")
                if source.is_file():
                    target = snapshots / source.relative_to(old_snapshots)
                    for parent in [target, *target.parents]:
                        if parent == directory: break
                        if parent.is_symlink(): raise ValueError("Snapshot destination is a symlink")
                    content = source.read_bytes()
                    if target.exists() and target.read_bytes() != content:
                        raise ValueError("Conflicting snapshot names; preserve both directories")
                    copies.append((source, target, content))
        if old is not None:
            saved = backup / (hashlib.sha256(old).hexdigest() + ".json")
            if saved.exists() and saved.read_bytes() != old:
                raise ValueError("Migration backup mismatch")
            atomic_bytes(saved, old)
            if saved.read_bytes() != old: raise ValueError("Backup read-back mismatch")
        for source, target, content in copies:
            if not target.exists(): atomic_bytes(target, content)
            if target.read_bytes() != content: raise ValueError("Snapshot read-back mismatch")
        if raw is not None:
            if current is None: atomic_bytes(modern, raw)
            if modern.read_bytes() != raw: raise ValueError("Overview read-back mismatch")
        receipt = {"storage_version": 2, "overview": str(STORAGE_DIRECTORY / NAME),
                   "sha256": hashlib.sha256(raw).hexdigest() if raw is not None else "missing",
                   "copied_snapshots": len(copies), "migrated_at": dt.datetime.now(dt.timezone.utc).isoformat()}
        atomic_bytes(directory / "storage-migration.json", (json.dumps(receipt, indent=2) + "\n").encode())
        # Cleanup starts only after all copies and the recovery receipt are durable.
        if old is not None:
            if legacy.read_bytes() != old: raise ValueError("Legacy overview changed during migration")
            legacy.unlink()
        for source, target, content in copies:
            if source.read_bytes() != content or target.read_bytes() != content:
                raise ValueError("Snapshot changed during migration; verified copies are preserved")
            source.unlink()
        if old_snapshots.exists():
            for path in sorted(old_snapshots.rglob("*"), key=lambda p: len(p.parts), reverse=True):
                if path.is_dir() and not any(path.iterdir()): path.rmdir()
            if not any(old_snapshots.iterdir()): old_snapshots.rmdir()
        parent = root / ".snapshots"
        if parent.exists() and not any(parent.iterdir()): parent.rmdir()
        completed = True
        return receipt
    finally:
        # Removing a lock inode is safe only after the caller has quiesced old writers.
        if completed and old_lock.exists(): old_lock.unlink()
        for stream in reversed(locks): stream.close()

def write(root, source, expected, action_id=None, card_id=None, result_message="", refresh_id=None):
    if not isinstance(result_message, str) or len(result_message) > 200:
        raise ValueError("Result message must be at most 200 characters")
    root = root.resolve(strict=True)
    if root == Path("/") or not root.is_dir():
        raise ValueError("Choose an explicit workspace")
    raw = source.read_bytes()
    candidate = validate(raw, root)
    if candidate.get("example_only") is True:
        raise ValueError("Example data must not replace a real overview")
    if (action_id is None) != (card_id is None):
        raise ValueError("action_id and card_id must be supplied together")
    if action_id is not None and any(not re.fullmatch(r"[A-Za-z0-9_-]{1,96}", value) for value in (action_id, card_id)):
        raise ValueError("Invalid action or card id")
    target = target_for_workspace(root)
    directory = target.parent
    if target.is_symlink():
        raise ValueError("The daily brief must not be a symlink")
    lock_path = directory / ".hermes-today.lock"
    fd = os.open(lock_path, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW, 0o600)
    with os.fdopen(fd, "r+") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        if target_for_workspace(root) != target:
            raise ValueError("Overview storage changed; re-read before writing")
        before = target.read_bytes() if target.exists() else None
        actual = hashlib.sha256(before).hexdigest() if before is not None else "missing"
        try:
            previous = json.loads(before) if before else {}
            receipts = previous.get("action_receipts", []) if isinstance(previous, dict) else []
            if not isinstance(receipts, list):
                raise ValueError("Invalid existing action receipts")
            receipts = [r for r in receipts if isinstance(r, dict)]
        except (json.JSONDecodeError, UnicodeDecodeError):
            previous = {}
            receipts = []  # Format-repair mode may be replacing a malformed old file.
        if action_id is not None:
            found = next((r for r in receipts if r.get("operation_id") == action_id), None)
            if found:
                if found.get("card_id") != card_id:
                    raise ValueError("Action id belongs to a different card")
                return actual
        refresh_receipts = previous.get("refresh_receipts", []) if isinstance(previous, dict) else []
        if not isinstance(refresh_receipts, list): raise ValueError("Invalid refresh receipts")
        refresh_receipts = [r for r in refresh_receipts if isinstance(r, dict)][-20:]
        if refresh_id is not None:
            if not re.fullmatch(r"[A-Za-z0-9_-]{1,96}", refresh_id): raise ValueError("Invalid refresh id")
            if any(r.get("request_id") == refresh_id and r.get("status") == "applied" for r in refresh_receipts): return actual
        if expected != actual:
            raise ValueError("The current file changed. Read and merge it before retrying.")
        if action_id is None and isinstance(previous, dict):
            old_cards = previous.get("cards", [])
            if not isinstance(old_cards, list):
                old_cards = []
            closed = {card.get("id") for card in old_cards if isinstance(card, dict) and card.get("status") in {"done", "archived"}}
            if any(card["id"] in closed and card.get("status", "open") not in {"done", "archived"} for card in candidate["cards"]):
                raise ValueError("A scheduled refresh cannot reopen a closed card. Preserve its status; record genuinely new work separately.")
        if action_id is not None:
            if not any(card["id"] == card_id for card in candidate["cards"]):
                raise ValueError("The action target must remain in the overview")
            receipts = receipts[-199:] + [{"operation_id": action_id, "card_id": card_id, "status": "applied",
                                          "processed_at": dt.datetime.now(dt.timezone.utc).isoformat(), "message": result_message}]
        if refresh_id is not None:
            refresh_receipts = refresh_receipts[-19:] + [{"request_id": refresh_id, "status": "applied",
                "processed_at": dt.datetime.now(dt.timezone.utc).isoformat()}]
        refresh_changed = candidate.get("refresh_receipts", []) != refresh_receipts
        if refresh_receipts or "refresh_receipts" in candidate: candidate["refresh_receipts"] = refresh_receipts
        if refresh_changed or candidate.get("action_receipts", []) != receipts[-200:]:
            candidate["action_receipts"] = receipts[-200:]
            raw = (json.dumps(candidate, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
            validate(raw, root)
        if before is not None:
            snapshots = root / ".snapshots" / "hermes-today" if directory == root else directory / "snapshots"
            if snapshots.is_symlink() or not snapshots.resolve().is_relative_to(root):
                raise ValueError("Snapshot directory escapes the workspace")
            snapshots.mkdir(parents=True, exist_ok=True)
            snapshot = snapshots / (dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex + ".json")
            with snapshot.open("xb") as stream:
                stream.write(before)
                stream.flush()
                os.fsync(stream.fileno())
        descriptor, temporary = tempfile.mkstemp(prefix=".hermes-today-", suffix=".tmp", dir=directory)
        try:
            with os.fdopen(descriptor, "wb") as stream:
                stream.write(raw)
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, target)
            directory_fd = os.open(directory, os.O_RDONLY)
            try:
                os.fsync(directory_fd)
            finally:
                os.close(directory_fd)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
        return hashlib.sha256(raw).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--input", type=Path)
    parser.add_argument("--migrate-storage", action="store_true")
    parser.add_argument("--writers-stopped", action="store_true")
    parser.add_argument("--refresh-id")
    parser.add_argument("--expected-sha256",
                        help="SHA-256 of the current file; use 'missing' only for first creation.")
    parser.add_argument("--action-id", help="Idempotency key for this already-applied card operation")
    parser.add_argument("--card-id", help="Card targeted by --action-id")
    parser.add_argument("--result-message", default="", help="Brief factual description of the confirmed update")
    parser.add_argument("--legacy-format-repair", action="store_true", help="Only for repairing syntax of an existing legacy file, never for an upgrade")
    arguments = parser.parse_args()
    if arguments.migrate_storage:
        print(json.dumps(migrate_storage(arguments.workspace, arguments.writers_stopped), ensure_ascii=False))
        return
    if arguments.input is None or arguments.expected_sha256 is None:
        parser.error("--input and --expected-sha256 are required for a write")
    candidate = validate(arguments.input.read_bytes(), arguments.workspace.resolve())
    if not arguments.legacy_format_repair and candidate.get("presentation_version") != 4:
        parser.error("This writer requires presentation_version 4 for new briefings. Legacy repair preserves older content without changing its meaning.")
    digest = write(arguments.workspace, arguments.input, arguments.expected_sha256, arguments.action_id, arguments.card_id, arguments.result_message, arguments.refresh_id)
    value = json.loads(target_for_workspace(arguments.workspace.resolve()).read_text(encoding="utf-8"))
    counts = {}
    for card in value["cards"]:
        p = card.get("presentation", {})
        key = p.get("interaction", {}).get("type") or p.get("layout") or "legacy"
        counts[key] = counts.get(key, 0) + 1
    print(json.dumps({"sha256": digest, "presentation_version": value.get("presentation_version", 0), "cards": len(value["cards"]), "layouts": counts}, ensure_ascii=False))


if __name__ == "__main__":
    main()
