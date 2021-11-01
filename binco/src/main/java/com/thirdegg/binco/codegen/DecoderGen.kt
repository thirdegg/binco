package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor
import com.thirdegg.binco.entries.EnumMessage
import com.thirdegg.binco.entries.InterfaceMessage
import com.thirdegg.binco.entries.MessageEntry
import com.thirdegg.binco.entries.Type.Companion.getCorrectName
import java.lang.IllegalStateException

class DecoderGen(
    private val messages: List<MessageEntry>,
    private val prefix: String,
    private val postfix: String
) {

    private fun fromBin(message: MessageEntry):FunSpec {
        when (message) {
            is InterfaceMessage -> {
                return FunSpec.builder("fromBin${message.type.escapedClassName}")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("arr", ByteArray::class)
                    .addParameter("offset", ClassName(BincoProcessor.BINCO_PKG, "DecodeUtils.Counter"))
                    .returns(message.type.getCorrectName(prefix, postfix))
                    .addCode(message.getDecodeCode(prefix, postfix))
                    .addStatement("return ${prefix}${message.type.className}${postfix}(")
                    .addStatement(message.fields.mapIndexed { i, _ -> "var$i" }.joinToString(","))
                    .addStatement(")")
                    .build()
            }
            is EnumMessage -> {
                return FunSpec.builder("fromBin${message.type.escapedClassName}")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("arr", ByteArray::class)
                    .addParameter("offset", ClassName(BincoProcessor.BINCO_PKG, "DecodeUtils.Counter"))
                    .returns(message.type.getCorrectName(prefix, postfix))
                    .addCode(message.getDecodeCode(prefix, postfix))
                    .addStatement("offset.add(1)")
                    .addStatement("return var0")
                    .build()
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    private fun collectDecodeMessageCasesList(messages:List<MessageEntry>):List<String> {
        val list = ArrayList<String>()
        messages.forEach {
            list.addAll(collectDecodeMessageCasesList(it.childs))
            list.add("${it.id} -> {return fromBin${it.type.escapedClassName}(arr, offset)}")
        }
        return list
    }

    private fun collectDecodeFuncs(messages:List<MessageEntry>):List<FunSpec> {
        val list = ArrayList<FunSpec>()
        messages.forEach {
            list.addAll(collectDecodeFuncs(it.childs))
            list.add(fromBin(it))
        }
        return list
    }

    fun build(): FileSpec {
        return FileSpec.builder(BincoProcessor.BINCO_PKG, "BincoDecoder")
            .addImport(BincoProcessor.BINCO_PKG,"DecodeUtils")
            .addType(
                TypeSpec.objectBuilder("BincoDecoder").apply {
                    collectDecodeFuncs(messages).forEach {
                        addFunction(it)
                    }
                }.addFunction(
                        FunSpec.builder("decode")
                            .addParameter("arr", ByteArray::class)
                            .addParameter(
                                ParameterSpec
                                    .builder("offset", ClassName(BincoProcessor.BINCO_PKG, "DecodeUtils.Counter"))
                                    .defaultValue("DecodeUtils.Counter(0)")
                                    .build()
                            )
                            .returns(ClassName(BincoProcessor.BINCO_PKG, "BincoInterface"))
                            .addStatement("val messageId = DecodeUtils.binToShort(arr, offset).toInt()")
                            .beginControlFlow("when (messageId)")
                            .apply {
                                collectDecodeMessageCasesList(messages).forEach {
                                    addStatement(it)
                                }
                            }
                            .addStatement("else -> throw Exception(\"Message not found\")")
                            .endControlFlow()
                            .build()
                    ).build()
            ).build()
    }
}