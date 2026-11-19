#!/usr/bin/env node
/**
 * Seeds Firestore from the same data as Android SampleData.kt.
 *
 *   GOOGLE_APPLICATION_CREDENTIALS=./service-account.json npm run seed
 *
 * The service account needs Firestore Data Editor (it writes menus, toppings,
 * settings and the order-number counter). Never commit the key file.
 */
import { applicationDefault, cert, initializeApp } from 'firebase-admin/app'
import { getFirestore } from 'firebase-admin/firestore'
import { existsSync } from 'node:fs'
import { SEED_MENU, SEED_SETTINGS, SEED_TOPPINGS } from '../src/data/menu.js'

const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT || process.env.GOOGLE_APPLICATION_CREDENTIALS
if (keyPath && !existsSync(keyPath)) {
  console.error(`Service account not found: ${keyPath}`)
  process.exit(1)
}

initializeApp(keyPath ? { credential: cert(keyPath) } : { credential: applicationDefault() })
const db = getFirestore()

const batch = db.batch()
for (const item of SEED_MENU) {
  batch.set(db.collection('menus').doc(item.id), { ...item, updatedAt: new Date() })
}
for (const topping of SEED_TOPPINGS) {
  batch.set(db.collection('toppings').doc(topping.id), topping)
}
batch.set(db.collection('settings').doc('general'), { ...SEED_SETTINGS, updatedAt: new Date() })

const counter = db.collection('counters').doc('online_orders')
const snapshot = await counter.get()
if (!snapshot.exists) batch.set(counter, { next: 1 })

await batch.commit()
console.log(`Seeded ${SEED_MENU.length} menu items, ${SEED_TOPPINGS.length} toppings, settings and the order counter.`)
