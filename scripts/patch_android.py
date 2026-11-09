"""A `npx cap add android` által létrehozott Android-projekt kiegészítése:
- a saját natív plugin (mentés a Letöltésekbe, linknyitás) és a MainActivity bemásolása,
- az AI-Aréna ikon (android-extra/res) és az ikon háttérszíne,
- állandó aláírókulcs (így az új verzió a régire ráfrissíthető, a beállítások és API-kulcsok megmaradnak).
A GitHub Actions futtatja, a projekt gyökeréből: python3 scripts/patch_android.py
"""
import json
import pathlib
import shutil
import sys

cfg = json.loads(pathlib.Path("capacitor.config.json").read_text(encoding="utf-8"))
app_id = cfg["appId"]
root = pathlib.Path("android")
if not root.is_dir():
    sys.exit("Nincs android mappa: előbb futtasd az `npx cap add android` parancsot.")

# 1) fájlok a csomagnak megfelelő mappába
pkg_dir = root / "app/src/main/java" / pathlib.Path(*app_id.split("."))
pkg_dir.mkdir(parents=True, exist_ok=True)
for name in ("ArenaPlugin.java", "MainActivity.java"):
    src = pathlib.Path("android-extra") / name
    (pkg_dir / name).write_text(src.read_text(encoding="utf-8").replace("__PACKAGE__", app_id), encoding="utf-8")
    print("Bemásolva:", pkg_dir / name)

# 2) ikon: a mipmap-* mappák tartalma felülírja a Capacitor alapikonját
res_src = pathlib.Path("android-extra/res")
res_dst = root / "app/src/main/res"
n = 0
if res_src.is_dir():
    for f in sorted(res_src.rglob("*")):
        if f.is_file():
            target = res_dst / f.relative_to(res_src)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(f, target)
            n += 1
print("Ikonfájlok bemásolva:", n)
bg = res_dst / "values/ic_launcher_background.xml"
bg.parent.mkdir(parents=True, exist_ok=True)
bg.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="ic_launcher_background">#1B1D1F</color>\n</resources>\n', encoding="utf-8")

# 3) állandó aláírókulcs
shutil.copyfile("android-extra/debug.keystore", root / "app/debug.keystore")
gradle = root / "app/build.gradle"
g = gradle.read_text(encoding="utf-8")
if "debug.keystore" not in g:
    g += """

// Állandó debug aláírás (a patch_android.py adta hozzá)
android {
    signingConfigs {
        debug {
            storeFile file('debug.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
    }
}
"""
    gradle.write_text(g, encoding="utf-8")
print("Aláírókulcs beállítva.")
