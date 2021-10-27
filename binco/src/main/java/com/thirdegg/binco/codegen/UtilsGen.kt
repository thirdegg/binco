package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
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
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Boolean::class)
            .addCode("""
                offset.add(1)
                return arr[offset.get()] == 1.toByte()
            """.trimIndent()).build()
    }

    private fun binToShort():FunSpec {
        return FunSpec.builder("binToShort")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Short::class)
            .addCode("""
                var data:Short = 0.toShort()
                data = (data + ((arr[offset.get()].toInt() and 0xff) shl 8)).toShort()
                data = (data + (arr[offset.get() + 1].toInt() and 0xff)).toShort()
                offset.add(2)
                return data
            """.trimIndent()).build()
    }

    private fun binToInt():FunSpec {
        return FunSpec.builder("binToInt")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Int::class)
            .addCode("""
                var data = 0
                data += (arr[offset.get()].toInt() and 0xff) shl 24
                data += (arr[offset.get() + 1].toInt() and 0xff) shl 16
                data += (arr[offset.get() + 2].toInt() and 0xff) shl 8
                data += (arr[offset.get() + 3].toInt() and 0xff)
                offset.add(4)
                return data
            """.trimIndent()).build()
    }

    private fun binToString():FunSpec {
        return FunSpec.builder("binToString")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(String::class)
            .addCode("""
                val size = DecodeUtils.binToInt(arr, offset)
                val data = String(arr, offset.get(), size)
                offset.add(size)
                return data
            """.trimIndent()).build()
    }

    private fun createCounter():TypeSpec {

        return TypeSpec.classBuilder("Counter")
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("counter", Int::class.asTypeName())
                    .build()
            )
            .addProperty(
                PropertySpec.builder("counter", Int::class.asTypeName())
                    .initializer("counter")
                    .mutable()
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(
                FunSpec.builder("add")
                    .addParameter(ParameterSpec.builder("count", Int::class).build())
                    .addStatement("this.counter += count")
                    .build()
            )
            .addFunction(
                FunSpec.builder("get")
                    .returns(Int::class)
                    .addStatement("return this.counter")
                    .build()
            )
            .build()
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
                    .addType(createCounter())
                    .build()
            ).build()
    }

}