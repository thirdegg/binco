package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor
import com.thirdegg.binco.entries.EnumMessage
import com.thirdegg.binco.entries.InterfaceMessage
import com.thirdegg.binco.entries.Type.Companion.getCorrectName

class EnumGen(
    private val message: EnumMessage,
    private val prefix: String,
    private val postfix: String
) {


    fun build(): FileSpec {
        return FileSpec.builder(message.type.pkg, "${prefix}${message.type.className}${postfix}")
            .addImport(BincoProcessor.BINCO_PKG, "DecodeUtils")
            .addType(makeTypeBody(message, prefix, postfix))
            .build()
    }

    companion object {

        private fun getStaticId(message: EnumMessage): TypeSpec {
            return TypeSpec.companionObjectBuilder()
                .addProperty(PropertySpec.builder("BINCO_ID", Int::class)
                    .initializer("${message.id}")
                    .build())
                .build()
        }

        private fun getId(message: EnumMessage): FunSpec {
            return FunSpec.builder("getBincoId")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Int::class)
                .addStatement("return ${message.id}")
                .build()
        }

        private fun toBin(message: EnumMessage, prefix: String, postfix: String): FunSpec {
            var code = """
            when (this) {
                
        """.trimIndent()

            message.enumConsts.forEach {
                code += """
                    ${message.type.getCorrectName(prefix, postfix)}.${it.name} -> { data.add(${it.id}) }
                    
                """.trimIndent()
            }

            code += """
                else -> throw Exception("Not found enum value")
            }
            
        """.trimIndent()

            return FunSpec.builder("toBin")
                .addModifiers(KModifier.OVERRIDE)
                .returns(ByteArray::class)
                .addStatement("val data = ArrayList<Byte>()")
                .addCode(code)
                .addStatement("return data.toByteArray()")
                .build()


        }

        private fun toMessage(message: EnumMessage): FunSpec {
            return FunSpec.builder("toMessage")
                .addModifiers(KModifier.OVERRIDE)
                .returns(ByteArray::class)
                .addStatement("val data = ArrayList<Byte>()")
                .addStatement("""
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

        fun makeTypeBody(message: EnumMessage, prefix: String, postfix: String):TypeSpec {
            return TypeSpec.enumBuilder("${prefix}${message.type.className}${postfix}")
                .addSuperinterface(ClassName(BincoProcessor.BINCO_PKG, "BincoInterface"))
                .apply {
                    message.getSortedFields().forEach {
                        addEnumConstant(it.name)
                    }
                    message.childs.forEach {
                        if (it is InterfaceMessage) {
                            addType(ClassGen.makeTypeBody(it, prefix, postfix))
                        }
                        if (it is EnumMessage) {
                            addType(makeTypeBody(it, prefix, postfix))
                        }
                    }
                }
                .addType(getStaticId(message))
                .addFunction(getId(message))
                .addFunction(toBin(message, prefix, postfix))
                .addFunction(toMessage(message))
                .build()
        }
    }

}