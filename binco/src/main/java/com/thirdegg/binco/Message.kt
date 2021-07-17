package com.thirdegg.binco

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Message(val id:Int, val priority: Int = 0)