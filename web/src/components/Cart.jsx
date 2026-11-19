import { money } from '../lib/money.js'
import { cartTotals, lineTotalSen } from '../lib/order.js'

/** Shared between the sticky desktop panel and the mobile cart section. */
export function CartContents({ lines, taxBasisPoints, onRemove, onClear }) {
  const totals = cartTotals(lines, taxBasisPoints)
  if (lines.length === 0) {
    return <p className="py-8 text-center text-sm text-charcoal/60">Your cart is empty. Add something delicious to get started.</p>
  }

  return (
    <>
      <ul className="divide-y divide-ember-100">
        {lines.map((line) => (
          <li key={line.lineId} className="flex gap-3 py-3">
            <span className="text-2xl" aria-hidden="true">{line.emoji}</span>
            <div className="min-w-0 flex-1">
              <p className="font-bold leading-tight">{line.name}</p>
              <p className="text-xs text-charcoal/65">
                {[line.sizeLabel, ...line.toppings.map((topping) => `+${topping}`), line.note && `“${line.note}”`].filter(Boolean).join(' · ') || 'Regular'}
              </p>
              <p className="text-xs text-charcoal/60">Qty {line.qty} × {money(line.unitPriceSen)}</p>
            </div>
            <p className="whitespace-nowrap font-bold">{money(lineTotalSen(line))}</p>
            <button
              type="button"
              onClick={() => onRemove(line.lineId)}
              aria-label={`Remove ${line.name} from cart`}
              className="grid h-8 w-8 shrink-0 place-items-center rounded-full text-charcoal/60 hover:bg-ember-100 hover:text-ember-700"
            >
              ×
            </button>
          </li>
        ))}
      </ul>
      <dl className="mt-3 space-y-1 border-t border-ember-200 pt-3 text-sm">
        <div className="flex justify-between"><dt>Subtotal</dt><dd>{money(totals.subtotalSen)}</dd></div>
        <div className="flex justify-between"><dt>Tax ({(Number(taxBasisPoints) || 0) / 100}%)</dt><dd>{money(totals.taxSen)}</dd></div>
        <div className="flex justify-between border-t border-ember-200 pt-2 text-lg font-extrabold"><dt>Total</dt><dd>{money(totals.totalSen)}</dd></div>
      </dl>
      <button type="button" onClick={onClear} className="mt-3 min-h-11 text-sm font-semibold text-ember-700 underline">Clear cart</button>
    </>
  )
}

export default function CartPanel({ lines, taxBasisPoints, onRemove, onClear }) {
  return (
    <aside aria-labelledby="cart-heading" className="rounded-2xl border border-ember-100 bg-white p-4 shadow-sm lg:sticky lg:top-4">
      <h2 id="cart-heading" className="text-xl font-extrabold">Your order</h2>
      <CartContents lines={lines} taxBasisPoints={taxBasisPoints} onRemove={onRemove} onClear={onClear} />
    </aside>
  )
}
