package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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

        private const val FLOAT = "float"
        private const val FLOAT_CLASS = "java.lang.Float"

        private const val DOUBLE = "double"
        private const val DOUBLE_CLASS = "java.lang.Double"

        private const val STRING = "java.lang.String"

        private val types = mapOf<String, KClass<*>>(
            BOOL to Boolean::class,
            BOOL_CLASS to Boolean::class,
            BYTE to Byte::class,
            BYTE_CLASS to Byte::class,
            SHORT to Short::class,
            SHORT_CLASS to Short::class,
            INT to Int::class,
            INT_CLASS to Int::class,
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
                    || name == FLOAT || name == FLOAT_CLASS
                    || name == DOUBLE || name == DOUBLE_CLASS
                    || name == STRING
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
        private fun isFloat() = fullClassName == FLOAT || fullClassName == FLOAT_CLASS
        private fun isDouble() = fullClassName == DOUBLE || fullClassName == DOUBLE_CLASS
        private fun isString() = fullClassName == "java.lang.String"

        override fun getEncodeCode(varName: String): CodeBlock {
            when {
                isBool() -> {
                    return CodeBlock.builder()
                        .addStatement("DecodeUtils.boolToBin(${varName}).forEach {")
                        .addStatement("data.add(it)")
                        .addStatement("}")
                        .build()
                }
                isByte() -> {
                    return CodeBlock.builder()
                        .addStatement("data.add(${varName})")
                        .build()
                }
                isShort() -> {
                    return CodeBlock.builder()
                        .addStatement("DecodeUtils.shortToBin(${varName}).forEach {")
                        .addStatement("data.add(it)")
                        .addStatement("}")
                        .build()
                }
                isInt() -> {
                    return CodeBlock.builder()
                        .addStatement("DecodeUtils.intToBin(${varName}).forEach {")
                        .addStatement("data.add(it)")
                        .addStatement("}")
                        .build()
                }
                isFloat() -> {

                }
                isDouble() -> {

                }
                isString() -> {
                    return CodeBlock.builder()
                        .addStatement("DecodeUtils.stringToBin(${varName}).forEach {")
                        .addStatement("data.add(it)")
                        .addStatement("}")
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
                        .addStatement("offset += 1")
                        .build()
                }
                isByte() -> {

                }
                isShort() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToShort(arr, offset)")
                        .addStatement("offset += 2")
                        .build()
                }
                isInt() -> {
                    return CodeBlock.builder()
                        .addStatement("var ${varIteration} = DecodeUtils.binToInt(arr, offset)")
                        .addStatement("offset += 4")
                        .build()
                }
                isFloat() -> {

                }
                isDouble() -> {

                }
                isString() -> {
                    return CodeBlock.builder()
                        .addStatement("val size${varIteration} = DecodeUtils.binToInt(arr, offset)")
                        .addStatement("offset += 4")
                        .addStatement("var ${varIteration} = DecodeUtils.binToString(arr, offset, size${varIteration})")
                        .addStatement("offset += size${varIteration}")
                        .build()
                }
            }
            throw IllegalStateException()
        }
    }

    class MessageType(typeMirror: TypeMirror, parentType: Type?) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .addStatement("""
                    ${varName}.toBin().apply {
                        DecodeUtils.intToBin(size).forEach {
                            data.add(it)
                        }
                        forEach {
                            data.add(it)
                        }
                    }
                    
                """.trimIndent())
                .build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            return CodeBlock.builder()
                .addStatement("val size${varIteration} = DecodeUtils.binToInt(arr, offset)")
                .addStatement("offset += 4")
                .addStatement("var ${varIteration} = fromBin${escapedClassName}(arr, offset)")
                .addStatement("offset += size${varIteration}")
                .build()
        }

    }

    class EnumType(typeMirror: TypeMirror, parentType: Type?, private val fields: ArrayList<EnumConst>) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder().addStatement(
                """
                    ${varName}.toBin().apply {
                        forEach {
                            data.add(it)
                        }
                    }
                    
                """.trimIndent()
            ).build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            val code = CodeBlock.builder()
            code.addStatement("var ${varIteration} = when (arr[offset].toInt()) {")
            fields.forEach {
                code.addStatement("${it.id} -> ${getCorrectName(prefix, postfix)}.${it.name}")
            }
            code.addStatement("""else -> throw Exception("Not found enum value")""")
            code.addStatement("}")
            code.addStatement("offset += 1")
            return code.build()
        }

    }

    class ListType(typeMirror: TypeMirror, parentType: Type?, val genericType: Type) : Type(typeMirror, parentType) {
        override fun getEncodeCode(varName: String): CodeBlock {
            return CodeBlock.builder()
                .addStatement("""
                    DecodeUtils.intToBin(${varName}.size).forEach {
                        data.add(it)
                    }
                    ${varName}.forEach { item ->
                       ${genericType.getEncodeCode("item")}
                    }
                """.trimIndent()).build()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock {
            val genericName = getKotlinClassName(genericType.getCorrectName(prefix, postfix).toString())
            return CodeBlock.builder().addStatement("""
                val size${varIteration} = DecodeUtils.binToInt(arr, offset)
                offset += 4
                
                var $varIteration = ArrayList<${genericName}>()
                for (i in 0 until size${varIteration}) {
                   ${genericType.getDecodeCode(varName, "${varIteration}Temp", prefix, postfix)}
                   ${varIteration}.add(${varIteration}Temp)
                }
               
            """.trimIndent()).build()
        }

    }

    abstract fun getEncodeCode(varName: String): CodeBlock
    abstract fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): CodeBlock

}