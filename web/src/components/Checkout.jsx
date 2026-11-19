import { useMemo, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { CartContents } from './Cart.jsx'
import { money } from '../lib/money.js'
import { cartTotals, isMalaysianMobile, pickupSlots } from '../lib/order.js'
import { isFirebaseConfigured, submitCustomerOrder } from '../firebase.js'

const INITIAL = { customerName: '', customerPhone: '', orderType: 'TAKEAWAY', pickupTime: 'ASAP', note: '' }

const LABELS = {
  customerName: 'Customer name',
  customerPhone: 'Phone number',
  pickupTime: 'Pickup time',
}

function validate(values) {
  const errors = {}
  if (!values.customerName.trim()) errors.customerName = 'Enter the name we should call when your order is ready.'
  else if (values.customerName.trim().length > 80) errors.customerName = 'Please use 80 characters or fewer.'
  if (!values.customerPhone.trim()) errors.customerPhone = 'Enter a Malaysian mobile number so we can reach you.'
  else if (!isMalaysianMobile(values.customerPhone)) errors.customerPhone = 'Use a Malaysian mobile such as 012-3456789 or 60123456789.'
  if (!values.pickupTime) errors.pickupTime = 'Choose when you would like to collect your order.'
  return errors
}

export default function Checkout({ lines, settings, onClearCart, onRemove, onClear }) {
  const navigate = useNavigate()
  const [values, setValues] = useState(INITIAL)
  const [touched, setTouched] = useState({})
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const refs = useRef({})
  const slots = useMemo(() => pickupSlots(), [])
  const totals = cartTotals(lines, settings.taxBasisPoints)
  const errors = validate(values)
  const shown = (field) => (submitted || touched[field]) && errors[field]

  const update = (field, value) => setValues((old) => ({ ...old, [field]: value }))
  const describedBy = (field, extra) => [shown(field) ? `${field}-error` : '', extra].filter(Boolean).join(' ') || undefined

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmitted(true)
    setSubmitError('')
    const firstInvalid = Object.keys(errors)[0]
    if (firstInvalid) {
      refs.current[firstInvalid]?.focus()
      return
    }
    if (lines.length === 0) {
      setSubmitError('Your cart is empty. Add an item before checking out.')
      return
    }
    setSubmitting(true)
    try {
      const orderId = await submitCustomerOrder({ lines, customer: values, totals, settings })
      onClearCart()
      navigate(`/order/${orderId}`, { replace: true })
    } catch (error) {
      setSubmitError(error?.code === 'permission-denied'
        ? 'The shop could not accept this order right now. Please call us instead.'
        : `We could not send your order: ${error?.message || 'please try again.'}`)
      setSubmitting(false)
    }
  }

  if (lines.length === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-16 text-center">
        <h1 className="text-3xl font-extrabold">Your cart is empty</h1>
        <p className="mt-2 text-charcoal/70">Add something from the menu first.</p>
        <Link to="/" className="mt-6 inline-block rounded-xl bg-ember-600 px-6 py-3 font-extrabold text-white">Browse the menu</Link>
      </div>
    )
  }

  return (
    <div className="mx-auto grid w-full max-w-6xl gap-6 px-4 py-8 sm:px-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
      <section aria-labelledby="checkout-heading">
        <p className="text-sm font-bold uppercase tracking-widest text-ember-600">Almost there</p>
        <h1 id="checkout-heading" tabIndex={-1} className="mt-1 text-3xl font-extrabold">Checkout</h1>
        <p className="mt-2 text-sm text-charcoal/70">You pay at the counter when you collect. No card details needed here.</p>

        <form noValidate onSubmit={handleSubmit} aria-labelledby="checkout-heading" className="mt-6 space-y-6 rounded-2xl border border-ember-100 bg-white p-5 shadow-sm sm:p-6">
          <div className="field">
            <label htmlFor="customerName">Customer name <span aria-hidden="true">*</span></label>
            <input
              ref={(node) => { refs.current.customerName = node }}
              id="customerName"
              name="customerName"
              type="text"
              autoComplete="name"
              required
              maxLength={80}
              value={values.customerName}
              aria-invalid={Boolean(shown('customerName'))}
              aria-describedby={describedBy('customerName')}
              onChange={(event) => update('customerName', event.target.value)}
              onBlur={() => setTouched((old) => ({ ...old, customerName: true }))}
            />
            {shown('customerName') && <p id="customerName-error" className="error-msg !block">{errors.customerName}</p>}
          </div>

          <div className="field">
            <label htmlFor="customerPhone">Phone number <span aria-hidden="true">*</span></label>
            <p id="customerPhone-hint" className="text-xs text-charcoal/60">Malaysian mobile, e.g. 012-3456789 or 60123456789.</p>
            <input
              ref={(node) => { refs.current.customerPhone = node }}
              id="customerPhone"
              name="customerPhone"
              type="tel"
              inputMode="numeric"
              autoComplete="tel"
              required
              value={values.customerPhone}
              aria-invalid={Boolean(shown('customerPhone'))}
              aria-describedby={describedBy('customerPhone', 'customerPhone-hint')}
              onChange={(event) => update('customerPhone', event.target.value)}
              onBlur={() => setTouched((old) => ({ ...old, customerPhone: true }))}
            />
            {shown('customerPhone') && <p id="customerPhone-error" className="error-msg !block">{errors.customerPhone}</p>}
          </div>

          <fieldset>
            <legend className="mb-2 font-bold">Order type <span aria-hidden="true">*</span></legend>
            <div className="flex flex-wrap gap-2">
              {[
                ['TAKEAWAY', '🛍️ Takeaway'],
                ['DINE_IN', '🍽️ Dine-in'],
                ['DELIVERY', '🛵 Delivery'],
              ].map(([value, label]) => (
                <label key={value} className="choice flex min-h-12 cursor-pointer items-center rounded-xl border-2 border-ember-100 px-4 py-2 font-semibold has-[:checked]:border-ember-500 has-[:checked]:bg-ember-50">
                  <input type="radio" name="orderType" value={value} checked={values.orderType === value} onChange={() => update('orderType', value)} className="mr-2 h-5 w-5 accent-ember-500" />
                  {label}
                </label>
              ))}
            </div>
          </fieldset>

          <div className="field">
            <label htmlFor="pickupTime">Pickup time <span aria-hidden="true">*</span></label>
            <select
              ref={(node) => { refs.current.pickupTime = node }}
              id="pickupTime"
              name="pickupTime"
              required
              value={values.pickupTime}
              aria-invalid={Boolean(shown('pickupTime'))}
              aria-describedby={describedBy('pickupTime')}
              onChange={(event) => update('pickupTime', event.target.value)}
              onBlur={() => setTouched((old) => ({ ...old, pickupTime: true }))}
            >
              {slots.map((slot) => <option key={slot} value={slot}>{slot === 'ASAP' ? 'ASAP (as soon as possible)' : slot}</option>)}
            </select>
            {shown('pickupTime') && <p id="pickupTime-error" className="error-msg !block">{errors.pickupTime}</p>}
          </div>

          <div className="field">
            <label htmlFor="generalNote">General order note <span className="font-normal text-charcoal/60">(optional)</span></label>
            <textarea
              id="generalNote"
              name="note"
              maxLength={300}
              value={values.note}
              onChange={(event) => update('note', event.target.value)}
              placeholder="e.g. deliver to the gate, extra napkins please"
            />
          </div>

          <div aria-live="polite">
            {submitError && <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm font-semibold text-red-900">{submitError}</p>}
          </div>

          <button type="submit" disabled={submitting || !isFirebaseConfigured} className="min-h-12 w-full rounded-xl bg-ember-600 px-5 py-3 text-lg font-extrabold text-white hover:bg-ember-700 disabled:cursor-not-allowed disabled:opacity-60">
            {submitting ? 'Sending order…' : !isFirebaseConfigured ? 'Configure Firebase to order' : `Place order · ${money(totals.totalSen)}`}
          </button>
        </form>
      </section>

      <aside aria-labelledby="summary-heading" className="rounded-2xl border border-ember-100 bg-white p-4 shadow-sm lg:sticky lg:top-4">
        <h2 id="summary-heading" className="text-xl font-extrabold">Order summary</h2>
        <CartContents lines={lines} taxBasisPoints={settings.taxBasisPoints} onRemove={onRemove} onClear={onClear} />
        <p className="mt-4 text-xs text-charcoal/60">After placing the order you will get a live tracking page. Keep that tab open to see each kitchen step.</p>
      </aside>
    </div>
  )
}
