// Builds the browser version of the app into web-dist/ (deployed to GitHub Pages).
// Shares src/ (UI) and lib/excel-core.js (Excel import/export) with the desktop app.
const fs = require('fs');
const path = require('path');
const esbuild = require('esbuild');

const root = path.join(__dirname, '..');
const out = path.join(root, 'web-dist');
const pkg = require('../package.json');
const version = process.env.APP_VERSION || pkg.version;

fs.rmSync(out, { recursive: true, force: true });
fs.mkdirSync(out, { recursive: true });

// 1. Bundle the browser bridge (IndexedDB storage + Excel in the browser)
(async () => {
await esbuild.build({
  entryPoints: [path.join(root, 'web', 'bridge.js')],
  bundle: true, minify: true, format: 'esm', splitting: true, platform: 'browser', target: ['es2020'],
  entryNames: '[name]', chunkNames: 'chunks/[name]-[hash]',
  define: { __APP_VERSION__: JSON.stringify(version), global: 'globalThis' },
  outdir: out,
  // Test builds swap Firebase for a local stand-in (see test/mock-backend.js)
  plugins: process.env.SYNC_MOCK ? [{
    name: 'mock-sync',
    setup(b) {
      b.onResolve({ filter: /firebase-backend\.js$/ }, () => ({ path: path.join(root, 'test', 'mock-backend.js') }));
      b.onResolve({ filter: /firebase-config\.js$/ }, () => ({ path: 'mock-config', namespace: 'mock' }));
      b.onLoad({ filter: /.*/, namespace: 'mock' }, () => ({ contents: 'export default { mock: true };', loader: 'js' }));
    },
  }] : [],
  logLevel: 'warning',
});

// 2. Copy UI files
for (const f of ['app.js', 'styles.css']) fs.copyFileSync(path.join(root, 'src', f), path.join(out, f));

// 3. index.html: load the bridge first, add PWA manifest + service worker
let html = fs.readFileSync(path.join(root, 'src', 'index.html'), 'utf8');
html = html.replace(/<meta http-equiv="Content-Security-Policy"[^>]*>\s*/, '');
html = html.replace(/\s*<script src="\.\.\/app-dist\/desktop-sync\.js"><\/script>/, '');
html = html.replace('</title>', `</title>
  <meta name="theme-color" content="#13294b">
  <meta name="description" content="Sales & cash paid tracker for travel sales">
  <link rel="manifest" href="manifest.webmanifest">
  <link rel="icon" href="icon-192.png">
  <link rel="apple-touch-icon" href="icon-192.png">
  <meta name="apple-mobile-web-app-capable" content="yes">
  <meta name="mobile-web-app-capable" content="yes">`);
html = html.replace('<script src="app.js"></script>', `<script type="module" src="bridge.js?v=${version}"></script>
  <script type="module" src="app.js?v=${version}"></script>
  <script>
    if ('serviceWorker' in navigator && location.protocol === 'https:') navigator.serviceWorker.register('sw.js');
  </script>`);
html = html.replace('href="styles.css"', `href="styles.css?v=${version}"`);
fs.writeFileSync(path.join(out, 'index.html'), html);

// 4. Icons (copied from desktop build resources)
fs.copyFileSync(path.join(root, 'web', 'icon-192.png'), path.join(out, 'icon-192.png'));
fs.copyFileSync(path.join(root, 'web', 'icon-512.png'), path.join(out, 'icon-512.png'));

// 5. Manifest
fs.writeFileSync(path.join(out, 'manifest.webmanifest'), JSON.stringify({
  name: 'Sales Cash Tracker',
  short_name: 'Sales Tracker',
  description: 'Sales & cash paid tracker',
  start_url: './',
  scope: './',
  display: 'standalone',
  background_color: '#f4f6fa',
  theme_color: '#13294b',
  icons: [
    { src: 'icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
  ],
}, null, 2));

// 6. Service worker: works offline, picks up new versions on next open
const assets = ['./', 'index.html', `app.js?v=${version}`, `bridge.js?v=${version}`, `styles.css?v=${version}`, 'manifest.webmanifest', 'icon-192.png', 'icon-512.png'];
fs.writeFileSync(path.join(out, 'sw.js'), `const CACHE = 'sct-${version}';
const ASSETS = ${JSON.stringify(assets)};
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting())); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())); });
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  // Network first for the page so updates arrive; cache fallback when offline
  if (e.request.mode === 'navigate') {
    e.respondWith(fetch(e.request).then(r => { const copy = r.clone(); caches.open(CACHE).then(c => c.put('index.html', copy)); return r; }).catch(() => caches.match('index.html')));
    return;
  }
  e.respondWith(caches.match(e.request).then(hit => hit || fetch(e.request).then(r => {
    if (r.ok && new URL(e.request.url).origin === location.origin) { const copy = r.clone(); caches.open(CACHE).then(c => c.put(e.request, copy)); }
    return r;
  })));
});
`);
fs.writeFileSync(path.join(out, '.nojekyll'), '');
console.log('Web app built into web-dist/ (version ' + version + ')');
for (const f of fs.readdirSync(out).filter(f => f !== 'chunks')) console.log('  ' + f + '  ' + (fs.statSync(path.join(out, f)).size / 1024).toFixed(0) + ' KB');
})().catch(e => { console.error(e); process.exit(1); });
