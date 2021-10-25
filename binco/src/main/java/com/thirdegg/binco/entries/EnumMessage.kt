package com.thirdegg.binco.entries

import javax.lang.model.type.TypeMirror

class EnumMessage(
    id: Int,
    fullClassName: TypeMirror,
    val fields: ArrayList<EnumField>
): MessageEntry(
    id,
    Type.EnumType(fullClassName, fields)
) {

    fun getSortedFields() = fields.sortedBy { it.id }

}