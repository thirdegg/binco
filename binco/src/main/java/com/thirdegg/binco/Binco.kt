package com.thirdegg.binco

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Binco(val id:Int) {

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Field(val id:Int)

    annotation class Props {
        @Retention(AnnotationRetention.SOURCE)
        annotation class GenNameRule(val regex: String, val replace: String)

        @Retention(AnnotationRetention.SOURCE)
        annotation class MessagesIdType(val type: KClass<*>)

        @Retention(AnnotationRetention.SOURCE)
        annotation class ListSizeType(val type: KClass<*>)

        @Retention(AnnotationRetention.SOURCE)
        annotation class StringSizeType(val type: KClass<*>)
    }
}