package com.thirdegg.binco.entries

class InterfaceField(
    val id: Int,
    val type: Type,
    val getterMethodName: String
) {

    fun getFieldName(): String {
        return getterMethodName.replace("^get".toRegex(), "").replaceFirstChar(Char::lowercase)
    }

    fun getEncodeCode(): String {
        return type.getEncodeCode("${getterMethodName}()")
    }

    fun getDecodeCode(varIteration: String, prefix: String, postfix: String): String {
        return type.getDecodeCode("${getterMethodName}()", varIteration, prefix, postfix)
    }

}