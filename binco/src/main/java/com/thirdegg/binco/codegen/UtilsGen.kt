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
            .addStatement("return ByteBuffer.allocate(2).putShort(num).array()")
            .build()
    }

    private fun intToBinFun():FunSpec {
        return FunSpec.builder("intToBin")
            .addParameter("num", Int::class)
            .returns(ByteArray::class)
            .addStatement("return ByteBuffer.allocate(4).putInt(num).array()")
            .build()
    }

    private fun longToBinFun():FunSpec {
        return FunSpec.builder("longToBin")
            .addParameter("num", Long::class)
            .returns(ByteArray::class)
            .addStatement("return ByteBuffer.allocate(8).putLong(num).array()")
            .build()
    }

    private fun floatToBinFun():FunSpec {
        return FunSpec.builder("floatToBin")
            .addParameter("num", Float::class)
            .returns(ByteArray::class)
            .addStatement("return ByteBuffer.allocate(4).putFloat(num).array()")
            .build()
    }

    private fun doubleToBinFun():FunSpec {
        return FunSpec.builder("doubleToBin")
            .addParameter("num", Double::class)
            .returns(ByteArray::class)
            .addStatement("return ByteBuffer.allocate(8).putDouble(num).array()")
            .build()
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
            .addStatement("val result = arr[offset.get()] == 1.toByte()")
            .addStatement("offset.add(1)")
            .addStatement("return result")
            .build()
    }

    private fun binToShort():FunSpec {
        return FunSpec.builder("binToShort")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Short::class)
            .addStatement("val num = ByteBuffer.wrap(arr, offset.get(), 2).short")
            .addStatement("offset.add(2)")
            .addStatement("return num")
            .build()
    }

    private fun binToInt():FunSpec {

        return FunSpec.builder("binToInt")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Int::class)
            .addStatement("val num = ByteBuffer.wrap(arr, offset.get(), 4).int")
            .addStatement("offset.add(4)")
            .addStatement("return num")
            .build()
    }

    private fun binToLong():FunSpec {
        return FunSpec.builder("binToLong")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Long::class)
            .addStatement("val num = ByteBuffer.wrap(arr, offset.get(), 8).long")
            .addStatement("offset.add(8)")
            .addStatement("return num")
            .build()
    }

    private fun binToFloat():FunSpec {
        return FunSpec.builder("binToFloat")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Float::class)
            .addStatement("val num = ByteBuffer.wrap(arr, offset.get(), 4).float")
            .addStatement("offset.add(4)")
            .addStatement("return num")
            .build()
    }

    private fun binToDouble():FunSpec {
        return FunSpec.builder("binToDouble")
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", ClassName("", "Counter"))
            .returns(Double::class)
            .addStatement("val num = ByteBuffer.wrap(arr, offset.get(), 8).double")
            .addStatement("offset.add(8)")
            .addStatement("return num")
            .build()
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
            .addImport("java.nio", "ByteBuffer")
            .addType(
                TypeSpec.objectBuilder("DecodeUtils")
                    .addFunction(booleanToBinFun())
                    .addFunction(shortToBinFun())
                    .addFunction(intToBinFun())
                    .addFunction(longToBinFun())
                    .addFunction(floatToBinFun())
                    .addFunction(doubleToBinFun())
                    .addFunction(stringToBinFun())
                    .addFunction(binToBoolean())
                    .addFunction(binToShort())
                    .addFunction(binToInt())
                    .addFunction(binToLong())
                    .addFunction(binToFloat())
                    .addFunction(binToDouble())
                    .addFunction(binToString())
                    .addType(createCounter())
                    .build()
            ).build()
    }

}