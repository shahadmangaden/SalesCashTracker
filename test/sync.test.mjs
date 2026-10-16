// Simulates several devices syncing through a fake cloud backend.
// Run: node test/sync.test.mjs
import assert from 'node:assert/strict';
import { createSync } from '../web/sync-core.js';

const tick = () => new Promise(r => setTimeout(r, 5));
const settle = async () => { for (let i = 0; i < 20; i++) await tick(); };

// ---------- Fake cloud (behaves like Firestore with offline queueing) ----------
function makeCloud() {
  const store = {}; // uid -> coll -> id -> data
  const subs = [];  // {uid, coll, cb, device}
  const users = {}; // email -> {uid, password}
  return {
    store, users,
    deliver(uid, coll, id, data, fromDevice) {
      for (const s of subs) {
        if (s.uid !== uid || s.coll !== coll || !s.device.online) continue;
        s.cb([{ id, data: JSON.parse(JSON.stringify(data)), type: 'upsert', pending: s.device === fromDevice }]);
      }
    },
    subs,
  };
}
function makeBackend(cloud, device) {
  let authCb = () => { };
  const queue = [];
  device.online = true;
  device.flush = async () => {
    while (queue.length) {
      const { uid, ops, resolve } = queue.shift();
      for (const o of ops) {
        cloud.store[uid] = cloud.store[uid] || {};
        cloud.store[uid][o.coll] = cloud.store[uid][o.coll] || {};
        cloud.store[uid][o.coll][o.id] = JSON.parse(JSON.stringify(o.data));
        cloud.deliver(uid, o.coll, o.id, o.data, device);
      }
      resolve();
    }
  };
  device.goOnline = async () => {
    device.online = true;
    // like Firestore: resend everything the server has that we missed
    for (const s of cloud.subs.filter(s => s.device === device)) {
      const docs = (cloud.store[s.uid] || {})[s.coll] || {};
      s.cb(Object.entries(docs).map(([id, data]) => ({ id, data: JSON.parse(JSON.stringify(data)), type: 'upsert', pending: false })));
    }
    await device.flush();
  };
  return {
    onAuth(cb) { authCb = cb; setTimeout(() => cb(null), 0); },
    async signUp(email, pw) { const u = { uid: 'u-' + email, email }; cloud.users[email] = { ...u, pw }; authCb(u); return u; },
    async signIn(email, pw) { const u = cloud.users[email]; if (!u || u.pw !== pw) throw new Error('wrong password'); authCb({ uid: u.uid, email }); return u; },
    async signOut() { authCb(null); },
    async resetPassword() { },
    subscribe(uid, coll, cb) {
      const s = { uid, coll, cb, device };
      cloud.subs.push(s);
      if (device.cacheFirst) setTimeout(() => cb([], { fromCache: true }), 0);
      setTimeout(() => {
        const docs = (cloud.store[uid] || {})[coll] || {};
        cb(Object.entries(docs).map(([id, data]) => ({ id, data: JSON.parse(JSON.stringify(data)), type: 'upsert', pending: false })));
      }, 1);
      return () => { const i = cloud.subs.indexOf(s); if (i >= 0) cloud.subs.splice(i, 1); };
    },
    write(uid, ops) {
      return new Promise(resolve => {
        queue.push({ uid, ops: JSON.parse(JSON.stringify(ops)), resolve });
        if (device.online) setTimeout(() => device.flush(), 1);
      });
    },
  };
}
function makeLocal(initial) {
  let saved = initial ? JSON.parse(JSON.stringify(initial)) : null;
  return { load: async () => (saved ? JSON.parse(JSON.stringify(saved)) : null), save: async d => { saved = JSON.parse(JSON.stringify(d)); return true; }, peek: () => saved };
}
async function device(cloud, name, initial) {
  const dev = { name };
  const localStore = makeLocal(initial);
  const api = createSync(localStore, makeBackend(cloud, dev));
  dev.api = api;
  dev.local = localStore;
  dev.data = (await api.load()) || { bookings: [], cash: [], lists: { agents: [] }, settings: {} };
  api.sync.onRemoteChange(d => { dev.data = d; });
  dev.statuses = [];
  api.sync.onStatus(s => dev.statuses.push(s.status));
  dev.save = () => api.save(dev.data);
  return dev;
}
const booking = (id, customer, sell) => ({ id, date: '2026-10-16', customer, sellingPrice: sell, netCost: sell - 50 });
const names = d => d.data.bookings.map(b => b.customer).sort();

