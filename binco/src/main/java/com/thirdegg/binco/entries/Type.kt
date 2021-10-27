package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.thirdegg.binco.BincoProcessor
import java.lang.IllegalStateException
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

sealed class Type private constructor(
    typeMirror: TypeMirror,
    val parentType: Type?,
) {

    val fullClassName = typeMirror.toString()
    val type: Pair<TypeMirror, KClass<*>?> = Pair(typeMirror, types[fullClassName])

    val className = fullClassName.split(".").last()
    val pkg = fullClassName.replace(".${this.className}", "")

    val escapedClassName = fullClassName.split(".").joinToString("") {
        it[0].uppercaseChar() + it.drop(1)
    }

    companion object {

        private const val BOOL = "boolean"
        private const val BOOL_CLASS = "java.lang.Boolean"

        private const val BYTE = "byte"
        private const val BYTE_CLASS = "java.lang.Byte"

        private const val SHORT = "short"
        private const val SHORT_CLASS = "java.lang.Short"

        private const val INT = "int"
        private const val INT_CLASS = "java.lang.Integer"

        private const val LONG = "long"
        private const val LONG_CLASS = "java.lang.Long"

        private const val FLOAT = "float"
        private const val FLOAT_CLASS = "java.lang.Float"

        private const val DOUBLE = "double"
        private const val DOUBLE_CLASS = "java.lang.Double"

        private const val STRING = "java.lang.String"

        const val ANY_JAVA = "java.lang.Object"
        const val ANY_KT = "kotlin.Any"

        private val types = mapOf<String, KClass<*>>(
            BOOL to Boolean::class,
            BOOL_CLASS to Boolean::class,
            BYTE to Byte::class,
            BYTE_CLASS to Byte::class,
            SHORT to Short::class,
            SHORT_CLASS to Short::class,
            INT to Int::class,
            INT_CLASS to Int::class,
            LONG to Long::class,
            LONG_CLASS to Long::class,
            FLOAT to Float::class,
            FLOAT_CLASS to Float::class,
            DOUBLE to Double::class,
            DOUBLE_CLASS to Double::class,
            STRING to String::class
        )

        fun isPrimitiveType(type: TypeMirror): Boolean {
            val name = type.toString()
            return name == BOOL || name == BOOL_CLASS
                    || name == BYTE || name == BYTE_CLASS
                    || name == SHORT || name == SHORT_CLASS
                    || name == INT || name == INT_CLASS
                    || name == LONG || name == LONG_CLASS
                    || name == FLOAT || name == FLOAT_CLASS
                    || name == DOUBLE || name == DOUBLE_CLASS
                    || name == STRING
        }

        fun isAnyType(type: TypeMirror): Boolean {
            val name = type.toString()
            return name == ANY_JAVA || name == ANY_KT
        }

        fun getKotlinClassName(javaClassName: String): String {
            return javaClassName.replace("java.util.", "kotlin.collections.")
                .replace("java.lang.String", "kotlin.String")
        }

        fun Type.getCorrectName(prefix: String, postfix: String): TypeName {
            when (this) {
                is PrimitiveType -> {
                    return this.type.second!!.asClassName()
                }
                is AnyMessageType -> {
                    return ClassName(BincoProcessor.BINCO_PKG, "BincoInterface")
                }
                is EnumType -> {
                    return if (this.parentType != null) {
                        val parentClassName = parentType.getCorrectName(prefix, postfix) as ClassName
                        ClassName(parentClassName.packageName,"${parentClassName.simpleName}.${prefix}${this.className}${postfix}")
                    } else {
                        ClassName(this.pkg, "${prefix}${this.className}${postfix}")
                    }
                }
                is ListType -> {
                    val nameOfGeneric = this.genericType.getCorrectName(prefix, postfix)
                    val clearedType = (this.type.first as DeclaredType).asElement().toString()
                    val list = ClassName("", getKotlinClassName(clearedType))
                    return list.parameterizedBy(nameOfGeneric)
                }
                else -> {
                    return if (this.parentType != null) {
                        val parentClassName = parentType.getCorrectName(prefix, postfix) as ClassName
                        ClassName(parentClassName.packageName,"${parentClassName.simpleName}.${prefix}${this.className}${postfix}")
                    } else {
                        ClassName(this.pkg, "${prefix}${this.className}${postfix}")
                    }
                }
            }
        }

    }

    class PrimitiveType(typeMirror: TypeMirror, parentType: Type?) : Type(typeMirror, parentType) {

        private fun isBool() = fullClassName == BOOL || fullClassName == BOOL_CLASS
        private fun isByte() = fullClassName == BYTE || fullClassName == BYTE_CLASS
        private fun isShort() = fullClassName == SHORT || fullClassName == SHORT_CLASS
        private fun isInt() = fullClassName == INT || fullClassName == INT_CLASS
        private fun isLong() = fullClassName == LONG || fullClassName == LONG_CLASS
        private fun isFloat() = fullClassName == FLOAT || fullClassName == FLOAT_CLASS
        private fun isDouble() = fullClassName == DOUBLE || fullClassName == DOUBLE_CLASS
        private fun isString() = fullClassName == "java.lang.String"

        override fun getEncodeCode(varName: String): CodeBlock {
            when {
                isBool() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.boolToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isByte() -> {
                    return CodeBlock.builder()
                        .addStatement("data.add(${varName})")
                        .build()
                }
                isShort() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.shortToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isInt() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.intToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isLong() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.longToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isFloat() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.floatToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isDouble() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.doubleToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
                isString() -> {
                    return CodeBlock.builder()
                        .beginControlFlow("DecodeUtils.stringToBin(${varName}).forEach")
                        .addStatement("data.add(it)")
                        .endControlFlow()
                        .build()
                }
            }
            throw IllegalStateException()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            when {
                isBool() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToBool(arr, offset)")
                        .build()
                }
                isByte() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = arr[offset.get()]")
                        .addStatement("offset.add(1)")
                        .build()
                }
                isShort() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToShort(arr, offset)")
                        .build()
                }
                isInt() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToInt(arr, offset)")
                        .build()
                }
                isLong() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToLong(arr, offset)")
                        .build()
                }
                isFloat() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToFloat(arr, offset)")
                        .build()
                }
                isDouble() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToDouble(arr, offset)")
                        .build()
                }
                isString() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToString(arr, offset)")
                        .build()
                }
            }
            throw IllegalStateException()
        }
    }

    class MessageType(typeMirror: TypeMirror, parentType: Type?) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .beginControlFlow("${varName}.toBin().forEach")
                .addStatement("data.add(it)")
                .endControlFlow()
                .build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            return CodeBlock.builder()
                .addStatement("var ${varIteration} = fromBin${escapedClassName}(arr, offset)")
                .build()
        }
    }

    class EnumType(typeMirror: TypeMirror, parentType: Type?, private val fields: ArrayList<EnumConst>) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .beginControlFlow("${varName}.toBin().forEach")
                .addStatement("data.add(it)")
                .endControlFlow()
                .build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            val code = CodeBlock.builder()
            code.addStatement("var ${varIteration} = fromBin${escapedClassName}(arr, offset)")
            return code.build()
        }

    }

    class ListType(typeMirror: TypeMirror, parentType: Type?, val genericType: Type) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .beginControlFlow("DecodeUtils.intToBin(${varName}.size).forEach")
                .addStatement("data.add(it)")
                .endControlFlow()
                .beginControlFlow("${varName}.forEach")
                .addStatement("${genericType.getEncodeCode("it")}")
                .endControlFlow()
                .build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            val genericName = getKotlinClassName(genericType.getCorrectName(prefix, postfix).toString())
            return CodeBlock.builder()
                .addStatement("val size${varIteration} = DecodeUtils.binToInt(arr, offset)")
                .addStatement("var $varIteration = ArrayList<${genericName}>()")
                .beginControlFlow("for (i in 0 until size${varIteration})")
                .addStatement("${genericType.getDecodeCode(varName, "${varIteration}Item", prefix, postfix)}")
                .addStatement("${varIteration}.add(${varIteration}Item)")
                .endControlFlow()
                .build()
        }
    }

    class AnyMessageType(typeMirror: TypeMirror, parentType: Type?) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .beginControlFlow("${varName}.toMessage().forEach")
                .addStatement("data.add(it)")
                .endControlFlow()
                .build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            return CodeBlock.builder()
                .addStatement("var ${varIteration} = decode(arr, offset)")
                .build()
        }
    }

    abstract fun getEncodeCode(varName: String): CodeBlock
    abstract fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock

}