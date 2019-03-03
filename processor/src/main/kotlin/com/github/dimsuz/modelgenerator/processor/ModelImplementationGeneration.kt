package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.util.Either
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement

internal fun generateModelImplementation(
  processingEnv: ProcessingEnvironment,
  reactiveModelElement: TypeElement,
  reactiveProperties: List<ReactiveProperty>
): Either<String, Unit> {
  val modelName = reactiveModelElement.simpleName.toString()
  val className = modelName + "Impl"
  val fileSpec = FileSpec
    .builder(reactiveModelElement.enclosingPackageName, "$className.kt")
    .addType(
      TypeSpec.classBuilder(className)
        .addSuperinterface(reactiveModelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(reactiveProperties.map { FunSpec.overriding(it.getter.element).build() })
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}
