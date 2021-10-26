package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor
import com.thirdegg.binco.entries.EnumMessage
import com.thirdegg.binco.entries.InterfaceMessage
import com.thirdegg.binco.entries.Type.Companion.getCorrectName

class ClassGen(
    private val message: InterfaceMessage,
    private val prefix: String,
    private val postfix: String
) {

    fun build(): FileSpec {
        return FileSpec.builder(message.type.pkg, "${prefix}${message.type.className}${postfix}")
            .addImport(BincoProcessor.BINCO_PKG, "DecodeUtils")
            .addType(makeTypeBody(message, prefix, postfix)).build()
    }

    companion object {

        private fun getStaticId(message: InterfaceMessage): TypeSpec {
            return TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.builder("BINCO_ID", Int::class)
                    .initializer("${message.id}")
                    .build())
                .build()
        }


        private fun getId(message: InterfaceMessage): FunSpec {
            return FunSpec.builder("getBincoId")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Int::class)
                .addStatement("return ${message.id}")
                .build()
        }

        private fun toBin(message: InterfaceMessage): FunSpec {
            return FunSpec.builder("toBin")
                .addModifiers(KModifier.OVERRIDE)
                .returns(ByteArray::class)
                .addStatement("val data = ArrayList<Byte>()")
                .addCode(message.getEncodeCode())
                .addStatement("return data.toByteArray()")
                .build()
        }

        private fun toMessage(message: InterfaceMessage): FunSpec {
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

        fun makeTypeBody(message: InterfaceMessage, prefix: String, postfix: String):TypeSpec {
            return TypeSpec.classBuilder("${prefix}${message.type.className}${postfix}")
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
                                .addCode(
                                    """
                                    return ${it.getFieldName()}
                                    
                                """.trimIndent()
                                ).returns(it.type.getCorrectName(prefix, postfix))
                                .build()
                        )
                    }
                    message.childs.forEach {
                        if (it is InterfaceMessage) {
                            addType(makeTypeBody(it, prefix, postfix))
                        }
                        if (it is EnumMessage) {
                            addType(EnumGen.makeTypeBody(it, prefix, postfix))
                        }
                    }
                }
                .addSuperinterface(ClassName(BincoProcessor.BINCO_PKG, "BincoInterface"))
                .addType(getStaticId(message))
                .addFunction(getId(message))
                .addFunction(toBin(message))
                .addFunction(toMessage(message))
                .build()
        }

    }

}