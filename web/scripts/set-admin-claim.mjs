#!/usr/bin/env node
/**
 * Grants the `admin` custom claim to a Firebase Auth user so the Android tablet
 * can write Firestore status changes and menu edits.
 *
 *   cd web
 *   GOOGLE_APPLICATION_CREDENTIALS=./service-account.json npm run admin:claim -- owner@example.com
 */
import { applicationDefault, cert, initializeApp } from 'firebase-admin/app'
import { getAuth } from 'firebase-admin/auth'
import { existsSync } from 'node:fs'

const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT || process.env.GOOGLE_APPLICATION_CREDENTIALS
if (keyPath && !existsSync(keyPath)) {
  console.error(`Service account not found: ${keyPath}`)
  process.exit(1)
}

const email = process.argv[2]
if (!email) {
  console.error('Usage: npm run admin:claim -- owner@example.com')
  process.exit(1)
}

initializeApp(keyPath ? { credential: cert(keyPath) } : { credential: applicationDefault() })
const auth = getAuth()
const user = await auth.getUserByEmail(email)
await auth.setCustomUserClaims(user.uid, { ...(user.customClaims || {}), admin: true })
console.log(`Granted admin claim to ${email} (uid ${user.uid}). Sign out and back in on the tablet.`)
