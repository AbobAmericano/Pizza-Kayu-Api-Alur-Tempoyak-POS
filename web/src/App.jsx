import { useEffect, useState } from 'react'
import { Link, Route, Routes, useLocation } from 'react-router-dom'
import Header from './components/Header.jsx'
import MenuBrowser from './components/MenuBrowser.jsx'
import ItemCustomizerDialog from './components/ItemCustomizerDialog.jsx'
import CartPanel, { CartContents } from './components/Cart.jsx'
import Checkout from './components/Checkout.jsx'
import TrackOrder from './components/TrackOrder.jsx'
import AdminSeed from './components/AdminSeed.jsx'
import { useShopData } from './hooks/useShopData.js'
import { buildCartLine } from './lib/order.js'
import { isFirebaseConfigured } from './firebase.js'
import { money } from './lib/money.js'
import { cartTotals } from './lib/order.js'

const CART_KEY = 'pizza-kayu-cart'

function loadCart() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(CART_KEY) || '[]')
    return Array.isArray(saved) ? saved : []
  } catch {
    return []
  }
}

export default function App() {
  const { items, toppings, settings, live, error } = useShopData()
  const [cart, setCart] = useState(loadCart)
  const [activeItem, setActiveItem] = useState(null)
  const [announcement, setAnnouncement] = useState('')
  const location = useLocation()

  useEffect(() => {
    sessionStorage.setItem(CART_KEY, JSON.stringify(cart))
  }, [cart])

  useEffect(() => {
    if (location.pathname !== '/') return
    document.title = 'Order Online | Pizza Kayu Api Dungun'
  }, [location.pathname])

  const addToCart = (item, choices) => {
    setCart((old) => [...old, buildCartLine(item, choices, toppings)])
    setAnnouncement(`${item.name} added to cart.`)
  }

  const cartCount = cart.reduce((sum, line) => sum + line.qty, 0)
  const cartTotalSen = cartTotals(cart, settings.taxBasisPoints).totalSen

  return (
    <>
      <a href="#menu-heading" className="skip-link visually-hidden">Skip to menu</a>
      <Header cartCount={cartCount} live={live} shopName={settings.shopName} />
      <div aria-live="polite" className="visually-hidden">{announcement}</div>

      {!isFirebaseConfigured && (
        <div role="status" className="mx-auto mt-4 max-w-7xl px-4 sm:px-6">
          <p className="rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900">
            Demo mode: add <code>web/.env.local</code> with your Firebase keys to place live orders and sync availability.
          </p>
        </div>
      )}
      {error && (
        <div role="alert" className="mx-auto mt-4 max-w-7xl px-4 sm:px-6">
          <p className="rounded-xl border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-900">
            Live menu unavailable ({error}). Showing the shop's standard menu for now.
          </p>
        </div>
      )}

      <main>
        <Routes>
          <Route
            path="/"
            element={(
              <div className="mx-auto grid w-full max-w-7xl gap-6 px-4 pb-32 pt-2 sm:px-6 lg:grid-cols-[minmax(0,1fr)_22rem] lg:pb-12">
                <MenuBrowser items={items} onSelect={setActiveItem} />
                <div className="hidden lg:block">
                  <CartPanel lines={cart} taxBasisPoints={settings.taxBasisPoints} onRemove={(id) => setCart((old) => old.filter((line) => line.lineId !== id))} onClear={() => setCart([])} />
                  <Link to="/checkout" className="mt-3 block rounded-xl bg-ember-600 px-5 py-3 text-center font-extrabold text-white hover:bg-ember-700">Go to checkout</Link>
                </div>
                <section id="mobile-cart" aria-labelledby="mobile-cart-heading" className="rounded-2xl border border-ember-100 bg-white p-4 lg:hidden">
                  <h2 id="mobile-cart-heading" className="text-xl font-extrabold">Your order</h2>
                  <CartContents lines={cart} taxBasisPoints={settings.taxBasisPoints} onRemove={(id) => setCart((old) => old.filter((line) => line.lineId !== id))} onClear={() => setCart([])} />
                </section>
              </div>
            )}
          />
          <Route
            path="/checkout"
            element={(
              <Checkout
                lines={cart}
                settings={settings}
                onClearCart={() => setCart([])}
                onRemove={(id) => setCart((old) => old.filter((line) => line.lineId !== id))}
                onClear={() => setCart([])}
              />
            )}
          />
          <Route path="/order/:orderId" element={<TrackOrder settings={settings} />} />
          <Route path="/admin" element={<AdminSeed />} />
          <Route path="*" element={<p className="mx-auto max-w-xl px-4 py-16 text-center">Page not found. <Link className="font-bold text-ember-600 underline" to="/">Back to the menu</Link></p>} />
        </Routes>
      </main>

      {location.pathname === '/' && cartCount > 0 && (
        <div className="bottom-bar fixed inset-x-0 bottom-0 z-30 border-t border-ember-100 bg-white/95 shadow-[0_-8px_24px_rgba(36,24,18,0.08)] backdrop-blur lg:hidden">
          <div className="mx-auto flex max-w-7xl items-center justify-between gap-3">
            <div>
              <p className="text-xs text-charcoal/60">{cartCount} {cartCount === 1 ? 'item' : 'items'}</p>
              <p className="text-lg font-extrabold">Total {money(cartTotalSen)}</p>
            </div>
            <Link to="/checkout" className="min-h-12 rounded-xl bg-ember-600 px-6 py-3 font-extrabold text-white">Checkout</Link>
          </div>
        </div>
      )}

      <footer className="border-t border-ember-100 bg-white py-8 text-center text-sm text-charcoal/60">
        <p>{settings.shopName} · {settings.location}</p>
        <p className="mt-1">Pay at pickup · {settings.phone ? `Call ${settings.phone}` : ''}</p>
      </footer>

      {activeItem && (
        <ItemCustomizerDialog
          key={activeItem.id}
          item={activeItem}
          toppings={toppings}
          onAdd={addToCart}
          onClose={() => setActiveItem(null)}
        />
      )}
    </>
  )
}
