package com.slicehouse.pos.print

import com.slicehouse.pos.data.Order
import com.slicehouse.pos.data.Settings
import com.slicehouse.pos.data.compactUrl
import com.slicehouse.pos.data.instagramHandle
import com.slicehouse.pos.data.money
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds ESC/POS byte streams for 58 mm and 80 mm thermal receipt printers.
 * Everything is plain text plus the usual alignment/bold/feed/cut commands, so it
 * works with the common Xprinter/Goojprt/Epson-compatible models.
 */
object ReceiptPrinter {

    fun buildReceiptBytes(order: Order, settings: Settings): ByteArray {
        val builder = EscPosBuilder(if (settings.paperWidthMm >= 76) 48 else 32)
        builder.init()

        builder.centeredBold(settings.shopName)
        builder.centeredWrapped(settings.address)
        if (settings.phone.isNotBlank()) builder.centered(settings.phone)
        if (settings.instagram.isNotBlank()) builder.centered("Instagram: ${instagramHandle(settings.instagram)}")
        if (settings.tiktok.isNotBlank()) builder.centered("TikTok: ${settings.tiktok}")
        if (settings.locationUrl.isNotBlank()) builder.centeredWrapped("Maps: ${compactUrl(settings.locationUrl)}")

        builder.divider()
        builder.line("Order #${order.number}")
        builder.line(receiptDate(order.createdAt))
        builder.line(
            order.type.label + if (order.table.isNotBlank()) "  |  Table ${order.table}" else ""
        )
        if (order.pickupTime.isNotBlank()) builder.line("Pickup: ${order.pickupTime}")
        if (order.customerName.isNotBlank()) builder.line("Customer: ${order.customerName}")
        if (order.customerPhone.isNotBlank()) builder.line("Phone: ${order.customerPhone}")
        if (order.address.isNotBlank()) builder.wrapped("Address: ${order.address}")

        builder.divider()
        order.lines.forEach { line ->
            val amount = money(line.lineTotal, settings.currency)
            val label = if (line.modifiers().isNotBlank()) "${line.qty}x ${line.name} ${line.modifiers()}" else "${line.qty}x ${line.name}"
            val nameWidth = (builder.width - amount.length - 1).coerceAtLeast(8)
            val nameLines = wrap(label, nameWidth)
            nameLines.forEachIndexed { index, part ->
                if (index == 0) {
                    builder.line(part.padEnd(nameWidth) + " " + amount)
                } else {
                    builder.line(part)
                }
            }
        }

        builder.divider()
        builder.twoColumns("Subtotal", money(order.subtotal, settings.currency))
        builder.twoColumns("Tax", money(order.tax, settings.currency))
        builder.twoColumns("TOTAL", money(order.total, settings.currency))
        builder.twoColumns(
            order.payment.label,
            money(if (order.payment.name == "CASH") order.cashReceived else order.total, settings.currency)
        )
        if (order.payment.name == "CASH" && order.change > 0) {
            builder.twoColumns("Change", money(order.change, settings.currency))
        }
        if (order.note.isNotBlank()) {
            builder.divider()
            builder.wrapped("Note: ${order.note}")
        }

        if (settings.instagram.isNotBlank() || settings.tiktok.isNotBlank() || settings.locationUrl.isNotBlank()) {
            builder.divider()
            if (settings.instagram.isNotBlank()) builder.centered("Instagram: ${instagramHandle(settings.instagram)}")
            if (settings.tiktok.isNotBlank()) builder.centered("TikTok: ${settings.tiktok}")
            if (settings.locationUrl.isNotBlank()) builder.centeredWrapped("Maps: ${compactUrl(settings.locationUrl)}")
        }

        builder.blank()
        builder.centeredWrapped(settings.footer)
        builder.feed(3)
        builder.cut()
        return builder.toByteArray()
    }

    /**
     * Simple kitchen ticket printed before payment: items, sizes, toppings and
     * notes only. No prices, payment details, social links or footer message.
     */
    fun buildKitchenBytes(order: Order, settings: Settings): ByteArray {
        val builder = EscPosBuilder(if (settings.paperWidthMm >= 76) 48 else 32)
        builder.init()

        builder.centeredBold(settings.shopName)
        builder.centeredBold("KITCHEN")
        builder.divider()
        builder.line("Order #${order.number}")
        builder.line(receiptDate(order.createdAt))
        builder.line(
            order.type.label + if (order.table.isNotBlank()) "  |  Table ${order.table}" else ""
        )
        if (order.pickupTime.isNotBlank()) builder.line("Pickup: ${order.pickupTime}")
        if (order.customerName.isNotBlank()) builder.line("Customer: ${order.customerName}")
        if (order.customerPhone.isNotBlank()) builder.line("Phone: ${order.customerPhone}")
        if (order.address.isNotBlank()) builder.wrapped("Address: ${order.address}")

        builder.divider()
        order.lines.forEach { line ->
            val label = if (line.modifiers().isNotBlank()) "${line.qty}x ${line.name} ${line.modifiers()}" else "${line.qty}x ${line.name}"
            wrap(label, builder.width).forEach(builder::line)
        }

        if (order.note.isNotBlank()) {
            builder.divider()
            builder.wrapped("Note: ${order.note}")
        }

        builder.feed(3)
        builder.cut()
        return builder.toByteArray()
    }

