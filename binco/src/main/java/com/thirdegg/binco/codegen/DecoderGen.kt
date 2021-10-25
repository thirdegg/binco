package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor
import com.thirdegg.binco.entries.EnumMessage
import com.thirdegg.binco.entries.InterfaceMessage
import com.thirdegg.binco.entries.MessageEntry

class DecoderGen(
    private val messages: List<MessageEntry>,
    private val prefix: String,
    private val postfix: String
) {

    private fun fromBin(message: InterfaceMessage):FunSpec {
        return FunSpec.builder("fromBin${message.type.escapedClassName}")
            .addModifiers(KModifier.PRIVATE)
            .addParameter("arr", ByteArray::class)
            .addParameter("offset", Int::class)
            .returns(ClassName(message.type.pkg, "${prefix}${message.type.className}${postfix}"))
            .addStatement("var offset = offset")
            .addCode(message.getDecodeCode(prefix, postfix))
            .addStatement("return ${prefix}${message.type.className}${postfix}(${message.fields.mapIndexed { i, _ -> "var$i" }.joinToString(",")})")
            .build()
    }

    fun build(): FileSpec {
        return FileSpec.builder(BincoProcessor.BINCO_PKG, "BincoDecoder")
            .addImport(BincoProcessor.BINCO_PKG,"DecodeUtils")
            .addType(
                TypeSpec.objectBuilder("BincoDecoder").apply {
                    messages.forEach {
                        if (it is InterfaceMessage) {
                            addFunction(fromBin(it))
                        }
                    }
                }.addFunction(
                        FunSpec.builder("decode")
                            .addParameter("arr", ByteArray::class)
                            .returns(ClassName(BincoProcessor.BINCO_PKG, "BincoInterface"))
                            .addCode("""
                                var offset = 0
                                val messageId = DecodeUtils.binToShort(arr, offset).toInt()
                                offset  += 2
                                when (messageId) {
                                
                            """.trimIndent())
                            .apply {
                                messages.forEach {
                                    when (it) {
                                        is InterfaceMessage -> {
                                            addCode("""
                                                ${it.id} -> {
                                                    return fromBin${it.type.escapedClassName}(arr, offset)
                                                }
                                                
                                            """.trimIndent())
                                        }
                                        is EnumMessage -> {

                                        }
                                    }
                                }
                            }.addCode("""
                                    else -> throw Exception("Message not found")
                                }
                            """.trimIndent())
                            .build()
                    )
                    .build()
            ).build()
    }
}