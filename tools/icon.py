"""Writes the launcher icon vectors: an enamel star badge with a gold rim, on red felt.

    python tools/icon.py
then `./gradlew testDebugUnitTest -Prender --tests '*RenderArtIcon*'` draws docs/logo.png from them.
"""
import math
from pathlib import Path

DRAW = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"
HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def star(cx, cy, r, inner=.5, n=5):
    pts = []
    for i in range(n * 2):
        a = -math.pi / 2 + i * math.pi / n
        rr = r if i % 2 == 0 else r * inner
        pts.append(f"{cx + math.cos(a) * rr:.2f},{cy + math.sin(a) * rr:.2f}")
    return "M" + "L".join(pts) + "Z"


def circle(cx, cy, r):
    return f"M{cx - r},{cy}A{r},{r} 0,1 1,{cx + r},{cy}A{r},{r} 0,1 1,{cx - r},{cy}Z"


def half(cx, cy, r, inner=.5, n=5):
    """The lit half of each ray, for the faceted look of a stamped star."""
    out = []
    for i in range(n):
        a = -math.pi / 2 + i * 2 * math.pi / n
        b = a + math.pi / n
        out.append(f"M{cx:.2f},{cy:.2f}L{cx + math.cos(a) * r:.2f},{cy + math.sin(a) * r:.2f}L{cx + math.cos(b) * r * inner:.2f},{cy + math.sin(b) * r * inner:.2f}Z")
    return "".join(out)


def main():
    cx, cy = 54, 55.5
    p = [
        ("#40000000", star(cx + 1.2, cy + 2, 31)),
        ("#FFA9832A", star(cx, cy, 31)),
        ("#FFF2CF6A", half(cx, cy, 31)),
        ("#FF8E1A14", star(cx, cy, 25.5)),
        ("#FFC8302B", star(cx, cy, 24.5)),
        ("#33FFFFFF", half(cx, cy, 24.5)),
        ("#FFA9832A", circle(cx, cy, 10.2)),
        ("#FFF1EADB", circle(cx, cy, 8.8)),
        ("#FFD8AB3A", star(cx, cy + .4, 6.6, .45)),
    ]
    body = "\n".join(f'    <path android:fillColor="{c}" android:pathData="{d}" />' for c, d in p)
    (DRAW / "ic_launcher_foreground.xml").write_text(HEAD + body + "\n</vector>\n", encoding="utf-8", newline="\n")
    (DRAW / "ic_launcher_background.xml").write_text(HEAD + '    <path android:fillColor="#FFF2EAD8" android:pathData="M0,0h108v108h-108z" />\n</vector>\n', encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main()
