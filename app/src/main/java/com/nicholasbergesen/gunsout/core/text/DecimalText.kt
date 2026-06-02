package com.nicholasbergesen.gunsout.core.text

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

fun String.normalizeDecimalInput(): String {
    var decimalSeen = false
    val out = StringBuilder(length)
    for (char in this) {
        when {
            char.isDigit() -> out.append(char)
            (char == '.' || char == ',') && !decimalSeen -> {
                out.append('.')
                decimalSeen = true
            }
        }
    }
    return out.toString()
}

fun String.toNormalizedDoubleOrNull(): Double? =
    normalizeDecimalInput().toDoubleOrNull()

fun formatOneDecimalOrInt(value: Double): String {
    val roundedInt = value.roundToInt()
    return if (abs(value - roundedInt) < 0.000_001) {
        roundedInt.toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}
