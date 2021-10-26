package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.CodeBlock
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
        return type.getDecodeCode("this", "var0", prefix, postfix)
    }
}