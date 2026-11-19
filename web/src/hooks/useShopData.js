import { useEffect, useState } from 'react'
import { collection, doc, onSnapshot } from 'firebase/firestore'
import { db, COLLECTIONS, isFirebaseConfigured } from '../firebase.js'
import { SEED_MENU, SEED_SETTINGS, SEED_TOPPINGS } from '../data/menu.js'

const asArray = (snapshot) => snapshot.docs.map((document) => ({ id: document.id, ...document.data() }))

/** Live menu, toppings and shop settings. Falls back to SampleData when Firestore is empty. */
export function useShopData() {
  const [data, setData] = useState({
    items: SEED_MENU,
    toppings: SEED_TOPPINGS,
    settings: SEED_SETTINGS,
    live: false,
    loading: isFirebaseConfigured,
    error: null,
  })

  useEffect(() => {
    if (!db) return undefined
    let current = { items: null, toppings: null, settings: null }
    let live = false
    const push = () => {
      setData({
        items: current.items?.length ? sortItems(current.items) : SEED_MENU,
        toppings: current.toppings?.length ? current.toppings : SEED_TOPPINGS,
        settings: { ...SEED_SETTINGS, ...(current.settings || {}) },
        live,
        loading: false,
        error: null,
      })
    }
    const unsubscribers = [
      onSnapshot(collection(db, COLLECTIONS.menus), (snapshot) => {
        current.items = asArray(snapshot)
        live = true
        push()
      }, (error) => setData((old) => ({ ...old, loading: false, error: error.message }))),
      onSnapshot(collection(db, COLLECTIONS.toppings), (snapshot) => {
        current.toppings = asArray(snapshot)
        push()
      }, () => {}),
      onSnapshot(doc(db, COLLECTIONS.settings, 'general'), (snapshot) => {
        current.settings = snapshot.data()
        push()
      }, () => {}),
    ]
    return () => unsubscribers.forEach((unsubscribe) => unsubscribe())
  }, [])

  return data
}

function sortItems(items) {
  return [...items].sort((a, b) => (a.sortIndex ?? 0) - (b.sortIndex ?? 0) || a.name.localeCompare(b.name))
}

/** Real-time order tracking: one Firestore onSnapshot listener per tracking page. */
export function useOrder(orderId) {
  const [state, setState] = useState({ order: null, loading: Boolean(db && orderId), error: null })

  useEffect(() => {
    if (!db || !orderId) {
      setState({ order: null, loading: false, error: db ? null : 'Firebase is not configured.' })
      return undefined
    }
    return onSnapshot(doc(db, COLLECTIONS.orders, orderId), (snapshot) => {
      if (!snapshot.exists()) {
        setState({ order: null, loading: false, error: 'We could not find that order.' })
        return
      }
      setState({ order: { id: snapshot.id, ...snapshot.data() }, loading: false, error: null })
    }, (error) => setState({ order: null, loading: false, error: error.message }))
  }, [orderId])

  return state
}
