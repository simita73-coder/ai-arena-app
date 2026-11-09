# AI-Aréna (Android APK) – v1.0.0

Az **AI-Aréna Web** (`index.html`) Android-alkalmazásként. Az `index.html` **ugyanaz a fájl**, mint a GitHub Pages-es webes változat, nem kell külön szerkeszteni:
az APK építésekor egy kis „híd" fájl (`src/bridge.js`) kerül mellé, ez oldja meg a telefonos különbségeket.

## Mit tud az APK a webes változathoz képest
- **Natív hálózati hívás** (a Capacitor `CapacitorHttp`-je): nincs CORS-korlát, bármelyik AI hívható.
- **Mentés a Letöltések mappába**: a 💾 mentés a `Letöltések/AI-Arena/` mappába írja a txt-t (engedély nem kell). Az Android WebView a böngészős letöltést nem tudja, ezt oldja meg a híd.
- **Források / linkek** az alapértelmezett böngészőben nyílnak meg.
- **Vissza gomb**: felugró ablak bezárása → teljes képernyős nézetből vissza → az app a háttérbe megy (a beszélgetés megmarad).
- **■ Megszakítás**: a felület azonnal felszabadul (a már elküldött kérést az AI-szolgáltató ettől még végigszámolhatja és felszámolhatja).
- A fejlécben a verzió: `Web v1.13.0 · APK v1.0.0` – így látszik, melyik APK van telepítve.
- Saját ikon (két kard), a név: **AI-Aréna**.

## Fontos tudnivalók
- Az APK **külön app**, a beállításai (API-kulcsok, PIN, limit, egyenleg) **nem azonosak** a böngészős AI-Aréna beállításaival. Az első indításkor a ⚙ Beállításokban újra meg kell adni a kulcsokat.
- Frissítésnél (új APK a régi fölé) a beállítások megmaradnak, mert az aláírókulcs ugyanaz (`android-extra/debug.keystore`, azonos a többi appodéval). Eltávolításkor viszont elvesznek.
- **Diktálás (🎤)**: az Android WebView-ban nincs beépített beszédfelismerés, használd a billentyűzet mikrofon gombját. A felolvasás (🔊) a telefon saját hangját használja; ha nem szól, jelezd.
- A beszélgetések csak a memóriában élnek: ha az Android leállítja az appot, elvesznek. Fontos beszélgetést mentsd el.

## Új repó létrehozása és az APK elkészítése (egyszer)
1. github.com → **+ → New repository** → név: `ai-arena-app`, **Public** → Create.
2. **uploading an existing file** → húzd be a Windows Intézőből ezeket **(a mappákat magukat, nem a bennük lévő fájlokat)**:
   `index.html`, `package.json`, `package-lock.json`, `capacitor.config.json`, `README.md`, `scripts` mappa, `src` mappa, `android-extra` mappa.
   Ellenőrizd a listát, majd **Commit changes**.
3. Hozd létre kézzel a munkafolyamat-fájlt: **Add file → Create new file**, a névmezőbe: `.github/workflows/build-apk.yml`, a szövegmezőbe másold be a `build-apk.yml` tartalmát, **Commit changes**.
   (Az ilyen ponttal kezdődő mappák húzással nem kerülnek fel.) Ez a commit elindítja az építést.
4. **Actions** fül → a legfelső futás: sárga pörgő pont = épül (3-5 perc), zöld pipa = kész, piros X = hiba.
5. Letöltés a telefon böngészőjével: `github.com/simita73-coder/ai-arena-app/releases/latest/download/ai-arena.apk`
6. Telepítés, majd ⚙ Beállítások → API-kulcsok megadása.

Piros X esetén: kattints a futásra → bal oldalt `build` → a piros X-szel jelölt lépés → a vörös hibaüzenetről képernyőkép.

## Frissítés később
- **Új webes AI-Aréna verzió (index.html):** töltsd fel ugyanazt az `index.html`-t a `ai-arena` repóba (Pages) **és** az `ai-arena-app` repóba is. Az APK magától újraépül.
- **Csak az APK változik (híd, Java, ikon):** emeld a `package.json` `version` számát (a fejlécben ez látszik), és töltsd fel a módosított fájlokat.

## Fájlok
- `index.html` – az AI-Aréna Web (a Pages-es fájl másolata)
- `src/bridge.js` – a telefonos híd (mentés, linkek, Vissza gomb, megszakítás, verziószám)
- `scripts/build.js` – összeállítja a `www` mappát (index.html + híd)
- `scripts/patch_android.py` – a generált Android-projekt kiegészítése (plugin, ikon, aláírás)
- `android-extra/ArenaPlugin.java` – natív rész: txt mentése a Letöltésekbe, webcím megnyitása
- `android-extra/MainActivity.java` – a plugin regisztrálása
- `android-extra/res/` – az app ikonja (két kard)
- `android-extra/debug.keystore` – állandó debug aláírókulcs
- `build-apk.yml` – a GitHub Actions munkafolyamat (a repóban: `.github/workflows/build-apk.yml`)
