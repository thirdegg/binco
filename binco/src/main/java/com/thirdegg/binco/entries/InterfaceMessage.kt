package com.thirdegg.binco.entries

import javax.lang.model.type.TypeMirror

class InterfaceMessage(
    id: Int,
    fullClassName: TypeMirror,
    val fields:ArrayList<InterfaceField>
): MessageEntry(
    id,
    Type.MessageType(fullClassName)
) {

    fun getSortedFields() = fields.sortedBy { it.id }

    fun getEncodeCode(): String {
        var code = ""
        fields.sortedBy { it.id }.forEach {
            code += it.getEncodeCode()
        }
        return code
    }

    fun getDecodeCode(prefix: String, postfix: String): String {
        var code = ""
        fields.sortedBy { it.id }.forEachIndexed { i, item ->
            code += item.getDecodeCode("var${i}", prefix, postfix)
        }
        return code
    }
}