// ---------- Tests ----------
const results = [];
async function test(name, fn) {
  try { await fn(); results.push(['PASS', name]); }
  catch (e) { results.push(['FAIL', name, e.message]); }
}

await test('PC with existing records signs in; empty phone receives everything', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300), booking('b2', 'SARA', 500)], cash: [{ id: 'c1', date: '2026-10-16', agent: 'X', amount: 400 }], lists: { agents: ['X'] }, settings: { company: 'BAKHTAR' } });
  await settle();
  await pc.api.sync.signUp('me@x.com', 'pw123456'); await settle();
  const phone = await device(cloud, 'phone');
  await settle();
  await phone.api.sync.signIn('me@x.com', 'pw123456'); await settle();
  assert.deepEqual(names(phone), ['ALI', 'SARA']);
  assert.equal(phone.data.cash.length, 1);
  assert.equal(phone.data.settings.company, 'BAKHTAR');
  assert.deepEqual(phone.data.lists.agents, ['X']);
  assert.deepEqual(phone.local.peek().bookings.length, 2, 'phone saved records locally too');
});

await test('New sale on phone appears on PC; edit on PC appears on phone', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc'); const phone = await device(cloud, 'phone'); await settle();
  await pc.api.sync.signUp('a@x.com', 'pw'); await phone.api.sync.signIn('a@x.com', 'pw'); await settle();
  phone.data.bookings.push(booking('n1', 'NEW CUSTOMER', 150)); await phone.save(); await settle();
  assert.deepEqual(names(pc), ['NEW CUSTOMER']);
  pc.data.bookings[0].sellingPrice = 175; await pc.save(); await settle();
  assert.equal(phone.data.bookings[0].sellingPrice, 175);
  assert.equal(pc.statuses.at(-1), 'synced');
});

await test('Delete on one device removes it on the other', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300), booking('b2', 'SARA', 500)], cash: [], lists: {}, settings: {} });
  const phone = await device(cloud, 'phone'); await settle();
  await pc.api.sync.signUp('d@x.com', 'pw'); await phone.api.sync.signIn('d@x.com', 'pw'); await settle();
  phone.data.bookings = phone.data.bookings.filter(b => b.id !== 'b1'); await phone.save(); await settle();
  assert.deepEqual(names(pc), ['SARA']);
});

await test('Offline phone: entries made offline upload when back online; deletes made elsewhere arrive', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300), booking('b2', 'SARA', 500)], cash: [], lists: {}, settings: {} });
  const phone = await device(cloud, 'phone'); await settle();
  await pc.api.sync.signUp('o@x.com', 'pw'); await phone.api.sync.signIn('o@x.com', 'pw'); await settle();
  phone.online = false;
  phone.data.bookings.push(booking('off1', 'OFFLINE SALE', 220)); await phone.save();
  pc.data.bookings = pc.data.bookings.filter(b => b.id !== 'b2'); await pc.save(); await settle();
  assert.deepEqual(names(pc), ['ALI'], 'pc does not see offline sale yet');
  await phone.goOnline(); await settle();
  assert.deepEqual(names(pc), ['ALI', 'OFFLINE SALE']);
  assert.deepEqual(names(phone), ['ALI', 'OFFLINE SALE']);
});

await test('A device that was signed out for a while catches up after reload (deletes included)', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300), booking('b2', 'SARA', 500)], cash: [], lists: {}, settings: {} });
  await settle(); await pc.api.sync.signUp('r@x.com', 'pw'); await settle();
  const laptop = await device(cloud, 'laptop'); await settle(); await laptop.api.sync.signIn('r@x.com', 'pw'); await settle();
  await laptop.api.sync.signOut(); await settle();
  pc.data.bookings = pc.data.bookings.filter(b => b.id !== 'b1');
  pc.data.bookings.push(booking('b3', 'OMAR', 900)); await pc.save(); await settle();
  // laptop restarts with its old local copy, signs in again
  const laptop2 = await device(cloud, 'laptop2', laptop.local.peek()); await settle();
  await laptop2.api.sync.signIn('r@x.com', 'pw'); await settle();
  assert.deepEqual(names(laptop2), ['OMAR', 'SARA']);
});

await test('Records made on two devices before ever syncing are merged, not lost', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('p1', 'PC SALE', 100)], cash: [], lists: {}, settings: {} });
  const phone = await device(cloud, 'phone', { bookings: [booking('m1', 'PHONE SALE', 200)], cash: [], lists: {}, settings: {} });
  await settle();
  await pc.api.sync.signUp('m@x.com', 'pw'); await settle();
  await phone.api.sync.signIn('m@x.com', 'pw'); await settle();
  assert.deepEqual(names(pc), ['PC SALE', 'PHONE SALE']);
  assert.deepEqual(names(phone), ['PC SALE', 'PHONE SALE']);
});

