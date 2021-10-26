package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.CodeBlock
import javax.lang.model.type.TypeMirror

class InterfaceMessage(
    id: Int,
    fullClassName: TypeMirror,
    parentType: Type?,
    val fields: ArrayList<InterfaceField>
) : MessageEntry(
    id,
    Type.MessageType(fullClassName, parentType)
) {

    fun getSortedFields() = fields.sortedBy { it.id }

    override fun getEncodeCode(): CodeBlock {
        val code = CodeBlock.builder()
        fields.sortedBy { it.id }.forEach {
            code.add(it.getEncodeCode())
        }
        return code.build()
    }

    override fun getDecodeCode(prefix: String, postfix: String): CodeBlock {
        val code = CodeBlock.builder()
        fields.sortedBy { it.id }.forEachIndexed { i, item ->
            code.add(item.getDecodeCode("var${i}", prefix, postfix))
        }
        return code.build()
    }
}