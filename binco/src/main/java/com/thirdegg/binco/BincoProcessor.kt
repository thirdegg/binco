package com.thirdegg.binco

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.thirdegg.binco.codegen.*
import com.thirdegg.binco.entries.*
import java.io.File
import java.lang.IllegalStateException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic


@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(BincoProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedAnnotationTypes("*")
class BincoProcessor : AbstractProcessor() {

    private val messages = ArrayList<MessageEntry>()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if (generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can't find the target directory for generated Kotlin files."
            )
            return false
        }

        roundEnv.getElementsAnnotatedWithAny(setOf(Binco::class.java)).forEach { element ->

            if (element.kind == ElementKind.INTERFACE) {

                val id = element.getAnnotation(Binco::class.java).id
                val className = element.asType().toString()

                if (!element.modifiers.contains(Modifier.PUBLIC)) {
                    val message = "Class cannot be private: $className."
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                    return false
                }

                for (messageEntry in messages) {
                    if (messageEntry.id == id) {
                        val message =
                            "Duplicate ID class: " + className + ":" + id + " " + messageEntry.type.fullClassName + ":" + messageEntry.id
                        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                        return false
                    }
                }

                messages.add(InterfaceMessage(id, element.asType(), ArrayList()))
                return@forEach
            }

            if (element.kind == ElementKind.ENUM) {
                val id = element.getAnnotation(Binco::class.java).id
                val fullClassName = element.asType().toString()
                messages.add(EnumMessage(id, element.asType(), ArrayList()))
                return@forEach
            }

            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Only interface or enum supported @Binco annotation"
            )
            return false
        }

        roundEnv.getElementsAnnotatedWithAny(setOf(Binco.Field::class.java)).forEach { element ->

            if (element.kind == ElementKind.METHOD) {
                element as ExecutableElement
                val id = element.getAnnotation(Binco.Field::class.java).id
                val fullClassName = element.enclosingElement.asType().toString()
                val returnType = element.returnType.toString()
                val fieldName = element.simpleName.toString()

                messages.find {
                    it.type.fullClassName == fullClassName && it is InterfaceMessage
                }?.run {
                    this as InterfaceMessage
                    for (field in fields) {
                        if (field.id == id) {
                            val message = "Duplicate ID field: " + fullClassName + ":" + id + " " + field.id
                            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                            return false
                        }
                    }

                    val type = getType(element.returnType)
                    val field = InterfaceField(id, type, fieldName)

                    field.let { field ->
                        this.fields.add(field)
                    }

                }
                return@forEach
            }

            if (element.kind == ElementKind.ENUM_CONSTANT) {
                val id = element.getAnnotation(Binco.Field::class.java).id
                val fullClassName = element.enclosingElement.asType().toString()
                val fieldName = element.simpleName.toString()
                messages.find {
                    it.type.fullClassName == fullClassName && it is EnumMessage
                }?.run {
                    this as EnumMessage
                    fields.add(EnumField(id, fieldName))
                }
                return@forEach
            }

            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Only interface or enum supported @Binco.Field annotation"
            )
            return false
        }

        try {
            writeClass(UtilsGen().build())
            writeClass(InterfaceGen().build())
            messages.forEach {
                if (it is InterfaceMessage) {
                    writeClass(ClassGen(it, "", POSTFIX).build())
                }
            }
            writeClass(DecoderGen(messages, "", POSTFIX).build())
        } catch (e: Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, e.message)
            return false
        }

        return true
    }

    private fun getType(typeMirror:TypeMirror): Type {
        when {
            Type.isPrimitiveType(typeMirror) -> {
                return Type.PrimitiveType(typeMirror)
            }
            checkFieldIsEnum(typeMirror) -> {
                messages.find { it is EnumMessage && it.type.fullClassName == typeMirror.toString() }?.run {
                    this as EnumMessage
                    return Type.EnumType(typeMirror, this.fields)
                }
            }
            checkFieldIsList(typeMirror) -> {
                val argumentOfList = getGenericType(typeMirror)[0]
                val genericType = getType(argumentOfList)
                return Type.ListType(typeMirror, genericType)
            }
            else -> {
                messages.find { it is InterfaceMessage && it.type.fullClassName == typeMirror.toString() }?.run {
                    this as InterfaceMessage
                    return Type.MessageType(typeMirror)
                }
            }
        }
        throw IllegalStateException(typeMirror.toString())
    }

    private fun writeClass(fileSpec: FileSpec) {
        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val file = File(kaptKotlinGeneratedDir)
        fileSpec.writeTo(file)
    }

    private fun getGenericType(type: TypeMirror): ArrayList<TypeMirror> {
        val arguments = ArrayList<TypeMirror>()
        processingEnv.typeUtils.directSupertypes(type).forEach { typeMirror ->
            if (typeMirror is DeclaredType) {
                for (argument in typeMirror.typeArguments) {
                    arguments.add(argument)
                }
            }
        }
        return arguments
    }

    private fun checkFieldIsEnum(typeMirror: TypeMirror): Boolean {
        messages.find { it is EnumMessage && it.type.fullClassName == typeMirror.toString() }?.run {
            return true
        }
        return false
    }

    private fun checkFieldIsList(type: TypeMirror?) = checkFieldOfType(type, "java.util.List")

    private fun checkFieldIsMap(type: TypeMirror?) = checkFieldOfType(type, "Map") //TODO

    private fun checkFieldOfType(type: TypeMirror?, checkingType: String): Boolean {
        type?:return false
        if (type is DeclaredType) {
            if (type.asElement().toString() == checkingType) {
                return true
            }
        }
        return false
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        const val BINCO_PKG = "com.thirdegg.binco"
        const val POSTFIX = "Bin"
    }

}