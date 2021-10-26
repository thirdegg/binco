package com.thirdegg.binco.entries

import com.squareup.kotlinpoet.CodeBlock

sealed class MessageEntry(
    val id: Int,
    val type: Type
) {
    val childs = ArrayList<MessageEntry>()

    abstract fun getEncodeCode(): CodeBlock
    abstract fun getDecodeCode(prefix: String, postfix: String): CodeBlock
}