    fun buildTestBytes(settings: Settings): ByteArray {
        val builder = EscPosBuilder(if (settings.paperWidthMm >= 76) 48 else 32)
        builder.init()
        builder.centeredBold(settings.shopName)
        builder.centeredWrapped(settings.address)
        if (settings.phone.isNotBlank()) builder.centered(settings.phone)
        builder.divider()
        builder.centeredBold("TEST PRINT")
        builder.centered(receiptDate(System.currentTimeMillis()))
        builder.blank()
        builder.centeredWrapped("If you can read this, the printer is connected and ready.")
        if (settings.printerName.isNotBlank()) {
            builder.blank()
            builder.centeredWrapped("Printer: ${settings.printerName}")
        }
        builder.blank()
        builder.centeredWrapped(settings.footer)
        builder.feed(3)
        builder.cut()
        return builder.toByteArray()
    }

    private fun receiptDate(time: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(time))

    private fun wrap(text: String, width: Int): List<String> {
        val lines = mutableListOf<String>()
        var current = ""
        fun flush() {
            if (current.isNotEmpty()) {
                lines += current
                current = ""
            }
        }
        text.split(' ').forEach { raw ->
            var word = raw
            while (word.isNotEmpty()) {
                when {
                    current.isEmpty() && word.length <= width -> {
                        current = word
                        word = ""
                    }
                    current.isEmpty() -> {
                        lines += word.take(width)
                        word = word.drop(width)
                    }
                    current.length + 1 + word.length <= width -> {
                        current += " $word"
                        word = ""
                    }
                    else -> flush()
                }
            }
        }
        flush()
        return lines.ifEmpty { listOf("") }
    }

    private fun sanitize(value: String): String {
        val prepared = value
            .replace("\r", " ")
            .replace("\n", " ")
            .replace("\u20AC", "EUR")
            .replace("\u00A3", "GBP")
        return buildString {
            prepared.forEach { ch ->
                val mapped = when (ch) {
                    '\u00D7' -> 'x' // ×
                    '\u00B7', '\u2022' -> '-' // · •
                    '\u2014', '\u2013' -> '-' // — –
                    '\u201C', '\u201D' -> '"' // “ ”
                    '\u2018', '\u2019' -> '\'' // ‘ ’
                    else -> ch
                }
                append(if (mapped.code in 32..126) mapped else '?')
            }
        }
    }

    private class EscPosBuilder(val width: Int) {
        private val out = ByteArrayOutputStream()

        fun init() {
            out.write(byteArrayOf(0x1B, 0x40))
            out.write(byteArrayOf(0x1B, 0x32)) // default line spacing
            align(false)
            bold(false)
        }

        fun line(text: String) = write(sanitize(text).take(width))

        fun wrapped(text: String) = wrap(sanitize(text), width).forEach(::line)

        fun centered(text: String) {
            align(true)
            write(sanitize(text).take(width))
            align(false)
        }

        fun centeredWrapped(text: String) {
            align(true)
            wrap(sanitize(text), width).forEach { part ->
                val pad = ((width - part.length) / 2).coerceAtLeast(0)
                write(" ".repeat(pad) + part)
            }
            align(false)
        }

        fun centeredBold(text: String) {
            align(true)
            bold(true)
            write(sanitize(text).take(width))
            bold(false)
            align(false)
        }

        fun twoColumns(left: String, right: String) {
            val cleanLeft = sanitize(left)
            val cleanRight = sanitize(right)
            val leftWidth = (width - cleanRight.length - 1).coerceAtLeast(1)
            write(cleanLeft.take(leftWidth).padEnd(leftWidth) + " " + cleanRight.take(width))
        }

        fun divider() = line("-".repeat(width))

        fun blank() = write("")

        fun feed(lines: Int) {
            out.write(byteArrayOf(0x1B, 0x64, lines.coerceIn(0, 255).toByte()))
        }

        fun cut() {
            out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        }

        private fun align(center: Boolean) {
            out.write(byteArrayOf(0x1B, 0x61, if (center) 0x01 else 0x00))
        }

        private fun bold(on: Boolean) {
            out.write(byteArrayOf(0x1B, 0x45, if (on) 0x01 else 0x00))
        }

        private fun write(text: String) {
            out.write(text.toByteArray(Charsets.US_ASCII))
            out.write('\n'.code)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }
}
