package com.thirdegg.binco.codegen

import com.squareup.kotlinpoet.*
import com.thirdegg.binco.BincoProcessor

class InterfaceGen {

    fun build(): FileSpec {
        return FileSpec.builder(BincoProcessor.BINCO_PKG, "BincoInterface")
            .addType(
                TypeSpec.interfaceBuilder("BincoInterface")
                    .addFunction(
                        FunSpec.builder("toBin")
                            .addModifiers(KModifier.ABSTRACT)
                            .returns(ByteArray::class)
                            .build())
                    .addFunction(
                        FunSpec.builder("toMessage")
                            .addModifiers(KModifier.ABSTRACT)
                            .returns(ByteArray::class)
                            .build())
                    .addFunction(
                        FunSpec.builder("getBincoId")
                            .addModifiers(KModifier.ABSTRACT)
                            .returns(Int::class)
                            .build()
                    )
                    .build()
            ).build()
    }

}