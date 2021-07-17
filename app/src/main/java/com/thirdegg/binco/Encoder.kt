package com.thirdegg.binco

@Message(1)
class Data(
    @Field(1)
    val j1:String,

    @Field(2)
    val j2:Int,

    @Field(3)
    val kkk:SubData
)

@Message(2)
class SubData {

    @Field(1)
    val jkk2 = "Лучший"

    fun a() {

    }
}