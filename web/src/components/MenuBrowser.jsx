import { useMemo, useState } from 'react'
import { CATEGORIES, imageUrl } from '../data/menu.js'
import { money } from '../lib/money.js'

export default function MenuBrowser({ items, onSelect }) {
  const [category, setCategory] = useState('ALL')
  const visible = useMemo(
    () => items.filter((item) => category === 'ALL' || item.category === category),
    [items, category],
  )

  return (
    <section aria-labelledby="menu-heading" className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <p className="text-sm font-bold uppercase tracking-widest text-ember-600">Fresh from the wood fire</p>
          <h1 id="menu-heading" tabIndex={-1} className="mt-1 text-3xl font-extrabold sm:text-4xl">Our menu</h1>
        </div>
        <p className="max-w-md text-sm text-charcoal/70">
          Choose your size and toppings, then pay when you collect. Live availability comes straight from the shop's POS.
        </p>
      </div>

      <nav aria-label="Menu categories" className="mb-6 flex flex-wrap gap-2">
        <CategoryButton label="🍽️ All" active={category === 'ALL'} onClick={() => setCategory('ALL')} />
        {CATEGORIES.map((entry) => (
          <CategoryButton
            key={entry.id}
            label={`${entry.emoji} ${entry.label}`}
            active={category === entry.id}
            onClick={() => setCategory(entry.id)}
          />
        ))}
      </nav>

      <div className="menu-grid">
        {visible.map((item) => {
          const soldOut = !item.available
          return (
            <article key={item.id} className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-ember-100 bg-white shadow-sm">
              <div className="relative aspect-[4/3] bg-ember-100">
                {item.imageKey ? (
                  <img src={imageUrl(item)} alt={`${item.name} pizza`} loading="lazy" width="640" height="480" className="h-full w-full object-cover" />
                ) : (
                  <div className="grid h-full place-items-center text-6xl" aria-hidden="true">{item.emoji}</div>
                )}
                {soldOut && (
                  <p className="absolute inset-0 grid place-items-center bg-charcoal/65 text-xl font-extrabold tracking-wide text-white">SOLD OUT</p>
                )}
              </div>
              <div className="flex flex-1 flex-col gap-2 p-4">
                <div className="flex items-start justify-between gap-2">
                  <h2 className="text-lg font-extrabold leading-tight">{item.name}</h2>
                  <p className="whitespace-nowrap text-base font-extrabold text-ember-600">
                    {item.isPizza ? `from ${money(item.priceSen)}` : money(item.priceSen)}
                  </p>
                </div>
                <p className="flex-1 text-sm text-charcoal/70">{item.description}</p>
                <button
                  type="button"
                  disabled={soldOut}
                  onClick={() => onSelect(item)}
                  className="min-h-11 w-full rounded-xl border-2 border-ember-500 px-4 py-2 font-bold text-ember-600 hover:bg-ember-50 disabled:cursor-not-allowed disabled:border-charcoal/20 disabled:text-charcoal/40"
                >
                  {soldOut ? 'Sold out' : item.isPizza ? 'Customise & add' : 'Add to cart'}
                </button>
              </div>
            </article>
          )
        })}
      </div>
      {visible.length === 0 && <p className="mt-8 text-center text-charcoal/60">No items in this category right now.</p>}
    </section>
  )
}

function CategoryButton({ label, active, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`min-h-11 rounded-full border-2 px-4 py-2 text-sm font-bold transition-colors ${active ? 'border-ember-600 bg-ember-600 text-white' : 'border-ember-200 bg-white text-charcoal hover:border-ember-400'}`}
    >
      {label}
    </button>
  )
}
