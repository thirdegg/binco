package com.thirdegg.test

import com.thirdegg.binco.Binco

@Binco(1)
interface Data {
    @Binco.Field(1)
    fun getJ1(): String

    @Binco.Field(2)
    fun getJ2(): Int

    @Binco.Field(3)
    fun getKkk(): SubData

    @Binco.Field(4)
    fun getStatus(): Status

    @Binco.Field(5)
    fun getListOfStrings(): List<Int>

    @Binco(3)
    enum class Status {
        @Binco.Field(1)
        SUCCESS,

        @Binco.Field(2)
        ERROR,

        @Binco.Field(3)
        ERROR_2

    }

}

@Binco(2)
interface SubData {
    @Binco.Field(1)
    fun getJkk2():String
}

