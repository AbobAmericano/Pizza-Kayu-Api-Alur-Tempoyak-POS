import { useEffect } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useOrder } from '../hooks/useShopData.js'
import { money } from '../lib/money.js'
import { ACTIVE_STATUSES, statusLabel } from '../lib/order.js'
import { isFirebaseConfigured } from '../firebase.js'

export default function TrackOrder({ settings }) {
  const { orderId } = useParams()
  const { order, loading, error } = useOrder(orderId)

  useEffect(() => {
    document.title = order
      ? `Order ${order.label || orderId} | Pizza Kayu Api Dungun`
      : 'Track your order | Pizza Kayu Api Dungun'
  }, [order, orderId])

  if (loading) return <p className="mx-auto max-w-xl px-4 py-16 text-center" role="status">Loading your order…</p>
  if (error || !order) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center">
        <h1 className="text-3xl font-extrabold">We couldn't find that order</h1>
        <p className="mt-2 text-charcoal/70" role="alert">{error || 'Check the link from your confirmation and try again.'}</p>
        <Link to="/" className="mt-6 inline-block rounded-xl bg-ember-600 px-6 py-3 font-extrabold text-white">Back to the menu</Link>
      </div>
    )
  }

  const cancelled = order.status === 'CANCELLED'
  const completed = order.status === 'COMPLETED'
  const currentIndex = ACTIVE_STATUSES.indexOf(order.status)
  const timestamp = order.createdAt?.toDate?.() || (order.createdAt ? new Date(order.createdAt) : null)

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6">
      <p className="text-sm font-bold uppercase tracking-widest text-ember-600">Live order tracking</p>
      <h1 className="mt-1 text-3xl font-extrabold">Order {order.label || `#${order.orderNumber}`}</h1>
      <p className="mt-1 text-sm text-charcoal/60">
        {timestamp ? `Placed ${timestamp.toLocaleString('en-MY', { timeZone: 'Asia/Kuala_Lumpur' })}` : ''} · {order.customerName}
      </p>

      <section aria-labelledby="status-heading" className="mt-6 rounded-2xl border border-ember-100 bg-white p-5 shadow-sm sm:p-6">
        <h2 id="status-heading" className="text-xl font-extrabold">Status</h2>
        <p aria-live="polite" className={`mt-2 inline-block rounded-full px-4 py-2 text-base font-extrabold ${cancelled ? 'bg-red-100 text-red-900' : completed ? 'bg-emerald-100 text-emerald-900' : 'bg-ember-100 text-ember-700'}`}>
          {cancelled ? 'Cancelled' : statusLabel(order.status)}
        </p>
        {cancelled && order.cancelReason && <p className="mt-3 rounded-xl bg-red-50 p-3 text-sm text-red-900">{order.cancelReason}</p>}
        {!cancelled && order.status === 'PENDING' && <p className="mt-3 text-sm text-charcoal/70">Waiting for the shop to accept your order.</p>}
        {completed && <p className="mt-3 text-sm text-charcoal/70">Thank you! Your order is complete.</p>}

        {!cancelled && (
          <ol className="mt-6 space-y-3">
            {ACTIVE_STATUSES.map((status, index) => {
              const done = index < currentIndex || (completed && index === currentIndex)
              const active = index === currentIndex && !completed
              return (
                <li key={status} className="flex items-center gap-3">
                  <span aria-hidden="true" className={`grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-bold ${done ? 'bg-emerald-600 text-white' : active ? 'bg-ember-600 text-white' : 'bg-ember-100 text-charcoal/50'}`}>
                    {done ? '✓' : index + 1}
                  </span>
                  <span className={done || active ? 'font-bold' : 'text-charcoal/50'}>{statusLabel(status)}</span>
                  {active && <span className="sr-only">(current step)</span>}
                </li>
              )
            })}
          </ol>
        )}
        <p className="mt-5 text-sm text-charcoal/60">Pickup: {order.pickupTime || 'ASAP'} · Pay {money(order.totalSen, settings.currency || 'RM')} at the counter</p>
      </section>

      <section aria-labelledby="details-heading" className="mt-6 rounded-2xl border border-ember-100 bg-white p-5 shadow-sm sm:p-6">
        <h2 id="details-heading" className="text-xl font-extrabold">Your order</h2>
        <ul className="mt-3 divide-y divide-ember-100">
          {(order.items || []).map((line, index) => (
            <li key={line.lineId || index} className="flex gap-3 py-3 text-sm">
              <span aria-hidden="true">{line.emoji}</span>
              <div className="flex-1">
                <p className="font-bold">{line.qty} × {line.name}</p>
                <p className="text-charcoal/60">{[line.sizeLabel, ...(line.toppings || []).map((item) => `+${item}`), line.note && `“${line.note}”`].filter(Boolean).join(' · ')}</p>
              </div>
              <p className="font-bold">{money(line.unitPriceSen * line.qty, settings.currency || 'RM')}</p>
            </li>
          ))}
        </ul>
        <dl className="mt-3 space-y-1 border-t border-ember-200 pt-3 text-sm">
          <div className="flex justify-between"><dt>Subtotal</dt><dd>{money(order.subtotalSen, settings.currency || 'RM')}</dd></div>
          <div className="flex justify-between"><dt>Tax</dt><dd>{money(order.taxSen, settings.currency || 'RM')}</dd></div>
          <div className="flex justify-between text-lg font-extrabold"><dt>Total</dt><dd>{money(order.totalSen, settings.currency || 'RM')}</dd></div>
        </dl>
        {order.note && <p className="mt-4 rounded-xl bg-ember-50 p-3 text-sm">Note for the shop: {order.note}</p>}
      </section>

      <p className="mt-6 text-center text-sm text-charcoal/60">
        Questions? Call {settings.phone || 'the shop'}. <Link to="/" className="font-bold text-ember-600 underline">Order again</Link>
        {!isFirebaseConfigured && ' · Tracking needs Firebase configuration.'}
      </p>
    </div>
  )
}
