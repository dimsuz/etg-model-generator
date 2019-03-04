package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.ModelOperations
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.ClassName
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
): Either<String, ClassName> {
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
  return writeFile(processingEnv, fileSpec).map { ClassName(fileSpec.packageName, className) }
}
