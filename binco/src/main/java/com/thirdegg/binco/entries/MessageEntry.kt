package com.thirdegg.binco.entries

sealed class MessageEntry(
    val id:Int,
    val type: Type
)