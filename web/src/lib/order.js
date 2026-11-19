import { sizeDeltaSen } from '../data/menu.js'
import { taxSen } from './money.js'

/** The POS status vocabulary. The labels are what the customer sees at each step. */
export const STATUSES = {
  PENDING: 'Pending',
  RECEIVED: 'Accepted',
  PREPARING: 'Preparing',
  BAKING: 'In Oven',
  READY: 'Ready for Pickup',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
}

export const ACTIVE_STATUSES = ['PENDING', 'RECEIVED', 'PREPARING', 'BAKING', 'READY', 'COMPLETED']

const SIZE_LABELS = { s10: '10"', s11: '11"', s12: '12"' }

export function statusLabel(status) {
  return STATUSES[status] || String(status || 'Unknown').toLowerCase()
}

export function priceForLine(item, sizeId, toppingIds = [], toppings = []) {
  const size = sizeDeltaSen(item, sizeId)
  const extra = toppingIds.reduce((sum, id) => sum + (toppings.find((topping) => topping.id === id)?.priceSen || 0), 0)
  return (Number(item?.priceSen) || 0) + size + extra
}

/**
 * A cart line has exactly the shape of the POS CartLine / OrderLineDto:
 * toppings are display labels (not ids), money is whole sen.
 */
export function buildCartLine(item, { sizeId, toppingIds, toppingLabels, qty, note }, toppings = []) {
  const unitPriceSen = priceForLine(item, sizeId, toppingIds, toppings)
  return {
    lineId: `${item.id}-${sizeId || 'regular'}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`,
    itemId: item.id,
    name: item.name,
    emoji: item.emoji || '🍕',
    unitPriceSen,
    qty: Math.max(1, Math.trunc(qty) || 1),
    sizeLabel: item.isPizza ? SIZE_LABELS[sizeId] || '10"' : null,
    crustLabel: null,
    toppings: toppingLabels || [],
    note: (note || '').trim().slice(0, 200),
  }
}

export function lineTotalSen(line) {
  return line.unitPriceSen * line.qty
}

export function cartTotals(lines, taxBasisPoints) {
  const subtotalSen = lines.reduce((sum, line) => sum + lineTotalSen(line), 0)
  const taxAmountSen = taxSen(subtotalSen, taxBasisPoints)
  return { subtotalSen, taxSen: taxAmountSen, totalSen: subtotalSen + taxAmountSen }
}

/** Accepts 012-3456789, 0123456789, +60123456789 and 60123456789. */
export function normalizePhone(value) {
  return String(value || '').replace(/\D/g, '')
}

export function isMalaysianMobile(value) {
  const digits = normalizePhone(value)
  return /^(01\d|601\d)\d{7}$/.test(digits)
}

/** Pickup slots are generated in Malaysia time so they match the shop's clock. */
export function pickupSlots(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Kuala_Lumpur',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(now)
  const get = (type) => Number(parts.find((part) => part.type === type)?.value || 0)
  let minutes = get('hour') * 60 + get('minute')
  const slots = ['ASAP']
  // First slot is at least 20 minutes away, then every 30 minutes until closing-ish (22:30).
  minutes = Math.ceil((minutes + 20) / 30) * 30
  while (minutes <= 22 * 60 + 30) {
    const hour = String(Math.floor(minutes / 60)).padStart(2, '0')
    const minute = String(minutes % 60).padStart(2, '0')
    slots.push(`${hour}:${minute}`)
    minutes += 30
  }
  return slots.slice(0, 19)
}
