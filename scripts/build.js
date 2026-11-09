// A weboldal (index.html) és a telefonos "híd" (src/bridge.js) összeállítása a www mappába.
// Az index.html UGYANAZ a fájl, mint a GitHub Pages-es webes AI-Aréna: nem kell külön szerkeszteni.
const fs = require('fs');
const path = require('path');
const esbuild = require('esbuild');

const root = path.join(__dirname, '..');
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));
const www = path.join(root, 'www');
fs.mkdirSync(www, { recursive: true });

esbuild.buildSync({
  entryPoints: [path.join(root, 'src/bridge.js')],
  bundle: true,
  outfile: path.join(www, 'bridge.js'),
  target: 'es2019',
  define: { APK_VERSION: JSON.stringify(pkg.version) }
});

let html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const tag = '<script>';
const i = html.indexOf(tag);
if (i < 0) { console.error('HIBA: az index.html-ben nincs <script> elem.'); process.exit(1); }
if (html.indexOf('bridge.js') >= 0) { console.error('HIBA: az index.html már tartalmazza a bridge.js hivatkozást.'); process.exit(1); }
html = html.slice(0, i) + '<script src="bridge.js"></script>\n' + html.slice(i);
fs.writeFileSync(path.join(www, 'index.html'), html);
console.log('www/index.html és www/bridge.js kész (APK v' + pkg.version + ')');
