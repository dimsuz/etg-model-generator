package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.ModelOperations
import com.github.dimsuz.modelgenerator.processor.util.Either
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

internal fun generateModelOperations(
  processingEnv: ProcessingEnvironment,
  reactiveModelElement: TypeElement,
  reactiveProperties: List<ReactiveProperty>
): Either<String, Unit> {
  val modelName = reactiveModelElement.simpleName.toString()
  val className = modelName + "Operations"
  val fileSpec = FileSpec
    .builder(reactiveModelElement.enclosingPackageName, "$className.kt")
    .addType(TypeSpec.classBuilder(className)
      .addSuperinterface(
        ModelOperations::class.asClassName().parameterizedBy(reactiveModelElement.asType().asTypeName())
      )
      .addModifiers(KModifier.INTERNAL)
      .build())
    .build()
  return writeFile(processingEnv, fileSpec)
}
