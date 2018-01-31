package org.ygl.util

import java.time.Duration


fun Double.format(digits: Int) = String.format("%.${digits}f", this)

fun formatElapsed(msecs: Long): String {
    return if (msecs > 1000) {
        (msecs/1000.0).format(2) + " s"
    } else {
        "$msecs ms"
    }
}

inline fun time(body: () -> Unit): Long {
    val start = System.nanoTime()
    body()
    val stop = System.nanoTime()
    return Duration.ofNanos(stop - start).toMillis()
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