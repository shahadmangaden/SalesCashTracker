// Firebase (Auth + Firestore) adapter for sync-core.js.
// Data layout: users/{uid}/bookings/{id}, users/{uid}/cash/{id}, users/{uid}/meta/config
import { initializeApp } from 'firebase/app';
import {
  getAuth, onAuthStateChanged, signInWithEmailAndPassword, createUserWithEmailAndPassword,
  signOut, sendPasswordResetEmail,
} from 'firebase/auth';
import {
  initializeFirestore, persistentLocalCache, persistentMultipleTabManager,
  collection, doc, onSnapshot, writeBatch,
} from 'firebase/firestore';

export function createFirebaseBackend(config) {
  const app = initializeApp(config);
  const auth = getAuth(app);
  let db;
  try {
    db = initializeFirestore(app, {
      ignoreUndefinedProperties: true,
      localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }),
    });
  } catch (e) {
    // e.g. private browsing without IndexedDB: fall back to memory cache
    db = initializeFirestore(app, { ignoreUndefinedProperties: true });
  }

  return {
    onAuth(cb) { onAuthStateChanged(auth, u => cb(u ? { uid: u.uid, email: u.email } : null)); },
    signIn: (email, pw) => signInWithEmailAndPassword(auth, email, pw),
    signUp: (email, pw) => createUserWithEmailAndPassword(auth, email, pw),
    signOut: () => signOut(auth),
    resetPassword: email => sendPasswordResetEmail(auth, email),
    subscribe(uid, coll, cb, onError) {
      return onSnapshot(collection(db, 'users', uid, coll), { includeMetadataChanges: true }, snap => {
        const changes = snap.docChanges().map(c => ({
          id: c.doc.id,
          data: c.doc.data(),
          type: c.type === 'removed' ? 'remove' : 'upsert',
          pending: c.doc.metadata.hasPendingWrites,
        }));
        cb(changes, { fromCache: snap.metadata.fromCache });
      }, onError);
    },
    async write(uid, ops) {
      // Firestore batches hold up to 500 writes
      for (let i = 0; i < ops.length; i += 400) {
        const batch = writeBatch(db);
        for (const o of ops.slice(i, i + 400)) batch.set(doc(db, 'users', uid, o.coll, o.id), o.data);
        await batch.commit();
      }
    },
  };
}
