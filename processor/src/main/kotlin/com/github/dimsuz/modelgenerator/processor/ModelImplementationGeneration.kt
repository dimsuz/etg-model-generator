package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.util.enclosedMethods
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.overridingWrapper
import com.github.dimsuz.modelgenerator.processor.util.primaryConstructor
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

internal fun generateModelImplementation(
  processingEnv: ProcessingEnvironment,
  reactiveModelElement: TypeElement,
  reactiveProperties: List<ReactiveProperty>,
  operations: ClassName
): Either<String, Unit> {
  val modelName = reactiveModelElement.simpleName.toString()
  val className = modelName + "Impl"
  val fileSpec = FileSpec
    .builder(reactiveModelElement.enclosingPackageName, "$className.kt")
    .addType(
      TypeSpec.classBuilder(className)
        .primaryConstructor(PropertySpec.builder("operations", operations).addModifiers(KModifier.PRIVATE).build())
        .addSuperinterface(reactiveModelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(reactiveModelElement.enclosedMethods.map { FunSpec.overridingWrapper(it).build() })
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}
