package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.CodeBlock
import com.thirdegg.binco.entries.Type.Companion.getCorrectName
import javax.lang.model.type.TypeMirror

class EnumMessage(
    id: Int,
    typeMirror: TypeMirror,
    parentType: Type?,
    val enumConsts: ArrayList<EnumConst>
) : MessageEntry(
    id,
    Type.EnumType(typeMirror, parentType, enumConsts)
) {

    fun getSortedFields() = enumConsts.sortedBy { it.id }

    override fun getEncodeCode(): CodeBlock {
        return type.getEncodeCode("this")
    }

    override fun getDecodeCode(prefix: String, postfix: String):CodeBlock {
        val code = CodeBlock.builder()
        code.addStatement("var var0 = when (arr[offset.get()].toInt()) {")
        enumConsts.forEach {
            code.addStatement("${it.id} -> ${type.getCorrectName(prefix, postfix)}.${it.name}")
        }
        code.addStatement("else -> throw Exception(\"Not found enum value\")")
        code.addStatement("}")
        return code.build()
    }
}