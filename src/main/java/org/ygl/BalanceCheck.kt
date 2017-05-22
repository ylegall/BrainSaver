package org.ygl

import java.io.*


fun main(args: Array<String>) {
    val reader = BufferedReader(FileReader(File("output.txt")))

    var valid = "valid"
    var count = 0
    var totalCount = 0

    reader.use {
        do {
            var c = it.read()
            if (c < 0) break
            totalCount += 1

            when (c.toChar()) {
                '[' -> count += 1
                ']' -> count -= 1
            }
            if (count < 0) {
                valid = "invalid"
                break
            }
        } while (c != -1)
    }
    if (count != 0) valid = "invalid"

    println("file is $valid")
    println("$totalCount chars")
}