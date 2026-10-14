package com.example.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * CurrencyHelper ensures exact financial arithmetic in integer Paise.
 * 1 INR (₹) = 100 Paise.
 * Never uses Float or Double for stored financial figures to avoid floating-point drift.
 */
object CurrencyHelper {

    private val inrSymbols = DecimalFormatSymbols(Locale("en", "IN")).apply {
        currencySymbol = "₹"
    }

    /**
     * Formats paise (Long) to INR display string: e.g. "₹1,250.00" or "+₹400.00" / "-₹250.00"
     */
    fun formatInr(
        paise: Long,
        showSign: Boolean = false,
        includeSymbol: Boolean = true,
        includeDecimals: Boolean = true
    ): String {
        val isNegative = paise < 0
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100

        val pattern = if (includeDecimals) "#,##,##0.00" else "#,##,##0"
        val formatter = DecimalFormat(pattern, inrSymbols)
        val formattedNumber = formatter.format(rupees + (remainingPaise / 100.0))

        val sign = when {
            isNegative -> "-"
            showSign && paise > 0 -> "+"
            else -> ""
        }

        val symbol = if (includeSymbol) "₹" else ""
        return "$sign$symbol$formattedNumber"
    }

    /**
     * Formats integer rupees when decimals are not required, e.g. "₹1,250"
     */
    fun formatInrCompact(paise: Long, showSign: Boolean = false): String {
        return formatInr(paise, showSign = showSign, includeSymbol = true, includeDecimals = false)
    }

    /**
     * Converts a user-inputted string (e.g. "1250", "1250.50", "1,250") into integer Paise.
     * Returns 0L if blank or invalid.
     */
    fun parseInrToPaise(input: String): Long {
        val cleaned = input.trim().replace(",", "").replace("₹", "").replace(" ", "")
        if (cleaned.isEmpty()) return 0L

        return try {
            if (cleaned.contains(".")) {
                val parts = cleaned.split(".")
                val wholeRupees = parts[0].toLongOrNull() ?: 0L
                val fractionalString = parts.getOrElse(1) { "" }.take(2).padEnd(2, '0')
                val fractionalPaise = fractionalString.toLongOrNull() ?: 0L
                val isNeg = cleaned.startsWith("-")
                val total = abs(wholeRupees) * 100 + fractionalPaise
                if (isNeg) -total else total
            } else {
                (cleaned.toLongOrNull() ?: 0L) * 100
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Formats Paise back to an editable input text, e.g. "1250" or "1250.50"
     */
    fun paiseToInputString(paise: Long): String {
        if (paise == 0L) return ""
        val absPaise = abs(paise)
        val rupees = absPaise / 100
        val remainingPaise = absPaise % 100
        val prefix = if (paise < 0) "-" else ""
        return if (remainingPaise == 0L) {
            "$prefix$rupees"
        } else {
            "$prefix$rupees.${remainingPaise.toString().padStart(2, '0')}"
        }
    }

    /**
     * Formats ROI percentage with 1 decimal place, e.g. "40.0%" or "+15.2%" or "N/A"
     */
    fun formatRoi(roiPercentage: Double?, showSign: Boolean = true): String {
        if (roiPercentage == null || roiPercentage.isNaN() || roiPercentage.isInfinite()) {
            return "N/A"
        }
        val sign = if (showSign && roiPercentage > 0) "+" else ""
        return String.format(Locale.US, "%s%.1f%%", sign, roiPercentage)
    }

    /**
     * Formats ITM percentage with 1 decimal place, e.g. "60.0%" or "N/A"
     */
    fun formatItm(itmPercentage: Double?): String {
        if (itmPercentage == null || itmPercentage.isNaN() || itmPercentage.isInfinite()) {
            return "N/A"
        }
        return String.format(Locale.US, "%.1f%%", itmPercentage)
    }
}
