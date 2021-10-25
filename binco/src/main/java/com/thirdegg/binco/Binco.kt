package com.thirdegg.binco

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Binco(val id:Int) {

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Field(val id:Int)
}