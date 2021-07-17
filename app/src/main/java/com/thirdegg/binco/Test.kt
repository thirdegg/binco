package com.thirdegg.binco

import com.thirdegg.binco.generated.toMessage

fun main() {
    Data().toMessage().forEach {
        print("$it, ")
    }
    println()
    println(String(Data().toMessage()))
}