package org.ygl

internal object StringMinifier {

    // G[x][y]: BF code that transforms x to y.
    private var G = Array(256, { _ -> Array(256, { _ -> ""}) })

    init {
        // initial state for G[x][y]: go from x to y using +s or -s.
        for (x in 0..255) {
            for (y in 0..255) {
                var delta = y - x
                if (delta > 128) delta -= 256
                if (delta < -128) delta += 256

                if (delta >= 0) {
                    G[x][y] = "+".repeat(delta)
                } else {
                    G[x][y] = "-".repeat(Math.abs(delta))
                }
            }
        }

        // keep applying rules until we can't find any more shortenings
        var iter = true
        while (iter) {
            iter = false

            // multiplication by n/d
            for (x in 0..255) {
                for (n in 1..39) {
                    for (d in 1..39) {
                        var j = x
                        var y = 0
                        for (i in 0..255) {
                            if (j == 0) break
                            j = j - d + 256 and 255
                            y = y + n and 255
                        }
                        if (j == 0) {
                            val s = "[" + "-".repeat(d) + ">" + "+".repeat(n) + "<]>"
                            if (s.length < G[x][y].length) {
                                G[x][y] = s
                                iter = true
                            }
                        }

                        j = x
                        y = 0
                        for (i in 0..255) {
                            if (j == 0) break
                            j = j + d and 255
                            y = y - n + 256 and 255
                        }
                        if (j == 0) {
                            val s = "[" + "+".repeat(d) + ">" + "-".repeat(n) + "<]>"
                            if (s.length < G[x][y].length) {
                                G[x][y] = s
                                iter = true
                            }
                        }
                    }
                }
            }

            // combine number schemes
            for (x in 0..255) {
                for (y in 0..255) {
                    for (z in 0..255) {
                        if (G[x][z].length + G[z][y].length < G[x][y].length) {
                            G[x][y] = G[x][z] + G[z][y]
                            iter = true
                        }
                    }
                }
            }
        }
    }

    fun generate(s: String): String {
        val sb = StringBuilder()
        var lastc = 0
        for (c in s.toCharArray()) {
            val ci = c.toInt()
            val a = G[lastc][ci]
            val b = G[0][ci]
            if (a.length <= b.length) {
                sb.append(a)
            } else {
                sb.append(">" + b)
            }
            sb.append(".")
            lastc = ci
        }
        return sb.toString()
    }

}

fun main(args: Array<String>) {
    println(StringMinifier.generate("hello world"))
}