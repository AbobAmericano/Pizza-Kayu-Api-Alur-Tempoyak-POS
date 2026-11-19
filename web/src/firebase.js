import { initializeApp } from 'firebase/app'
import {
  getAuth,
  signInAnonymously,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth'
import {
  addDoc,
  collection,
  doc,
  getDoc,
  getFirestore,
  runTransaction,
  serverTimestamp,
  writeBatch,
} from 'firebase/firestore'
import { SEED_MENU, SEED_SETTINGS, SEED_TOPPINGS } from './data/menu.js'

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

export const isFirebaseConfigured = Boolean(
  firebaseConfig.apiKey && firebaseConfig.projectId && firebaseConfig.appId,
)

/** With no .env.local the menu still renders from SampleData; ordering is disabled. */
export const app = isFirebaseConfigured ? initializeApp(firebaseConfig) : null
export const auth = app ? getAuth(app) : null
export const db = app ? getFirestore(app) : null

export const COLLECTIONS = {
  menus: 'menus',
  toppings: 'toppings',
  settings: 'settings',
  orders: 'online_orders',
  counter: 'counters',
}

/** Customers get an anonymous Auth UID so Firestore rules can give them their own order. */
export async function ensureCustomerAuth() {
  if (!auth) throw new Error('Firebase is not configured. Copy web/.env.example to web/.env.local.')
  if (auth.currentUser) return auth.currentUser
  return (await signInAnonymously(auth)).user
}

async function nextOrderNumber() {
  const counter = doc(db, COLLECTIONS.counter, 'online_orders')
  return runTransaction(db, async (transaction) => {
    const snapshot = await transaction.get(counter)
    const next = snapshot.exists() ? Number(snapshot.data().next || 0) + 1 : 1
    if (snapshot.exists()) transaction.update(counter, { next })
    else transaction.set(counter, { next })
    return next
  })
}

/**
 * Writes one `online_orders` document in the exact shape the Android FirebaseSyncManager reads.
 * All money is whole sen; all money fields are integers, never floats.
 */
export async function submitCustomerOrder({ lines, customer, totals, settings }) {
  const user = await ensureCustomerAuth()
  const orderNumber = await nextOrderNumber()
  const payload = {
    orderNumber,
    label: `W${orderNumber}`,
    status: 'PENDING',
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    orderType: customer.orderType,
    customerUid: user.uid,
    customerName: customer.customerName.trim(),
    customerPhone: customer.customerPhone.trim(),
    note: (customer.note || '').trim(),
    pickupTime: customer.pickupTime || 'ASAP',
    pickupAt: null,
    subtotalSen: Math.trunc(totals.subtotalSen),
    taxSen: Math.trunc(totals.taxSen),
    totalSen: Math.trunc(totals.totalSen),
    taxBasisPoints: Math.trunc(settings.taxBasisPoints ?? 800),
    payment: '',
    cancelReason: '',
    statusHistory: [{ status: 'PENDING', by: 'customer' }],
    items: lines.map((line) => ({
      lineId: line.lineId,
      itemId: line.itemId,
      name: line.name,
      emoji: line.emoji,
      unitPriceSen: Math.trunc(line.unitPriceSen),
      qty: Math.trunc(line.qty),
      sizeLabel: line.sizeLabel || null,
      crustLabel: null,
      toppings: line.toppings || [],
      note: line.note || '',
    })),
  }
  const reference = await addDoc(collection(db, COLLECTIONS.orders), payload)
  return reference.id
}

/** Seed admin panel: needs a Firebase Auth user with the `admin: true` custom claim. */
export async function adminSignIn(email, password) {
  if (!auth) throw new Error('Firebase is not configured.')
  return signInWithEmailAndPassword(auth, email, password)
}

export async function adminSignOut() {
  if (auth) await signOut(auth)
}

export async function seedFirestoreFromSample() {
  if (!db) throw new Error('Firebase is not configured.')
  const batch = writeBatch(db)
  SEED_MENU.forEach((item) => {
    batch.set(doc(db, COLLECTIONS.menus, item.id), { ...item, updatedAt: serverTimestamp() })
  })
  SEED_TOPPINGS.forEach((topping) => {
    batch.set(doc(db, COLLECTIONS.toppings, topping.id), topping)
  })
  batch.set(doc(db, COLLECTIONS.settings, 'general'), { ...SEED_SETTINGS, updatedAt: serverTimestamp() })
  const counter = doc(db, COLLECTIONS.counter, 'online_orders')
  const snapshot = await getDoc(counter)
  if (!snapshot.exists()) batch.set(counter, { next: 1 })
  await batch.commit()
}
