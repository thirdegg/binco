package com.thirdegg.test

import com.thirdegg.binco.BincoDecoder


fun main() {

    val list = ArrayList<Int>()
    list.add(1)
    list.add(1994)
    list.add(9999)
    list.add(19999)
    list.add(-19999)
    val data = DataBin("Test", 9999, SubDataBin("Test 2"), Data.Status.SUCCESS, list).toMessage()
    data.forEach {
        print("${it}, ")
    }
    println()
    println(String(data))
    println("Data size is ${data.size}")

    val dataMessage = BincoDecoder.decode(data) as DataBin
    println(dataMessage.getStatus())

}