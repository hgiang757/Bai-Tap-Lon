from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parent.parent
pattern = re.compile(r"[A-Za-z]\?[A-Za-z]|\?{2,}|\ufffd")
errors = []
for folder in (root / "src/main", root / "docs/ui"):
    for path in folder.rglob("*"):
        if not path.is_file() or path.suffix not in {".java", ".fxml", ".css", ".properties", ".md"}:
            continue
        try:
            content = path.read_text(encoding="utf-8")
        except UnicodeDecodeError as exc:
            errors.append(f"{path.relative_to(root)}: invalid UTF-8: {exc}")
            continue
        for number, line in enumerate(content.splitlines(), 1):
            if pattern.search(line):
                errors.append(f"{path.relative_to(root)}:{number}: {line.strip()}")
print("\n".join(errors) if errors else "Encoding scan: 0 matches")
sys.exit(bool(errors))
