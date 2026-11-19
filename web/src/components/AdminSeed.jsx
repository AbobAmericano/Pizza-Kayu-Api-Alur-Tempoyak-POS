import { useState } from 'react'
import { getIdTokenResult } from 'firebase/auth'
import { Link } from 'react-router-dom'
import { adminSignIn, adminSignOut, auth, isFirebaseConfigured, seedFirestoreFromSample } from '../firebase.js'

/** Optional seed panel. The CLI (`npm run seed`) is the primary, service-account route. */
export default function AdminSeed() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [isAdmin, setIsAdmin] = useState(false)
  const [busy, setBusy] = useState(false)

  const signIn = async (event) => {
    event.preventDefault()
    setBusy(true)
    setMessage('')
    try {
      await adminSignIn(email, password)
      const claims = await getIdTokenResult(auth.currentUser)
      const admin = Boolean(claims.claims.admin)
      setIsAdmin(admin)
      setMessage(admin ? 'Signed in as an admin.' : 'This account does not have the admin custom claim. Ask the owner to set it with the Admin SDK.')
    } catch (error) {
      setMessage(`Sign-in failed: ${error.message}`)
    } finally {
      setBusy(false)
    }
  }

  const seed = async () => {
    setBusy(true)
    setMessage('')
    try {
      await seedFirestoreFromSample()
      setMessage('Seeded menus, toppings, settings and the order counter from SampleData.kt.')
    } catch (error) {
      setMessage(`Seed failed: ${error.message}. Check the admin claim and firestore.rules.`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6">
      <p className="text-sm font-bold uppercase tracking-widest text-ember-600">Shop tools</p>
      <h1 className="mt-1 text-3xl font-extrabold">Seed Firestore menu</h1>
      <p className="mt-2 text-sm text-charcoal/70">
        Copy the Android app's starter menu into Firestore. Prefer <code>npm run seed</code> with a service account;
        this panel is for an admin account with the <code>admin: true</code> custom claim.
      </p>

      {!isFirebaseConfigured && <p role="alert" className="mt-4 rounded-xl bg-red-50 p-3 text-sm text-red-900">Add your Firebase keys to web/.env.local first.</p>}

      <form onSubmit={signIn} className="mt-6 space-y-4 rounded-2xl border border-ember-100 bg-white p-5 shadow-sm">
        <div className="field">
          <label htmlFor="adminEmail">Admin email</label>
          <input id="adminEmail" type="email" autoComplete="username" required value={email} onChange={(event) => setEmail(event.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="adminPassword">Password</label>
          <input id="adminPassword" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
        </div>
        <button type="submit" disabled={busy || !isFirebaseConfigured} className="min-h-12 w-full rounded-xl bg-ember-600 px-5 py-3 font-extrabold text-white disabled:opacity-60">Sign in</button>
      </form>

      {isAdmin && (
        <div className="mt-4 rounded-2xl border border-ember-200 bg-white p-5 shadow-sm">
          <button type="button" onClick={seed} disabled={busy} className="min-h-12 w-full rounded-xl border-2 border-ember-600 px-5 py-3 font-extrabold text-ember-700 hover:bg-ember-50 disabled:opacity-60">Seed menu from SampleData.kt</button>
          <button type="button" onClick={async () => { await adminSignOut(); setIsAdmin(false) }} className="mt-3 min-h-11 w-full text-sm font-semibold text-charcoal/60 underline">Sign out</button>
        </div>
      )}

      {message && <p role="status" className="mt-4 rounded-xl bg-ember-100 p-3 text-sm font-semibold text-ember-900">{message}</p>}
      <p className="mt-6 text-sm"><Link to="/" className="font-bold text-ember-600 underline">Back to the menu</Link></p>
    </div>
  )
}
