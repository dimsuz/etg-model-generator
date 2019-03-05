package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.ModelOperations
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.getWrapper
import com.github.dimsuz.modelgenerator.processor.util.javaToKotlinType
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.reactivex.Completable
import io.reactivex.Single
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

internal fun generateModelOperations(
  processingEnv: ProcessingEnvironment,
  modelDescription: ReactiveModelDescription
): Either<String, ClassName> {
  val modelElement = modelDescription.modelElement
  val modelName = modelElement.simpleName.toString()
  val className = modelName + "Operations"
  val fileSpec = FileSpec
    .builder(modelElement.enclosingPackageName, "$className.kt")
    .addType(
      TypeSpec.interfaceBuilder(className)
        .addSuperinterface(
          ModelOperations::class.asClassName().parameterizedBy(modelElement.asType().asTypeName())
        )
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(modelDescription.reactiveProperties.map { createRequestMethod(it) })
        .addFunctions(modelDescription.nonReactiveMethods.map { createNonReactiveMethod(it) })
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec).map { ClassName(fileSpec.packageName, className) }
}

private fun createRequestMethod(property: ReactiveProperty): FunSpec {
  val requestOperationName = "create${property.request.name.capitalize()}Operation"
  val returnType = if (property.getter.contentType.asTypeName() == Unit::class.asTypeName()) {
    Completable::class.asClassName()
  } else {
    Single::class.asClassName().parameterizedBy(property.getter.contentType.asTypeName().javaToKotlinType())
  }
  return FunSpec.builder(requestOperationName)
    .addParameters(property.request.parameters.map { ParameterSpec.getWrapper(it) })
    .addModifiers(KModifier.ABSTRACT)
    .returns(returnType)
    .build()
}

private fun createNonReactiveMethod(executableElement: ExecutableElement): FunSpec {
  return FunSpec.builder(executableElement.simpleName.toString())
    .addParameters(executableElement.parameters.map { ParameterSpec.getWrapper(it) })
    .addModifiers(KModifier.ABSTRACT)
    .returns(executableElement.returnType.asTypeName().javaToKotlinType())
    .build()
}
