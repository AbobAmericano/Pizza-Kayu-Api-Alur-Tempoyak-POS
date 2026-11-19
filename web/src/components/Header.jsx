import { Link, NavLink } from 'react-router-dom'

export default function Header({ cartCount, live, shopName }) {
  return (
    <header className="sticky top-0 z-30 border-b border-ember-100 bg-white/95 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-3 sm:px-6" style={{ paddingTop: 'max(0.75rem, env(safe-area-inset-top, 0px))' }}>
        <Link to="/" className="flex items-center gap-2 no-underline" aria-label={`${shopName} home`}>
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-ember-600 text-2xl" aria-hidden="true">🍕</span>
          <span className="leading-tight">
            <span className="block text-sm font-extrabold text-ember-700 sm:text-base">Pizza Kayu Api</span>
            <span className="block text-xs text-charcoal/60">Dungun · Alur Tempoyak</span>
          </span>
        </Link>
        <nav aria-label="Primary" className="flex items-center gap-1 sm:gap-3">
          <NavLink to="/" end className={({ isActive }) => `min-h-11 rounded-lg px-3 py-2 text-sm font-bold ${isActive ? 'bg-ember-100 text-ember-700' : 'text-charcoal/70 hover:bg-ember-50'}`}>Menu</NavLink>
          <NavLink to="/admin" className={({ isActive }) => `min-h-11 rounded-lg px-3 py-2 text-sm font-bold ${isActive ? 'bg-ember-100 text-ember-700' : 'text-charcoal/70 hover:bg-ember-50'}`}>Shop tools</NavLink>
          <Link to="/checkout" className="relative grid h-11 min-w-11 place-items-center rounded-xl bg-ember-600 px-3 text-white" aria-label={`Go to checkout, ${cartCount} items in cart`}>
            <span aria-hidden="true" className="text-xl">🛒</span>
            {cartCount > 0 && <span className="absolute -right-1 -top-1 grid h-6 min-w-6 place-items-center rounded-full bg-white px-1 text-xs font-extrabold text-ember-700 shadow">{cartCount}</span>}
          </Link>
        </nav>
      </div>
      <p className="sr-only" aria-live="polite">{live ? 'Live menu availability from the shop' : 'Using the sample menu'}</p>
    </header>
  )
}
