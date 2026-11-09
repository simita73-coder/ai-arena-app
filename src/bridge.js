// AI-Aréna Android "híd": a webes index.html változtatás nélkül fut az APK-ban.
// Ez a fájl csak a telefonos különbségeket oldja meg.
import { registerPlugin } from '@capacitor/core';
import { App } from '@capacitor/app';

const Native = registerPlugin('ArenaNative');
window.ArenaNative = Native;

/* 1) Hálózat: a Capacitor natív HTTP-je (config: CapacitorHttp.enabled) már átveszi a fetch-et, így nincs CORS-gond.
      A natív kérés nem megszakítható, ezért a Megszakítás (■) gombnál a hívást "lecsatoljuk": a felület azonnal felszabadul. */
const baseFetch = window.fetch.bind(window);
window.fetch = function (input, init) {
  const sig = init && init.signal;
  const p = baseFetch(input, init);
  if (!sig) return p;
  return new Promise((resolve, reject) => {
    const onAbort = () => reject(new DOMException('Aborted', 'AbortError'));
    if (sig.aborted) { onAbort(); return; }
    sig.addEventListener('abort', onAbort, { once: true });
    p.then(
      r => { sig.removeEventListener('abort', onAbort); resolve(r); },
      e => { sig.removeEventListener('abort', onAbort); reject(e); }
    );
  });
};

/* 2) Kis üzenet (ugyanazzal a .toast elemmel, mint a weboldal) */
let toastTimer;
function toast(msg, ms) {
  let t = document.querySelector('.toast');
  if (!t) { t = document.createElement('div'); t.className = 'toast'; document.body.appendChild(t); }
  t.textContent = msg; t.style.display = 'block';
  clearTimeout(toastTimer); toastTimer = setTimeout(() => { t.style.display = 'none'; }, ms || 5000);
}

/* 3) Mentés: az Android WebView nem kezeli a blob-letöltést (<a download>), ezért a fájl a telefon
      Letöltések/AI-Arena mappájába kerül (engedély nem kell hozzá). */
const blobs = new Map();
const origCreate = URL.createObjectURL.bind(URL);
URL.createObjectURL = function (obj) {
  const u = origCreate(obj);
  if (typeof Blob !== 'undefined' && obj instanceof Blob) blobs.set(u, obj);
  return u;
};
const origRevoke = URL.revokeObjectURL.bind(URL);
URL.revokeObjectURL = function (u) { blobs.delete(u); return origRevoke(u); };

const readText = blob => blob.text ? blob.text() : new Promise((res, rej) => { const fr = new FileReader(); fr.onload = () => res(fr.result); fr.onerror = () => rej(fr.error); fr.readAsText(blob); });
async function saveBlob(blob, name) {
  try {
    const text = await readText(blob);
    const r = await Native.saveText({ name, text });
    toast('💾 Elmentve: ' + ((r && r.path) || name), 6000);
  } catch (e) {
    toast('A mentés nem sikerült: ' + ((e && e.message) || e), 6000);
  }
}
const origClick = HTMLAnchorElement.prototype.click;
HTMLAnchorElement.prototype.click = function () {
  if (this.hasAttribute('download') && /^blob:/.test(this.href)) {
    const blob = blobs.get(this.href);
    if (blob) { saveBlob(blob, this.getAttribute('download') || 'AI-Arena.txt'); return; }
  }
  return origClick.apply(this, arguments);
};

/* 4) Külső linkek (források): az alapértelmezett böngészőben nyílnak meg */
document.addEventListener('click', e => {
  const a = e.target && e.target.closest ? e.target.closest('a[href]') : null;
  if (!a) return;
  const h = a.href || '';
  if (/^https?:\/\//i.test(h) && h.indexOf(location.origin) !== 0) {
    e.preventDefault();
    Native.openUrl({ url: h }).catch(() => toast('A link nem nyitható meg.'));
  }
}, true);

/* 5) Android "Vissza" gomb: felugró ablak bezárása → teljes képernyős nézetből vissza → az app a háttérbe (a beszélgetés megmarad) */
App.addListener('backButton', () => {
  try {
    const d = document.querySelector('dialog[open]');
    if (d) {
      const ev = new Event('cancel', { cancelable: true });
      d.dispatchEvent(ev);
      if (!ev.defaultPrevented) { try { d.close(); } catch (e) {} }
      return;
    }
    if (document.body.classList.contains('full')) {
      const b = document.getElementById('fBack');
      if (b) { b.click(); return; }
    }
    App.minimizeApp();
  } catch (e) {
    try { App.minimizeApp(); } catch (e2) {}
  }
});

/* 6) Verziószám a fejlécben: így látszik, melyik APK van telepítve */
document.addEventListener('DOMContentLoaded', () => {
  const s = document.querySelector('.brand small');
  if (s) s.textContent += ' · APK v' + APK_VERSION;
});
