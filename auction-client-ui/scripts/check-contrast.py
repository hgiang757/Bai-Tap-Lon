"""Measure the palette pairs used by app.css (WCAG relative luminance)."""
from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parent.parent
css = (root / 'src/main/resources/com/auction/client/css/app.css').read_text(encoding='utf-8')
colors = dict(re.findall(r'(-c-[\w-]+):\s*(#[0-9a-fA-F]{6});', css))

def luminance(color):
    channels = [int(color[i:i+2], 16) / 255 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in channels]
    return sum(v * weight for v, weight in zip(linear, (0.2126, 0.7152, 0.0722)))

def contrast(first, second):
    light, dark = sorted((luminance(colors[first]), luminance(colors[second])), reverse=True)
    return (light + 0.05) / (dark + 0.05)

pairs = [(fg, bg, 4.5) for fg in ('-c-text', '-c-muted', '-c-primary', '-c-success', '-c-warning', '-c-danger', '-c-accent')
         for bg in ('-c-bg', '-c-surface', '-c-surface-2')]
pairs += [('-c-on-primary', bg, 4.5) for bg in ('-c-primary', '-c-primary-hover', '-c-text')]
pairs += [('-c-border-input', bg, 3) for bg in ('-c-bg', '-c-surface', '-c-surface-2')]
pairs += [('-c-primary', bg, 3) for bg in ('-c-bg', '-c-surface', '-c-surface-2')]
failed = []
for first, second, minimum in pairs:
    ratio = contrast(first, second)
    print(f'{first} / {second}: {ratio:.3f}:1 (minimum {minimum}:1)')
    if ratio < minimum:
        failed.append((first, second, ratio))
print(f'Contrast scan: {len(pairs)} pairs; {len(failed)} failures')
sys.exit(bool(failed))
