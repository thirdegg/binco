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

//    val begin = System.currentTimeMillis()
//    (0..1000000).forEach {
        val list = ArrayList<Int>()
        list.add(1)
        list.add(2)
        list.add(3)
        list.add(4)
        list.add(5)
        val data =
            DataBin("Test", 9999, DataBin.StatusBin.SubDataBin("Test 2"), DataBin.StatusBin.ERROR_2, list, DataBin.StatusBin.SubDataBin("Test 2")).toMessage()
        printInfo(data)
        println((((BincoDecoder.decode(data) as DataBin).getAny()) as DataBin.StatusBin.SubDataBin).getJkk2())
//    }
//    println("Time ${System.currentTimeMillis() - begin}")

}