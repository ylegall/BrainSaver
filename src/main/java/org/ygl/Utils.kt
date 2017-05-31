package org.ygl


fun Double.format(digits: Int) = String.format("%.${digits}f", this)

fun formatElapsed(msecs: Long): String {
    return if (msecs > 1000) {
        (msecs/1000.0).format(2) + " s"
    } else {
        "$msecs ms"
    }
}