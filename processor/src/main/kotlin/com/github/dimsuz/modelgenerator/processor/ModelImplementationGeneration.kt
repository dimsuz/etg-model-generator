package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
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
        .primaryConstructor(PropertySpec.builder("operations", operations).addModifiers(KModifier.PRIVATE).build())
        .addSuperinterface(modelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(modelElement.enclosedMethods.map { FunSpec.overridingWrapper(it).build() })
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}
