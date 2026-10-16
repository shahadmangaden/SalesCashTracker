// Browser implementation of window.api (the desktop app gets this from Electron's preload).
// Data lives in this browser's IndexedDB, with a daily snapshot kept for recovery.
// Excel code is large, so it is only downloaded when import/export is used.
const excel = () => import('../lib/excel-core').then(m => m.default || m);

const DB_NAME = 'sales-cash-tracker';
const STORE = 'kv';
const KEEP_SNAPSHOTS = 30;
const VERSION = typeof __APP_VERSION__ !== 'undefined' ? __APP_VERSION__ : 'dev';

function openDB() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, 1);
    req.onupgradeneeded = () => req.result.createObjectStore(STORE);
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}
const dbp = openDB();
async function tx(mode, fn) {
  const db = await dbp;
  return new Promise((resolve, reject) => {
    const t = db.transaction(STORE, mode);
    const store = t.objectStore(STORE);
    const out = fn(store);
    t.oncomplete = () => resolve(out && 'result' in out ? out.result : undefined);
    t.onerror = () => reject(t.error);
    t.onabort = () => reject(t.error || new Error('Storage aborted'));
  });
}
const get = key => tx('readonly', s => s.get(key));
const put = (key, val) => tx('readwrite', s => { s.put(val, key); });
const del = key => tx('readwrite', s => { s.delete(key); });
const keys = () => tx('readonly', s => s.getAllKeys());

function today() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function pickFile(accept) {
  return new Promise(resolve => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    input.onchange = () => { resolve(input.files[0] || null); input.remove(); };
    // If the picker is cancelled, browsers fire no event; resolve null when focus returns.
    window.addEventListener('focus', () => setTimeout(() => { if (!input.files.length) { resolve(null); input.remove(); } }, 1000), { once: true });
    document.body.appendChild(input);
    input.click();
  });
}

function download(blob, name) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = name;
  document.body.appendChild(a); a.click(); a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 5000);
}

// Ask the browser not to evict our data under storage pressure.
if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => { });

// One-time migration from the older localStorage preview format
async function migrate() {
  try {
    const old = localStorage.getItem('sct-data');
    if (old && !(await get('data'))) { await put('data', JSON.parse(old)); localStorage.removeItem('sct-data'); }
  } catch (_) { }
}
const ready = migrate();

window.api = {
  platform: 'web',
  async load() { await ready; return (await get('data')) || null; },
  async save(data) {
    await put('data', data);
    await put('snapshot:' + today(), data);
    const snaps = (await keys()).filter(k => String(k).startsWith('snapshot:')).sort();
    while (snaps.length > KEEP_SNAPSHOTS) await del(snaps.shift());
    return true;
  },
  async info() {
    let usage = '';
    try {
      const est = await navigator.storage.estimate();
      usage = `${(est.usage / 1048576).toFixed(1)} MB used`;
    } catch (_) { }
    return { platform: 'web', dataFile: 'This browser (' + location.host + ')' + (usage ? ' · ' + usage : ''), backupDir: 'Daily snapshots in this browser', version: VERSION };
  },
  async listSnapshots() {
    return (await keys()).filter(k => String(k).startsWith('snapshot:')).map(k => String(k).slice(9)).sort().reverse();
  },
  async getSnapshot(date) { return get('snapshot:' + date); },
  async openDataFolder() { },
  async importExcel() {
    const f = await pickFile('.xlsx,.xlsm,.xls');
    if (!f) return null;
    const { importWorkbookData } = await excel();
    return importWorkbookData(await f.arrayBuffer(), 'array');
  },
  async exportExcel(data, name) {
    const { buildWorkbook } = await excel();
    const buf = await buildWorkbook(data);
    download(new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), name || 'Sales-Cash-Tracker.xlsx');
    return name;
  },
  async saveBackup(data) {
    const name = `tracker-backup-${today()}.json`;
    download(new Blob([JSON.stringify(data, null, 1)], { type: 'application/json' }), name);
    return name;
  },
  async restoreBackup() {
    const f = await pickFile('.json,application/json');
    if (!f) return null;
    const d = JSON.parse(await f.text());
    if (!d || !Array.isArray(d.bookings)) throw new Error('This file is not a tracker backup.');
    return d;
  },
  async confirm(message, detail) { return window.confirm(message + (detail ? '\n\n' + detail : '')); },
};
