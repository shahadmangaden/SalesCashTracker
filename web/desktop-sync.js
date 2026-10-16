// Desktop (Electron) renderer: wraps the preload's window.api with online sync.
import firebaseConfig from './firebase-config.js';
import { createSync } from './sync-core.js';
import { createFirebaseBackend } from './firebase-backend.js';

window.apiReady = (async () => {
  if (!window.api) return undefined;
  if (!firebaseConfig) return createSync(window.api, null);
  try { return createSync(window.api, createFirebaseBackend(firebaseConfig)); }
  catch (e) { console.error('Online sync failed to start', e); return createSync(window.api, null); }
})();
