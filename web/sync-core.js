// Online sync between devices.
// Wraps a local storage bridge (browser or desktop) and mirrors every record to a cloud
// backend, one document per booking / cash entry plus one "config" document for lists
// and settings. Last change wins per record; deletions are kept as small "tombstones"
// so other devices learn about them, even devices that were offline at the time.
//
// The backend is an adapter (see firebase-backend.js) so this logic can be tested
// without a network.

const COLLS = ['bookings', 'cash'];
const META_ID = 'config';

function sortKeys(v) {
  if (Array.isArray(v)) return v.map(sortKeys);
  if (v && typeof v === 'object') {
    const o = {};
    for (const k of Object.keys(v).sort()) if (v[k] !== undefined) o[k] = sortKeys(v[k]);
    return o;
  }
  return v;
}
function stable(rec) {
  // JSON of a record (keys sorted at every level) without the sync timestamp,
  // used to detect real changes
  const { _ts, ...rest } = rec;
  return JSON.stringify(sortKeys(rest));
}

export function createSync(base, backend) {
  let local = null;            // latest full data object
  let appOwned = false;        // true once the app itself has loaded or saved data on this device
  let uid = null;
  let email = '';
  let unsubs = [];
  let synced = new Map();      // key -> stable json (or '__deleted__') last known to match server
  let initialPending = 0;      // snapshots still to arrive after sign-in
  let pushing = Promise.resolve();
  let status = backend ? 'signed-out' : 'off';
  let lastError = '';
  const listeners = { remote: [], status: [] };

  const emit = (type, v) => listeners[type].forEach(cb => { try { cb(v); } catch (e) { console.error(e); } });
  function setStatus(s, err) {
    status = s; lastError = err || '';
    emit('status', { status, email, error: lastError });
  }
  const key = (coll, id) => coll + ':' + id;

  function ensureLocal() {
    if (!local) local = { bookings: [], cash: [], lists: {}, settings: {} };
    if (!Array.isArray(local.bookings)) local.bookings = [];
    if (!Array.isArray(local.cash)) local.cash = [];
    return local;
  }

  function metaOf(d) { return { settings: d.settings || {}, lists: d.lists || {} }; }

  // ---------- local -> cloud ----------
  function buildOps(d) {
    const ops = [];
    const now = Date.now();
    const seen = new Set();
    for (const coll of COLLS) {
      for (const rec of d[coll]) {
        if (!rec || !rec.id) continue;
        const k = key(coll, rec.id);
        seen.add(k);
        const s = stable(rec);
        if (synced.get(k) !== s) {
          rec._ts = Math.max(now, (rec._ts || 0) + 1);
          ops.push({ coll, id: rec.id, data: { ...rec } });
          synced.set(k, s);
        }
      }
    }
    // deletions: anything we know the server has that is no longer local
    for (const [k, s] of synced) {
      if (s === '__deleted__' || k.startsWith('meta:') || seen.has(k)) continue;
      const [coll, id] = [k.slice(0, k.indexOf(':')), k.slice(k.indexOf(':') + 1)];
      ops.push({ coll, id, data: { id, deleted: true, _ts: now } });
      synced.set(k, '__deleted__');
    }
    const m = metaOf(d);
    const ms = stable(m);
    // A brand-new device must not overwrite the account's lists with its empty defaults
    if (appOwned && synced.get('meta:' + META_ID) !== ms) {
      d._metaTs = Math.max(now, (d._metaTs || 0) + 1);
      ops.push({ coll: 'meta', id: META_ID, data: { ...m, _ts: d._metaTs } });
      synced.set('meta:' + META_ID, ms);
    }
    return ops;
  }

  function push() {
    if (!uid || initialPending > 0 || !local) return pushing;
    const ops = buildOps(local);
    if (!ops.length) return pushing;
    const myUid = uid;
    setStatus('syncing');
    pushing = pushing.then(() => backend.write(myUid, ops)).then(() => {
      if (uid === myUid) setStatus('synced');
    }).catch(err => {
      // Forget what we claimed to have synced so the next save retries these records
      ops.forEach(o => synced.delete(key(o.coll, o.id)));
      if (uid === myUid) setStatus('error', err && err.message || String(err));
    });
    return pushing;
  }

  // ---------- cloud -> local ----------
  function applyRemote(coll, changes) {
    const d = ensureLocal();
    let changed = false;
    for (const ch of changes) {
      if (ch.pending) continue; // our own write echoing back
      const r = ch.data || {};
      if (coll === 'meta') {
        if (ch.id !== META_ID) continue;
        if ((r._ts || 0) > (d._metaTs || 0)) {
          if (r.settings) d.settings = r.settings;
          if (r.lists) d.lists = { ...d.lists, ...r.lists };
          d._metaTs = r._ts;
          changed = true;
        }
        synced.set('meta:' + META_ID, stable(metaOf(r)));
        continue;
      }
      const k = key(coll, ch.id);
      const list = d[coll];
      const i = list.findIndex(x => x.id === ch.id);
      const mine = i >= 0 ? list[i] : null;
      if (ch.type === 'remove' || r.deleted) {
        if (mine && (r._ts || Infinity) >= (mine._ts || 0)) { list.splice(i, 1); changed = true; }
        if (!mine || (r._ts || Infinity) >= (mine._ts || 0)) synced.set(k, '__deleted__');
        continue;
      }
      const { deleted, ...rec } = r;
      if (!mine) { list.push(rec); changed = true; synced.set(k, stable(rec)); }
      else if ((rec._ts || 0) >= (mine._ts || 0)) {
        if (stable(mine) !== stable(rec)) { list[i] = rec; changed = true; }
        synced.set(k, stable(rec));
      }
      // else: our copy is newer; leave synced unset so push() sends it
    }
    return changed;
  }

  function onSnapshot(coll, changes, fromInitial) {
    const changed = applyRemote(coll, changes);
    if (changed) {
      appOwned = true;
      Promise.resolve(base.save(local)).catch(() => { });
      emit('remote', local);
    }
    if (fromInitial) {
      initialPending--;
      if (initialPending === 0) { setStatus('synced'); push(); }
    }
  }

  function stop() {
    unsubs.forEach(u => { try { u(); } catch (_) { } });
    unsubs = [];
  }

  function start(user) {
    stop();
    uid = user.uid; email = user.email || '';
    synced = new Map();
    initialPending = COLLS.length + 1;
    setStatus('syncing');
    for (const coll of [...COLLS, 'meta']) {
      // Wait for the first answer from the server (not just the local cache) before
      // uploading, so a device with an old copy cannot bring back records that were
      // deleted elsewhere while it was away.
      let waitingForServer = true;
      unsubs.push(backend.subscribe(uid, coll, (changes, info) => {
        const fromServer = !(info && info.fromCache);
        const initial = waitingForServer && fromServer;
        if (initial) waitingForServer = false;
        onSnapshot(coll, changes, initial);
      }, err => setStatus('error', err && err.message || String(err))));
    }
  }

  if (backend) {
    backend.onAuth(user => {
      if (user) start(user);
      else { stop(); uid = null; email = ''; synced = new Map(); setStatus('signed-out'); }
    });
  }

  // The bridge the app talks to: same API as the local one, plus sync controls
  return {
    ...base,
    async load() { local = await base.load(); appOwned = !!local; return local; },
    async save(d) {
      local = d;
      appOwned = true;
      const r = await base.save(d);
      push();
      return r;
    },
    sync: {
      available: !!backend,
      get status() { return { status, email, error: lastError }; },
      onStatus(cb) { listeners.status.push(cb); cb({ status, email, error: lastError }); },
      onRemoteChange(cb) { listeners.remote.push(cb); },
      signIn: (e, p) => backend.signIn(e, p),
      signUp: (e, p) => backend.signUp(e, p),
      signOut: () => backend.signOut(),
      resetPassword: e => backend.resetPassword(e),
      flush: () => push(),
    },
  };
}
