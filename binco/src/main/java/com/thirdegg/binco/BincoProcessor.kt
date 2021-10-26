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
import javax.lang.model.type.TypeKind
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

                if (element.enclosingElement.asType().kind == TypeKind.PACKAGE) {
                    messages.add(InterfaceMessage(id, element.asType(), null, ArrayList()))
                } else if (element.enclosingElement.asType().kind == TypeKind.DECLARED) {
                    val parentClassName = element.enclosingElement.asType().toString()
                    val parent = findMessage(parentClassName, messages)
                    parent?.childs?.add(InterfaceMessage(id, element.asType(), parent.type, ArrayList()))?:throw Exception("Parent not found")
                }
                return@forEach
            }

            if (element.kind == ElementKind.ENUM) {
                val id = element.getAnnotation(Binco::class.java).id
                if (element.enclosingElement.asType().kind == TypeKind.PACKAGE) {
                    messages.add(EnumMessage(id, element.asType(), null, ArrayList()))
                } else if (element.enclosingElement.asType().kind == TypeKind.DECLARED) {
                    val parentClassName = element.enclosingElement.asType().toString()
                    val parent = findMessage(parentClassName, messages)
                    parent?.childs?.add(EnumMessage(id, element.asType(), parent.type, ArrayList()))?:throw Exception("Parent not found")
                }

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
                val fieldName = element.simpleName.toString()

                val message = findMessage(fullClassName, messages)
                if (message is InterfaceMessage) {
                    for (field in message.fields) {
                        if (field.id == id) {
                            val message = "Duplicate ID field: " + fullClassName + ":" + id + " " + field.id
                            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                            return false
                        }
                    }

                    val type = getType(element.returnType)
                    val field = InterfaceField(id, type, fieldName)

                    field.let { field ->
                        message.fields.add(field)
                    }

                }
                return@forEach
            }

            if (element.kind == ElementKind.ENUM_CONSTANT) {
                val id = element.getAnnotation(Binco.Field::class.java).id
                val fullClassName = element.enclosingElement.asType().toString()
                val fieldName = element.simpleName.toString()
                val message = findMessage(fullClassName, messages)
                if (message is EnumMessage) {
                    message.enumConsts.add(EnumConst(id, fieldName))
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
                if (it is EnumMessage) {
                    writeClass(EnumGen(it, "", POSTFIX).build())
                }
            }
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
                return Type.PrimitiveType(typeMirror, null)
            }
            checkFieldIsEnum(typeMirror) -> {
                val message = findMessage(typeMirror.toString(), messages)
                if (message is EnumMessage) {
                    return Type.EnumType(typeMirror, message.type.parentType, message.enumConsts)
                }
            }
            checkFieldIsList(typeMirror) -> {
                val argumentOfList = getGenericType(typeMirror)[0]
                val genericType = getType(argumentOfList)
                return Type.ListType(typeMirror, genericType.parentType, genericType)
            }
            else -> {
                val message = findMessage(typeMirror.toString(), messages)
                if (message is InterfaceMessage) {
                    return Type.MessageType(typeMirror, message.type.parentType)
                }
            }
        }
        throw IllegalStateException("Type not found $typeMirror")
    }

    private fun findMessage(className:String, messages:ArrayList<MessageEntry>):MessageEntry? {
        messages.forEach {
            val inChild = findMessage(className, it.childs)
            if (inChild != null) return inChild
            if (it.type.fullClassName == className) {
                return it
            }
        }
        return null
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
        findMessage(typeMirror.toString(), messages)?.run {
            if (this is EnumMessage) return true
            return false
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