await test('Lists & settings sync (new agent added on phone)', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [], cash: [], lists: { agents: ['A'] }, settings: { company: 'X' } });
  const phone = await device(cloud, 'phone'); await settle();
  await pc.api.sync.signUp('l@x.com', 'pw'); await phone.api.sync.signIn('l@x.com', 'pw'); await settle();
  phone.data.lists.agents.push('NEW AGENT'); await phone.save(); await settle();
  assert.deepEqual(pc.data.lists.agents, ['A', 'NEW AGENT']);
});

await test('Saving with nothing changed sends nothing', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300)], cash: [], lists: {}, settings: {} });
  await settle(); await pc.api.sync.signUp('n@x.com', 'pw'); await settle();
  let writes = 0; const orig = cloud.deliver; cloud.deliver = (...a) => { writes++; return orig(...a); };
  await pc.save(); await pc.save(); await settle();
  assert.equal(writes, 0);
});

await test('Wrong password is reported and nothing syncs', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc'); await settle(); await pc.api.sync.signUp('w@x.com', 'right'); await settle();
  const phone = await device(cloud, 'phone'); await settle();
  await assert.rejects(() => phone.api.sync.signIn('w@x.com', 'wrong'));
  assert.equal(phone.api.sync.status.status, 'signed-out');
});

await test('Fresh phone that saved app defaults takes the account lists, not its defaults', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [], cash: [], lists: { agents: ['A', 'B'] }, settings: { company: 'REAL' } });
  await settle(); await pc.api.sync.signUp('f@x.com', 'pw'); await settle();
  const phone = await device(cloud, 'phone');
  phone.data = { bookings: [], cash: [], lists: { agents: ['DIRECT / WALK-IN'] }, settings: { company: 'DEFAULT' } };
  await phone.save(); await settle();
  await phone.api.sync.signIn('f@x.com', 'pw'); await settle();
  assert.deepEqual(phone.data.lists.agents, ['A', 'B']);
  assert.equal(phone.data.settings.company, 'REAL');
  assert.deepEqual(pc.data.lists.agents, ['A', 'B'], 'pc lists unchanged');
});

await test('Two brand-new devices signing in at the same moment both end up with the same records', async () => {
  const cloud = makeCloud();
  const a = await device(cloud, 'a'); const b = await device(cloud, 'b'); await settle();
  await a.api.sync.signUp('s@x.com', 'pw'); await b.api.sync.signIn('s@x.com', 'pw'); await settle();
  a.data.bookings.push(booking('x1', 'FROM A', 10)); await a.save();
  b.data.bookings.push(booking('x2', 'FROM B', 20)); await b.save(); await settle();
  assert.deepEqual(names(a), ['FROM A', 'FROM B']);
  assert.deepEqual(names(b), ['FROM A', 'FROM B']);
});

await test('Old copy loading from cache first does not bring back records deleted elsewhere', async () => {
  const cloud = makeCloud();
  const pc = await device(cloud, 'pc', { bookings: [booking('b1', 'ALI', 300), booking('b2', 'SARA', 500)], cash: [], lists: {}, settings: {} });
  await settle(); await pc.api.sync.signUp('c@x.com', 'pw'); await settle();
  const oldCopy = JSON.parse(JSON.stringify(pc.local.peek()));
  pc.data.bookings = pc.data.bookings.filter(b => b.id !== 'b1'); await pc.save(); await settle();
  const laptop = { name: 'laptop', cacheFirst: true };
  const store = makeLocal(oldCopy);
  laptop.api = createSync(store, makeBackend(cloud, laptop)); laptop.cacheFirst = true;
  laptop.data = await laptop.api.load(); laptop.api.sync.onRemoteChange(d => { laptop.data = d; });
  await settle(); await laptop.api.sync.signIn('c@x.com', 'pw'); await settle();
  assert.deepEqual(names(laptop), ['SARA']);
  assert.deepEqual(names(pc), ['SARA'], 'deleted record not resurrected on pc');
});

for (const r of results) console.log(r.join('  '));
const failed = results.filter(r => r[0] === 'FAIL').length;
console.log(`\n${results.length - failed}/${results.length} passed`);
process.exit(failed ? 1 : 0);
