package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.ModelOperations
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.getWrapper
import com.github.dimsuz.modelgenerator.processor.util.isNullable
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
import io.reactivex.Observable
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
        .addFunctions(modelDescription.reactiveProperties
          .map { createRequestMethod(it, modelDescription.stateClassName) }
        )
        .addFunctions(modelDescription.nonReactiveMethods
          .map { createNonReactiveMethod(it, modelDescription.stateClassName) }
        )
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec).map { ClassName(fileSpec.packageName, className) }
}

private fun createRequestMethod(property: ReactiveProperty, stateClassName: ClassName): FunSpec {
  val requestOperationName = requestOperationFunName(property.request)
  val returnType = if (property.getter.hasUnitContent) {
    Completable::class.asClassName()
  } else {
    Single::class.asClassName().parameterizedBy(property.getter.contentType)
  }
  return FunSpec.builder(requestOperationName)
    .addParameters(property.request.parameters.map { ParameterSpec.getWrapper(it) })
    .addParameter("state", stateClassName)
    .addModifiers(KModifier.ABSTRACT)
    .returns(returnType)
    .build()
}

private fun createNonReactiveMethod(executableElement: ExecutableElement, stateClassName: ClassName): FunSpec {
  return FunSpec.builder(executableElement.simpleName.toString())
    .addParameter("stateChanges", Observable::class.asClassName().parameterizedBy(stateClassName))
    .addParameters(executableElement.parameters.map { ParameterSpec.getWrapper(it) })
    .addModifiers(KModifier.ABSTRACT)
    .returns(executableElement.returnType.asTypeName().javaToKotlinType(executableElement.isNullable))
    .build()
}

internal fun requestOperationFunName(request: ReactiveRequest): String {
  return "create${request.name.capitalize()}Operation"
}
