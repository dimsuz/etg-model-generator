package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.ModelGenerator
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.ProcessingEnvironment

internal fun generateFactoryExtension(
  modelDescription: ReactiveModelDescription,
  modelTypeSpec: TypeSpec,
  processingEnv: ProcessingEnvironment
): Either<String, Unit> {
  val constructorCallArgs = modelTypeSpec.primaryConstructor!!.parameters.map { it.name }
    .joinToString(", ")
  val fileSpec = FileSpec
    .builder(modelDescription.modelElement.enclosingPackageName, "${ModelGenerator::class.simpleName}Extensions.kt")
    .addFunction(FunSpec.builder("createModel")
      .addParameters(modelTypeSpec.primaryConstructor!!.parameters)
      .addModifiers(KModifier.INTERNAL)
      .receiver(ModelGenerator::class)
      .addStatement("return %T($constructorCallArgs)", modelDescription.className)
      .returns(modelDescription.modelElement.asClassName())
      .build())
    .build()
  return writeFile(processingEnv, fileSpec)
}
