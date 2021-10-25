package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor
import com.thirdegg.binco.entries.InterfaceMessage
import com.thirdegg.binco.entries.Type.Companion.getCorrectName

class ClassGen(
    private val message: InterfaceMessage,
    private val prefix: String,
    private val postfix: String
) {

    private fun getId(): FunSpec {
        return FunSpec.builder("getBincoId")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Int::class)
            .addStatement("return ${message.id}")
            .build()
    }

    private fun toBin(): FunSpec {
        return FunSpec.builder("toBin")
            .addModifiers(KModifier.OVERRIDE)
            .returns(ByteArray::class)
            .addStatement("val data = ArrayList<Byte>()")
            .addCode(message.getEncodeCode())
            .addStatement("return data.toByteArray()")
            .build()
    }

    private fun toMessage(): FunSpec {
        return FunSpec.builder("toMessage")
            .addModifiers(KModifier.OVERRIDE)
            .returns(ByteArray::class)
            .addStatement("val data = ArrayList<Byte>()")
            .addStatement(
                """
                DecodeUtils.shortToBin(${message.id}.toShort()).forEach {
                    data.add(it)
                }
                
                toBin().forEach {
                    data.add(it)
                }
                
                return data.toByteArray()
                
            """.trimIndent()
            )
            .build()
    }

    fun build(): FileSpec {
        return FileSpec.builder(message.type.pkg, "${prefix}${message.type.className}${postfix}")
            .addImport(BincoProcessor.BINCO_PKG, "DecodeUtils")
            .addType(
                TypeSpec.classBuilder("${prefix}${message.type.className}${postfix}")
                    .primaryConstructor(
                        FunSpec.constructorBuilder().apply {
                            message.getSortedFields().forEach {
                                addParameter(it.getFieldName(), it.type.getCorrectName(prefix, postfix))
                            }
                        }.build()
                    )
                    .apply {
                        message.getSortedFields().forEach {
                            addProperty(
                                PropertySpec.builder(it.getFieldName(), it.type.getCorrectName(prefix, postfix))
                                .initializer(it.getFieldName())
                                .addModifiers(KModifier.PRIVATE)
                                .build()
                            )
                            addFunction(
                                FunSpec.builder(it.getterMethodName)
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addCode(
                                        """
                                            return ${it.getFieldName()}
                                            
                                        """.trimIndent()
                                    ).returns(it.type.getCorrectName(prefix, postfix))
                                    .build()
                            )
                        }
                    }
                    .addSuperinterface(ClassName(message.type.pkg, message.type.className))
                    .addSuperinterface(ClassName(BincoProcessor.BINCO_PKG, "BincoInterface"))
                    .addFunction(getId())
                    .addFunction(toBin())
                    .addFunction(toMessage())
                    .build()
            ).build()
    }

}