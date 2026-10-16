// Browser stand-in for firebase-backend.js used only in tests (talks to test/mock-server.js).
export function createFirebaseBackend() {
  let authCb = () => { };
  const KEY = 'mock-user';
  const user = () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch (_) { return null; } };
  const post = (path, body) => fetch('/mock/' + path, { method: 'POST', body: JSON.stringify(body) }).then(async r => {
    const j = await r.json(); if (!r.ok) { const e = new Error(j.error); e.code = j.error; throw e; } return j;
  });
  const setUser = u => { if (u) localStorage.setItem(KEY, JSON.stringify(u)); else localStorage.removeItem(KEY); authCb(u); return u; };
  return {
    onAuth(cb) { authCb = cb; setTimeout(() => cb(user()), 0); },
    signUp: (email, password) => post('signup', { email, password }).then(setUser),
    signIn: (email, password) => post('signin', { email, password }).then(setUser),
    signOut: async () => setUser(null),
    resetPassword: async () => { },
    subscribe(uid, coll, cb) {
      let version = -1, stopped = false;
      const poll = async () => {
        if (stopped) return;
        try {
          const r = await fetch(`/mock/docs?uid=${encodeURIComponent(uid)}&coll=${coll}&since=${version}`).then(r => r.json());
          if (version === -1 || r.changes.length) cb(r.changes.map(c => ({ ...c, type: 'upsert', pending: false })), { fromCache: false });
          version = r.version;
        } catch (_) { }
        setTimeout(poll, 300);
      };
      poll();
      return () => { stopped = true; };
    },
    write: (uid, ops) => post('write', { uid, ops }),
  };
}
