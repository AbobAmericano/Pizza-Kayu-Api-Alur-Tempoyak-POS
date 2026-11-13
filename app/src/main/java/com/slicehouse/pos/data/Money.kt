package com.slicehouse.pos.data

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Money is always stored as whole sen (1 RM = 100 sen) in a [Long].
 * Whole numbers never drift the way Doubles do (0.1 + 0.2 != 0.3), so totals,
 * tax and change always add up to the sen.
 */
typealias Sen = Long

/** Tax rates in hundredths of a percent: 800 = 8%, 650 = 6.5%. */
typealias BasisPoints = Int

/** Formats sen for display, e.g. 2550 -> "RM25.50". */
fun money(value: Sen, currency: String = "RM"): String {
    val sign = if (value < 0) "-" else ""
    val amount = abs(value)
    return sign + currency + (amount / 100) + "." + (amount % 100).toString().padStart(2, '0')
}

/** Formats a price change, e.g. "+RM2.50" or "-RM2.50". */
fun signedMoney(value: Sen, currency: String = "RM"): String =
    if (value < 0) "-" + money(-value, currency) else "+" + money(value, currency)

/**
 * Parses what the user typed in a price field ("25", "25.5", "25,50") into sen.
 * Rounds to the nearest sen. Returns null for blank, negative or invalid input.
 */
fun parseMoney(text: String): Sen? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty()) return null
    return runCatching {
        val value = BigDecimal(cleaned)
        if (value.signum() < 0) return null
        value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }.getOrNull()
}

/** Sen as text for an editable price field: 2500 -> "25", 450 -> "4.50". */
fun moneyInput(value: Sen): String {
    val amount = abs(value)
    val sign = if (value < 0) "-" else ""
    return if (amount % 100 == 0L) sign + (amount / 100)
    else sign + (amount / 100) + "." + (amount % 100).toString().padStart(2, '0')
}

/** Converts an old RM Double (from the v1.4 JSON file) to sen, rounding half up. */
fun toSen(value: Double): Sen =
    BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()

/** Tax on [subtotal], rounded half up to the nearest sen. */
fun taxSen(subtotal: Sen, rate: BasisPoints): Sen {
    if (subtotal <= 0 || rate <= 0) return 0
    return (subtotal * rate + 5_000) / 10_000
}

/** 800 -> "8", 650 -> "6.5". */
fun percentLabel(rate: BasisPoints): String =
    BigDecimal(rate).movePointLeft(2).stripTrailingZeros().toPlainString()

/** Parses a tax field ("8", "6.5") into basis points. Null if invalid or outside 0-100%. */
fun parsePercent(text: String): BasisPoints? {
    val cleaned = text.trim().replace(',', '.')
    if (cleaned.isEmpty()) return null
    return runCatching {
        val value = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValueExact()
        value.takeIf { it in 0..10_000 }
    }.getOrNull()
}

/** Converts an old percent Double (8.0) to basis points (800). */
fun toBasisPoints(percent: Double): BasisPoints =
    BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toInt()
