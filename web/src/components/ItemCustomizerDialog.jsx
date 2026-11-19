import { useEffect, useMemo, useRef, useState } from 'react'
import { sizesFor, imageUrl, sizeDeltaSen } from '../data/menu.js'
import { money } from '../lib/money.js'
import { priceForLine } from '../lib/order.js'

/**
 * Native <dialog> modal. No role="dialog", aria-modal, focus trap or Esc handler:
 * showModal() gives all of that, plus focus return to the card that opened it.
 */
export default function ItemCustomizerDialog({ item, toppings, onAdd, onClose }) {
  const dialogRef = useRef(null)
  const [sizeId, setSizeId] = useState(item.isPizza ? 's10' : 'regular')
  const [chosen, setChosen] = useState([])
  const [qty, setQty] = useState(1)
  const [note, setNote] = useState('')
  const sizes = useMemo(() => sizesFor(item), [item])

  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog && !dialog.open) dialog.showModal()
  }, [])

  // closedby="any" is not in Safari yet; keep click-outside working with a pointer fallback.
  useEffect(() => {
    if ('closedBy' in HTMLDialogElement.prototype) return undefined
    const dialog = dialogRef.current
    const outside = (event) => {
      const rect = dialog.getBoundingClientRect()
      return event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom
    }
    let pressedOutside = false
    const down = (event) => { pressedOutside = outside(event) }
    const up = (event) => { if (pressedOutside && outside(event)) dialog.close() }
    dialog.addEventListener('pointerdown', down)
    dialog.addEventListener('pointerup', up)
    return () => {
      dialog.removeEventListener('pointerdown', down)
      dialog.removeEventListener('pointerup', up)
    }
  }, [])

  const unitPriceSen = priceForLine(item, sizeId, chosen, toppings)

  const toggleTopping = (id) => {
    setChosen((old) => (old.includes(id) ? old.filter((value) => value !== id) : [...old, id]))
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    onAdd(item, {
      sizeId,
      toppingIds: chosen,
      toppingLabels: chosen.map((id) => toppings.find((topping) => topping.id === id)?.label).filter(Boolean),
      qty,
      note,
    })
    dialogRef.current?.close()
  }

  return (
    <dialog
      ref={dialogRef}
      className="item-dialog"
      aria-labelledby="customizer-title"
      closedby="any"
      onClose={() => {
        if (dialogRef.current) dialogRef.current.returnValue = ''
        onClose()
      }}
    >
      <form method="dialog" onSubmit={handleSubmit}>
        <div className="relative">
          {item.imageKey && (
            <img src={imageUrl(item)} alt="" className="h-44 w-full object-cover" width="640" height="360" />
          )}
          <button
            type="button"
            onClick={() => dialogRef.current?.close()}
            className="absolute right-3 top-3 grid h-11 w-11 place-items-center rounded-full bg-white/95 text-2xl font-bold text-charcoal shadow"
            aria-label="Close customizer"
          >
            ×
          </button>
        </div>

        <div className="space-y-5 p-5 sm:p-6">
          <header>
            <p className="text-sm font-semibold uppercase tracking-wide text-ember-600">Make it yours</p>
            <h2 id="customizer-title" className="mt-1 text-2xl font-extrabold">
              <span aria-hidden="true">{item.emoji} </span>{item.name}
            </h2>
            {item.description && <p className="mt-1 text-sm text-charcoal/70">{item.description}</p>}
          </header>

          {sizes.length > 0 && (
            <fieldset>
              <legend className="mb-2 text-sm font-bold">Choose a size</legend>
              <div className="grid grid-cols-3 gap-2">
                {sizes.map((size) => (
                  <label key={size.id} className="choice flex min-h-14 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-ember-100 bg-white px-2 py-2 text-center has-[:checked]:border-ember-500 has-[:checked]:bg-ember-50">
                    <input
                      type="radio"
                      name="size"
                      value={size.id}
                      checked={sizeId === size.id}
                      onChange={() => setSizeId(size.id)}
                      className="sr-only"
                    />
                    <span className="text-base font-extrabold">{size.label}</span>
                    <span className="text-xs text-charcoal/60">{size.detail}</span>
                    <span className="text-xs font-semibold text-ember-600">
                      +{money(sizeId === 's12' ? item.twelveInchDeltaSen : sizeDeltaSen(item, size.id))}
                    </span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}

          {toppings.length > 0 && (
            <fieldset>
              <legend className="mb-2 text-sm font-bold">Extra toppings</legend>
              <div className="space-y-2">
                {toppings.map((topping) => (
                  <label key={topping.id} className="choice flex min-h-12 cursor-pointer items-center gap-3 rounded-xl border border-ember-100 bg-white px-3 py-2 has-[:checked]:border-ember-500 has-[:checked]:bg-ember-50">
                    <input
                      type="checkbox"
                      name="topping"
                      value={topping.id}
                      checked={chosen.includes(topping.id)}
                      onChange={() => toggleTopping(topping.id)}
                      className="h-5 w-5 accent-ember-500"
                    />
                    <span aria-hidden="true">{topping.emoji}</span>
                    <span className="flex-1 font-medium">{topping.label}</span>
                    <span className="text-sm font-bold text-ember-600">+{money(topping.priceSen)}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          )}

          <div className="flex items-center justify-between gap-3">
            <span className="text-sm font-bold">Quantity</span>
            <div className="flex items-center gap-3">
              <button type="button" onClick={() => setQty((value) => Math.max(1, value - 1))} disabled={qty <= 1} aria-label="Decrease quantity" className="grid h-11 w-11 place-items-center rounded-full border-2 border-ember-200 text-xl font-bold disabled:opacity-40">−</button>
              <output aria-live="polite" className="w-6 text-center text-lg font-extrabold">{qty}</output>
              <button type="button" onClick={() => setQty((value) => Math.min(30, value + 1))} disabled={qty >= 30} aria-label="Increase quantity" className="grid h-11 w-11 place-items-center rounded-full border-2 border-ember-200 text-xl font-bold disabled:opacity-40">+</button>
            </div>
          </div>

          <div className="field">
            <label htmlFor="item-note">Kitchen note <span className="font-normal text-charcoal/60">(optional)</span></label>
            <p id="item-note-hint" className="text-xs text-charcoal/60">e.g. kurang pedas, crispy crust</p>
            <textarea
              id="item-note"
              name="note"
              maxLength={200}
              value={note}
              onChange={(event) => setNote(event.target.value)}
              aria-describedby="item-note-hint"
              placeholder="Anything the kitchen should know?"
            />
          </div>

          <button
            type="submit"
            autoFocus
            className="min-h-12 w-full rounded-xl bg-ember-500 px-5 py-3 text-base font-extrabold text-white hover:bg-ember-600"
          >
            Add to cart · {money(unitPriceSen * qty)}
          </button>
        </div>
      </form>
    </dialog>
  )
}
