package com.thirdegg.test

import com.thirdegg.binco.BincoDecoder

fun printInfo(message:ByteArray) {
    println("Bytes:")
    message.forEach {
        print("${it}, ")
    }
    println()
    println("String:")
    println(String(message))
    println("Data size is ${message.size}")
    println("-=-=-=-=-")
}

fun main() {

    val list = ArrayList<Int>()
    list.add(1)
    list.add(1994)
    list.add(9999)
    list.add(19999)
    list.add(-19999)
    val data = DataBin("Test", 9999, DataBin.StatusBin.SubDataBin("Test 2"), DataBin.StatusBin.ERROR_2, list).toMessage()

    printInfo(DataBin.StatusBin.ERROR_2.toMessage())

//    val a = (BincoDecoder.decode(data) as DataBin)
    val a = (BincoDecoder.decode(DataBin.StatusBin.ERROR_2.toMessage()) as DataBin.StatusBin)
    println(a)

}