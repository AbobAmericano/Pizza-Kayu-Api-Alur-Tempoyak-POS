/** Whole-sen arithmetic, matching Money.kt exactly (no floating-point money). */

export function money(sen, currency = 'RM') {
  const value = Math.max(0, Math.trunc(Number(sen) || 0))
  return `${currency}${Math.floor(value / 100)}.${String(value % 100).padStart(2, '0')}`
}

/** Integer half-up rounding: (subtotal * rate + 5_000) / 10_000 in Money.kt. */
export function taxSen(subtotalSen, basisPoints) {
  const subtotal = Math.trunc(Number(subtotalSen) || 0)
  const rate = Math.trunc(Number(basisPoints) || 0)
  if (subtotal <= 0 || rate <= 0) return 0
  return Math.floor((subtotal * rate + 5_000) / 10_000)
}
