"""Check the client file map and local Markdown links using only Python's standard library."""
from pathlib import Path
import re
import sys
from urllib.parse import unquote

root = Path(__file__).resolve().parent.parent
code_map = root / "docs/CODE_MAP.md"
errors = []
content = code_map.read_text(encoding="utf-8")
required = {root / "pom.xml"}
required.update(path for path in (root / "src").rglob("*")
                if path.is_file() and path.suffix in {".java", ".fxml", ".css", ".properties"})
required.update((root / "scripts").glob("*.py"))
for path in sorted(required):
    if path.relative_to(root).as_posix() not in content:
        errors.append(f"CODE_MAP missing file: {path.relative_to(root)}")

for document in [root / "README.md", code_map, root / "docs/CLIENT_HANDOVER.md", root / "docs/VALIDATION.md",
                 root / "docs/UI_REFRESH.md", root / "docs/ui/DESIGN.md", root / "docs/IMAGE_CREDITS.md"]:
    text = document.read_text(encoding="utf-8")
    for target in re.findall(r"\]\(([^)]+)\)", text):
        target = unquote(target.split("#", 1)[0].strip("<>"))
        if not target or "://" in target or target.startswith("mailto:"):
            continue
        if not (document.parent / target).is_file():
            errors.append(f"{document.name}: missing local link: {target}")
    if text.count("```") % 2:
        errors.append(f"{document.name}: unclosed fenced code block")

print("\n".join(errors) if errors else f"Documentation scan: {len(required)} code/config files covered; local links OK")
sys.exit(bool(errors))
