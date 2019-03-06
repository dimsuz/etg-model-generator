package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.SchedulingSettings
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import io.reactivex.subjects.PublishSubject
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
  val requestType = createRequestType()
  val requestStreamProp = createRequestStreamProp(requestType)
  val scheduleRequestFun = createScheduleRequestFun(requestType, requestStreamProp)
  val fileSpec = FileSpec
    .builder(modelElement.enclosingPackageName, "$className.kt")
    .addType(
      TypeSpec.classBuilder(className)
        .primaryConstructor(
          PropertySpec.builder(
            OPERATIONS_PROPERTY_NAME,
            operations
          ).addModifiers(KModifier.PRIVATE).build(),
          PropertySpec.builder(
            SCHEDULING_PROPERTY_NAME,
            SchedulingSettings::class
          ).addModifiers(KModifier.PRIVATE).build()
        )
        .addSuperinterface(modelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addProperty(requestStreamProp)
        .addFunction(scheduleRequestFun)
        .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
        .addType(requestType)
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}

private fun createRequestStreamProp(requestType: TypeSpec): PropertySpec {
  return PropertySpec.builder("requestStream", PublishSubject::class.asClassName().parameterizedBy(ClassName.bestGuess(requestType.name!!)), KModifier.PRIVATE)
    .initializer("PublishSubject.create<%T>()", TypeVariableName(requestType.name!!))
    .build()
}

private fun generateNonReactiveMethodImpl(element: ExecutableElement): FunSpec {
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

private fun createScheduleRequestFun(
  requestType: TypeSpec,
  requestStreamProp: PropertySpec
): FunSpec {
  return FunSpec.builder("scheduleRequest")
    .addModifiers(KModifier.PRIVATE)
    .addParameter("request", ClassName.bestGuess(requestType.name!!))
    .addStatement("$SCHEDULING_PROPERTY_NAME.checkIsOnUiThread()")
    .addStatement("%N.onNext(%N)", requestStreamProp, "request")
    .build()
}

private fun createRequestType(): TypeSpec {
  return TypeSpec.classBuilder("Request")
    .addModifiers(KModifier.SEALED, KModifier.PRIVATE)
    .build()
}

private const val OPERATIONS_PROPERTY_NAME = "operations"
private const val SCHEDULING_PROPERTY_NAME = "schedulingSettings"
