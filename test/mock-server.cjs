// Static server for web-dist plus a tiny in-memory "cloud" for sync tests.
const http = require('http'), fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..', 'web-dist');
const users = {}; const docs = []; // {uid, coll, id, data, v}
let v = 0;
const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.png': 'image/png', '.webmanifest': 'application/manifest+json' };
http.createServer((req, res) => {
  const u = new URL(req.url, 'http://x');
  const json = (code, o) => { res.writeHead(code, { 'content-type': 'application/json' }); res.end(JSON.stringify(o)); };
  if (u.pathname.startsWith('/mock/')) {
    let body = ''; req.on('data', c => body += c); req.on('end', () => {
      const b = body ? JSON.parse(body) : {};
      const p = u.pathname.slice(6);
      if (p === 'signup') { if (users[b.email]) return json(400, { error: 'auth/email-already-in-use' }); if ((b.password || '').length < 6) return json(400, { error: 'auth/weak-password' }); users[b.email] = { uid: 'u' + Object.keys(users).length, email: b.email, pw: b.password }; return json(200, { uid: users[b.email].uid, email: b.email }); }
      if (p === 'signin') { const x = users[b.email]; if (!x || x.pw !== b.password) return json(400, { error: 'auth/invalid-credential' }); return json(200, { uid: x.uid, email: x.email }); }
      if (p === 'write') { for (const o of b.ops) { v++; const i = docs.findIndex(d => d.uid === b.uid && d.coll === o.coll && d.id === o.id); const d = { uid: b.uid, coll: o.coll, id: o.id, data: o.data, v }; if (i >= 0) docs[i] = d; else docs.push(d); } return json(200, { ok: true }); }
      if (p === 'docs') { const since = +u.searchParams.get('since'); const list = docs.filter(d => d.uid === u.searchParams.get('uid') && d.coll === u.searchParams.get('coll') && d.v > since); return json(200, { version: v, changes: list.map(d => ({ id: d.id, data: d.data })) }); }
      if (p === 'dump') return json(200, { users: Object.keys(users), docs: docs.length });
      json(404, {});
    });
    return;
  }
  let f = path.join(root, decodeURIComponent(u.pathname));
  if (f.endsWith('/')) f += 'index.html';
  fs.readFile(f, (err, data) => { if (err) { res.writeHead(404); return res.end(); } res.writeHead(200, { 'content-type': types[path.extname(f)] || 'application/octet-stream' }); res.end(data); });
}).listen(+process.argv[2] || 8790, () => console.log('mock server on', +process.argv[2] || 8790));
