package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.overridingWrapper
import com.github.dimsuz.modelgenerator.processor.util.primaryConstructor
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

internal fun generateModelImplementation(
  processingEnv: ProcessingEnvironment,
  modelDescription: ReactiveModelDescription,
  operations: ClassName
): Either<String, Unit> {
  val modelElement = modelDescription.modelElement
  val modelName = modelElement.simpleName.toString()
  val className = modelName + "Impl"
  val fileSpec = FileSpec
    .builder(modelElement.enclosingPackageName, "$className.kt")
    .addType(
      TypeSpec.classBuilder(className)
        .primaryConstructor(
          PropertySpec.builder(
            OPERATIONS_PROPERTY_NAME,
            operations
          ).addModifiers(KModifier.PRIVATE).build()
        )
        .addSuperinterface(modelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}

fun generateNonReactiveMethodImpl(element: ExecutableElement): FunSpec {
  return FunSpec.overridingWrapper(element)
    .addCode(
      CodeBlock.builder()
        .addStatement(
          "return $OPERATIONS_PROPERTY_NAME.%N(%N)",
          element.simpleName.toString(),
          element.parameters.joinToString(", ") { it.simpleName.toString() })
        .build()
    )
    .build()
}

private const val OPERATIONS_PROPERTY_NAME = "operations"
