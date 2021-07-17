package com.thirdegg.binco

import com.google.auto.service.AutoService
import java.io.File
import java.lang.Exception
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class) // For registering the service
@SupportedSourceVersion(SourceVersion.RELEASE_8) // to support Java 8
@SupportedOptions(BincoProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedAnnotationTypes("*")
class BincoProcessor: AbstractProcessor() {

    data class FieldEntry(val id: Int, val className: String, val returnType: String, val fieldName:String)
    data class MessageEntry(val id: Int, val className: String)

    val listOfMessages = ArrayList<MessageEntry>()
    val listOfFields = ArrayList<FieldEntry>()

    fun newMethod(messageEntry: MessageEntry): String {
        val fields = listOfFields.filter {
            it.className == messageEntry.className
        }.sortedBy {
            it.id
        }
        val toBinCode = toBinCode(fields)
        val fromBinCode = fromBinCode(fields)

        return """
            fun ${messageEntry.className}.toBin():ByteArray {
                val data = ArrayList<Byte>()
                ${toBinCode}
                return data.toByteArray()
            }
            
            fun ${messageEntry.className}.fromBin(data:ByteArray):${messageEntry.className} {
                val data = ArrayList<Byte>()
                ${fromBinCode}
                return data.toByteArray()
            }
            
            fun ${messageEntry.className}.toMessage():ByteArray {
                val data = ArrayList<Byte>()
                
                data.add((${messageEntry.id} shr 24).toByte())
                data.add((${messageEntry.id} shr 16).toByte())
                data.add((${messageEntry.id} shr 8).toByte())
                data.add(${messageEntry.id}.toByte())
                
                toBin().forEach {
                    data.add(it)
                }
                return data.toByteArray()
            }
            
        """.trimIndent()
    }

    fun toBinCode(fields: List<FieldEntry>): String {
        var code = ""
        fields.forEach { entry ->
            if (entry.returnType == INT) {
                code += """
                    data.add((this.${entry.fieldName} shr 24).toByte())
                    data.add((this.${entry.fieldName} shr 16).toByte())
                    data.add((this.${entry.fieldName} shr 8).toByte())
                    data.add(this.${entry.fieldName}.toByte())
                    
                """.trimIndent()
                return@forEach
            }

            if (entry.returnType == STRING) {
                code += """
                    this.${entry.fieldName}.toByteArray().apply {
                        data.add((size shr 24).toByte())
                        data.add((size shr 16).toByte())
                        data.add((size shr 8).toByte())
                        data.add(size.toByte())
                        forEach {
                            data.add(it)
                        }
                    }
                    
                """.trimIndent()
                return@forEach
            }

            listOfMessages.find { it.className == entry.returnType }?.let {
                code += """
                    ${entry.fieldName}.toBin().apply {
                        data.add((size shr 24).toByte())
                        data.add((size shr 16).toByte())
                        data.add((size shr 8).toByte())
                        data.add(size.toByte())
                        forEach {
                            data.add(it)
                        }
                    }
                    
                """.trimIndent()
            }?:throw Exception("Class not found "+ entry.returnType)

        }
        return code
    }

    fun fromBinCode(fields: List<FieldEntry>): String {
        var variable = 0
        var dataIteration = 0
        var code = ""
        fields.forEach { entry ->
            if (entry.returnType == INT) {
                code += """
                    var v${variable} = 0
                    v${variable} += data[${dataIteration++}] shr 24
                    v${variable} += data[${dataIteration++}] shr 16
                    v${variable} += data[${dataIteration++}] shr 8
                    v${variable} += data[${dataIteration++}]
                """.trimIndent()
                variable++
                return@forEach
            }

            if (entry.returnType == STRING) {
                code += """

                """.trimIndent()
                return@forEach
            }

            listOfMessages.find { it.className == entry.returnType }?.let {
                code += """
                    
                """.trimIndent()
            }?:throw Exception("Class not found "+ entry.returnType)

        }
        return code
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
        if(generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files.")
            return false
        }

        roundEnv.getElementsAnnotatedWithAny(setOf(Message::class.java, Field::class.java)).forEach { element ->

            if (element.kind == ElementKind.FIELD) {
                val id = element.getAnnotation(Field::class.java).id
                val className = element.enclosingElement.asType().toString()
                val returnType = element.asType().toString()
                val fieldName = element.simpleName.toString()

                for (field in listOfFields) {
                    if (field.id == id && field.className == className) {
                        val message = "Duplicate ID field: "+className+":"+id+" "+field.className+":"+field.id
                        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                        return false
                    }
                }

                listOfFields.add(FieldEntry(id, className, returnType, fieldName))
            }

            if (element.kind == ElementKind.CLASS) {
                val id = element.getAnnotation(Message::class.java).id
                val className = element.asType().toString()

                for (messageEntry in listOfMessages) {
                    if (messageEntry.id == id) {
                        val message = "Duplicate ID class: "+className+":"+id+" "+messageEntry.className+":"+messageEntry.id
                        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
                        return false
                    }
                }

                listOfMessages.add(MessageEntry(id, className))
            }
        }

        try {
            var code = """
                package com.thirdegg.binco.generated
                
            """.trimIndent()
            listOfMessages.forEach {
                code += newMethod(it)
            }

            generateClass("Utils", code)
        } catch (e:Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, e.message)
        }

        return true
    }

    private fun generateClass(className: String, fileContent: String){
        val fileName = "Generated_$className"

        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        val file = File(kaptKotlinGeneratedDir, "$fileName.kt")

        file.writeText(fileContent)
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        const val INT = "int"
        const val STRING = "java.lang.String"

    }

}