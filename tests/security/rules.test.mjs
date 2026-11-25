import { readFile } from 'node:fs/promises';
import { after, before, beforeEach, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, serverTimestamp } from 'firebase/firestore';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

let env;
before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-leaflens-security',
    firestore: { host: '127.0.0.1', port: 8088, rules: await readFile('../../firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9198, rules: await readFile('../../storage.rules', 'utf8') }
  });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); await env.clearStorage(); });
const context = (uid = 'alice') => env.authenticatedContext(uid, { auth_time: Math.floor(Date.now() / 1000) });
const usage = (db) => doc(db, 'users/alice/usage/2026-11-22');

test('unauthenticated and other users cannot read or modify private plants', async () => {
  const owner = context().firestore();
  await assertSucceeds(setDoc(doc(owner, 'users/alice/plants/rose'), { name: 'Rose' }));
  await assertFails(getDoc(doc(context('bob').firestore(), 'users/alice/plants/rose')));
  await assertFails(setDoc(doc(env.unauthenticatedContext().firestore(), 'users/alice/plants/rose'), { name: 'X' }));
});
test('trial start cannot be changed and clients cannot grant premium or roles', async () => {
  const db = context().firestore();
  const user = doc(db, 'users/alice');
  await assertSucceeds(setDoc(user, { trialStartTime: Date.now() - 60_000, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(user, { trialStartTime: Date.now(), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(user, { isPremium: true }));
  await assertFails(updateDoc(user, { role: 'admin' }));
});
test('quota counter only increments and stops at three', async () => {
  const counter = usage(context().firestore());
  await assertSucceeds(setDoc(counter, { scanCount: 1, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(counter, { scanCount: 0, updatedAt: serverTimestamp() }));
  for (const scanCount of [2, 3]) await assertSucceeds(updateDoc(counter, { scanCount, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(counter, { scanCount: 4, updatedAt: serverTimestamp() }));
});
test('quota cannot skip increments, forge timestamps, or add fields', async () => {
  const counter = usage(context().firestore());
  await assertFails(setDoc(counter, { scanCount: 3, updatedAt: serverTimestamp() }));
  await assertFails(setDoc(counter, { scanCount: 1, updatedAt: new Date(0) }));
  await assertFails(setDoc(counter, { scanCount: 1, updatedAt: serverTimestamp(), isPremium: true }));
});
test('usage cleanup requires recent sign in', async () => {
  const counter = usage(context().firestore());
  await setDoc(counter, { scanCount: 1, updatedAt: serverTimestamp() });
  const stale = env.authenticatedContext('alice', { auth_time: Math.floor(Date.now() / 1000) - 3600 });
  await assertFails(deleteDoc(usage(stale.firestore())));
  await assertSucceeds(deleteDoc(counter));
});
test('storage accepts owner images and denies cross-user reads and writes', async () => {
  const image = ref(context().storage(), 'users/alice/scans/rose.jpg');
  await assertSucceeds(uploadBytes(image, new Uint8Array([1,2,3]), { contentType: 'image/jpeg' }));
  await assertFails(getBytes(ref(context('bob').storage(), image.fullPath)));
  await assertFails(uploadBytes(ref(context('bob').storage(), image.fullPath), new Uint8Array([1]), { contentType: 'image/jpeg' }));
  await assertSucceeds(deleteObject(image));
});
test('storage denies oversized images and non-images', async () => {
  const image = ref(context().storage(), 'users/alice/scan.jpg');
  await assertFails(uploadBytes(image, new Uint8Array(5 * 1024 * 1024 + 1), { contentType: 'image/jpeg' }));
  await assertFails(uploadBytes(image, new Uint8Array([1]), { contentType: 'text/html' }));
});
