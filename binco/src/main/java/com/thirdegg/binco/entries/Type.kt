package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import java.lang.IllegalStateException
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

sealed class Type private constructor(typeMirror: TypeMirror) {

    val fullClassName = typeMirror.toString()
    val type: Pair<TypeMirror, KClass<*>?> = Pair(typeMirror, types[fullClassName])

    val escapedClassName = fullClassName.split(".").joinToString("") {
        it[0].uppercaseChar() + it.drop(1)
    }

    val className = fullClassName.split(".").last()
    val pkg = fullClassName.replace(".${this.className}", "")

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

        fun getKotlinClassName(javaClassName:String): String {
            return javaClassName.replace("java.util.", "kotlin.collections.")
                .replace("java.lang.String", "kotlin.String")
        }

        fun Type.getCorrectName(prefix: String, postfix: String): TypeName {
            when (this) {
                is PrimitiveType -> {
                    return this.type.second!!.asClassName()
                }
                is EnumType -> {
                    return ClassName("", this.fullClassName)
                }
                is ListType -> {
                    val nameOfGeneric = this.genericType.getCorrectName(prefix, postfix)
                    val clearedType = (this.type.first as DeclaredType).asElement().toString()
                    val list = ClassName("", getKotlinClassName(clearedType))
                    return list.parameterizedBy(nameOfGeneric)
                }
                else -> {
                    return ClassName(this.pkg, "${prefix}${this.className}${postfix}")
                }
            }
        }

    }

    class PrimitiveType(typeMirror: TypeMirror):Type(typeMirror) {

        private fun isBool() = fullClassName == BOOL || fullClassName == BOOL_CLASS
        private fun isByte() = fullClassName == BYTE || fullClassName == BYTE_CLASS
        private fun isShort() = fullClassName == SHORT || fullClassName == SHORT_CLASS
        private fun isInt() = fullClassName == INT || fullClassName == INT_CLASS
        private fun isFloat() = fullClassName == FLOAT || fullClassName == FLOAT_CLASS
        private fun isDouble() = fullClassName == DOUBLE || fullClassName == DOUBLE_CLASS
        private fun isString() = fullClassName == "java.lang.String"

        override fun getEncodeCode(varName:String): String {
            when {
                isBool() -> {
                    return """
                        DecodeUtils.boolToBin(${varName}).forEach {
                            data.add(it)
                        }
                        
                    """.trimIndent()
                }
                isByte() -> {
                    return """
                        data.add(${varName})
                        
                    """.trimIndent()
                }
                isShort() -> {
                    return """
                        DecodeUtils.shortToBin(${varName}).forEach {
                            data.add(it)
                        }
                        
                    """.trimIndent()
                }
                isInt() -> {
                    return """
                        DecodeUtils.intToBin(${varName}).forEach {
                            data.add(it)
                        }
                        
                    """.trimIndent()
                }
                isFloat() -> {

                }
                isDouble() -> {

                }
                isString() -> {
                    return """
                        DecodeUtils.stringToBin(${varName}).forEach {
                            data.add(it)
                        }
                        
                    """.trimIndent()
                }
            }
            throw IllegalStateException()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): String {
            when {
                isBool() -> {
                    return """
                        var ${varIteration} = DecodeUtils.binToBool(arr, offset)
                        offset += 1
                        
                    """.trimIndent()
                }
                isByte() -> {

                }
                isShort() -> {
                    return """
                        var ${varIteration} = DecodeUtils.binToShort(arr, offset)
                        offset += 2
                        
                    """.trimIndent()
                }
                isInt() -> {
                    return """
                        var ${varIteration} = DecodeUtils.binToInt(arr, offset)
                        offset += 4
                        
                    """.trimIndent()
                }
                isFloat() -> {

                }
                isDouble() -> {

                }
                isString() -> {
                    return """
                        val size${varIteration} = DecodeUtils.binToInt(arr, offset)
                        offset += 4
                        var ${varIteration} = DecodeUtils.binToString(arr, offset, size${varIteration})
                        offset += size${varIteration}
                        
                    """.trimIndent()
                }
            }
            throw IllegalStateException()
        }
    }

    class MessageType(typeMirror: TypeMirror):Type(typeMirror) {
        override fun getEncodeCode(varName: String): String {
            return """
                ${varName}.toBin().apply {
                    DecodeUtils.intToBin(size).forEach {
                        data.add(it)
                    }
                    forEach {
                        data.add(it)
                    }
                }
                
            """.trimIndent()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): String {
            return """
                val size${varIteration} = DecodeUtils.binToInt(arr, offset)
                offset += 4
                var ${varIteration} = fromBin${escapedClassName}(arr, offset)
                offset += size${varIteration}
                
            """.trimIndent()
        }

    }
    class EnumType(typeMirror: TypeMirror, private val fields: ArrayList<EnumField>):Type(typeMirror) {
        override fun getEncodeCode(varName: String): String {
            var code = """
                when (${varName}) {
                    
            """.trimIndent()

            fields.forEach {
                code += """
                    ${fullClassName}.${it.name} -> { data.add(${it.id}) }
                    
                """.trimIndent()
            }

            code += """
                    else -> throw Exception("Not found enum value")
                }
                
            """.trimIndent()
            return code
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): String {
            var code = """
                var ${varIteration} = when (arr[offset].toInt()) {
                
            """.trimIndent()

            fields.forEach {
                code += """
                        ${it.id} -> ${fullClassName}.${it.name}
                    
                """.trimIndent()
            }

            code += """
                else -> throw Exception("Not found enum value")
            }
                
            offset += 1
                
            """.trimIndent()
            return code
        }

    }

    class ListType(typeMirror: TypeMirror, val genericType: Type):Type(typeMirror) {
        override fun getEncodeCode(varName: String): String {
            return """
                DecodeUtils.intToBin(${varName}.size).forEach {
                    data.add(it)
                }
                
                ${varName}.forEach { item ->
                   ${genericType.getEncodeCode("item")}
                }
                
            """.trimIndent()
        }

        override fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String): String {
            return """
                
                val size${varIteration} = DecodeUtils.binToInt(arr, offset)
                offset += 4
                
                var $varIteration = ArrayList<${getKotlinClassName(genericType.getCorrectName(prefix, postfix).toString())}>()
                for (i in 0 until size${varIteration}) {
                   ${genericType.getDecodeCode(varName, "${varIteration}Temp",prefix,postfix)}
                   ${varIteration}.add(${varIteration}Temp)
                }
                
            """.trimIndent()
        }

    }

    abstract fun getEncodeCode(varName:String):String
    abstract fun getDecodeCode(varName: String, varIteration: String, prefix: String, postfix: String):String

}