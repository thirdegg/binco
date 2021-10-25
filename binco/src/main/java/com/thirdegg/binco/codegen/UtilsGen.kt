package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.thirdegg.binco.BincoProcessor

class UtilsGen {

    private fun booleanToBinFun():FunSpec {
        return FunSpec.builder("boolToBin")
            .addParameter("bool", Boolean::class)
            .returns(ByteArray::class)
            .addCode("""
                val data = ByteArray(1)
                data.set(0, if (bool) 1 else 0)
                return data
            """.trimIndent()).build()
    }

    private fun shortToBinFun():FunSpec {
        return FunSpec.builder("shortToBin")
            .addParameter("num", Short::class)
            .returns(ByteArray::class)
            .addCode("""
                val data = ByteArray(2)
                data.set(0, (num.toInt() shr 8).toByte())
                data.set(1, num.toByte())
                return data
            """.trimIndent()).build()
    }

    private fun intToBinFun():FunSpec {
        return FunSpec.builder("intToBin")
            .addParameter("num", Int::class)
            .returns(ByteArray::class)
            .addCode("""
                val data = ByteArray(4)
                data.set(0, (num shr 24).toByte())
                data.set(1, (num shr 16).toByte())
                data.set(2, (num shr 8).toByte())
                data.set(3, num.toByte())
                return data
            """.trimIndent()).build()
    }

    private fun stringToBinFun():FunSpec {
        return FunSpec.builder("stringToBin")
            .addParameter("str",String::class)
            .returns(ByteArray::class)
            .addCode("""
                val dataArray = str.toByteArray()
                val size = intToBin(dataArray.size)
                val data = ByteArray(size.size + dataArray.size)
                System.arraycopy(size, 0, data, 0, size.size)
                System.arraycopy(dataArray, 0, data, size.size, dataArray.size)
                return data
            """.trimIndent()).build()
    }

    private fun binToBoolean():FunSpec {
        return FunSpec.builder("binToBool")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", Int::class)
            .returns(Boolean::class)
            .addCode("""
                return arr[offset] == 1.toByte()
            """.trimIndent()).build()
    }

    private fun binToShort():FunSpec {
        return FunSpec.builder("binToShort")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", Int::class)
            .returns(Short::class)
            .addCode("""
                var data:Short = 0.toShort()
                data = (data + ((arr[offset].toInt() and 0xff) shl 8)).toShort()
                data = (data + (arr[offset + 1].toInt() and 0xff)).toShort()
                return data
            """.trimIndent()).build()
    }

    private fun binToInt():FunSpec {
        return FunSpec.builder("binToInt")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", Int::class)
            .returns(Int::class)
            .addCode("""
                var data = 0
                data += (arr[offset].toInt() and 0xff) shl 24
                data += (arr[offset + 1].toInt() and 0xff) shl 16
                data += (arr[offset + 2].toInt() and 0xff) shl 8
                data += (arr[offset + 3].toInt() and 0xff)
                return data
            """.trimIndent()).build()
    }

    private fun binToString():FunSpec {
        return FunSpec.builder("binToString")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", Int::class)
            .addParameter("size", Int::class)
            .returns(String::class)
            .addCode("""
                return String(arr, offset, size)
            """.trimIndent()).build()
    }

    fun build(): FileSpec {
        return FileSpec.builder(BincoProcessor.BINCO_PKG, "DecodeUtils")
            .addType(
                TypeSpec.objectBuilder("DecodeUtils")
                    .addFunction(booleanToBinFun())
                    .addFunction(shortToBinFun())
                    .addFunction(intToBinFun())
                    .addFunction(stringToBinFun())
                    .addFunction(binToBoolean())
                    .addFunction(binToShort())
                    .addFunction(binToInt())
                    .addFunction(binToString())
                    .build()
            ).build()
    }

}