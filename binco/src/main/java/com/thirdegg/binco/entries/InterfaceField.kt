package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.CodeBlock

class InterfaceField(
    val id: Int,
    val type: Type,
    val getterMethodName: String
) {

    fun getFieldName(): String {
        return getterMethodName.replace("^get".toRegex(), "").replaceFirstChar(Char::lowercase)
    }

    fun getEncodeCode(): CodeBlock {
        return type.getEncodeCode("${getterMethodName}()")
    }

    fun getDecodeCode(varIteration: String, prefix: String, postfix: String): CodeBlock {
        return type.getDecodeCode("${getterMethodName}()", varIteration, prefix, postfix)
    }

}