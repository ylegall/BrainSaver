package org.ygl.util


fun Double.format(digits: Int) = String.format("%.${digits}f", this)

fun formatElapsed(msecs: Long): String {
    return if (msecs > 1000) {
        (msecs/1000.0).format(2) + " s"
    } else {
        "$msecs ms"
    }
}

fun time(body: () -> Unit): Long {
    val elapsed = System.currentTimeMillis()
    body()
    return System.currentTimeMillis() - elapsed
}

fun unescape(str: String): String {
    val withoutQuotes = str.trim().substring(1 .. str.length-2)
    return withoutQuotes.replace("\\n", "\n").replace("\\t", "\t")
}

fun <T: Comparable<T>> T.clamp(min: T, max: T): T {
    if (this < min) return min
    if (this > max) return max
    return this
}

inline fun <T> T?.orElse(block: () -> T): T {
    return this ?: block()
}