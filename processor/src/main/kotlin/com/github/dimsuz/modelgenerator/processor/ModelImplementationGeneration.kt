package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.SchedulingSettings
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveGetter
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
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
import com.squareup.kotlinpoet.asClassName
import io.reactivex.Observable
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
  val className = ClassName(modelElement.enclosingPackageName, modelName + "Impl")
  val stateClassName = className.nestedClass("State")
  val requestClassName = className.nestedClass("Request")
  val actionClassName = className.nestedClass("Action")
  val requestStreamProp = createRequestStreamProp(requestClassName)
  val scheduleRequestFun = createScheduleRequestFun(requestClassName, requestStreamProp)
  val constructorCode = createConstructorBody(requestStreamProp)
  val fileSpec = FileSpec
    .builder(modelElement.enclosingPackageName, "${className.simpleName}.kt")
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
          ).addModifiers(KModifier.PRIVATE).build(),
          code = constructorCode
        )
        .addSuperinterface(modelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addProperty(requestStreamProp)
        .addProperty(PropertySpec
          .builder(STATE_CHANGES_PROPERTY_NAME, Observable::class.java.asClassName().parameterizedBy(stateClassName), KModifier.PRIVATE)
          .build())
        .addProperty(PropertySpec
          .builder(LAST_STATE_PROPERTY_NAME, stateClassName, KModifier.PRIVATE)
          .mutable(true)
          .addAnnotation(Volatile::class)
          .initializer("%T()", stateClassName)
          .build())
        .addFunction(scheduleRequestFun)
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveRequest(it.request) })
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveGetter(it.getter) })
        .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
        .addFunction(createCommandTemplateMethod(requestClassName, stateClassName, actionClassName))
        .addFunction(createReduceStateMethod(stateClassName, actionClassName))
        .addType(TypeSpec.classBuilder(requestClassName)
          .addModifiers(KModifier.PRIVATE)
          .build())
        .addType(TypeSpec.classBuilder(stateClassName)
          .addModifiers(KModifier.PRIVATE)
          .build())
        .addType(TypeSpec.classBuilder(actionClassName)
          .addModifiers(KModifier.PRIVATE)
          .build())
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
}

private fun createConstructorBody(requestStreamProp: PropertySpec): CodeBlock {
  return CodeBlock.of(
    """
    |$SCHEDULING_PROPERTY_NAME.checkIsOnUiThread()
    |$STATE_CHANGES_PROPERTY_NAME = %N
    |  .concatMap { request ->
    |    $CREATE_COMMAND_FUN_NAME(request, $LAST_STATE_PROPERTY_NAME)
    |      .map { r -> $REDUCE_STATE_FUN_NAME($LAST_STATE_PROPERTY_NAME, r) }
    |      .doOnNext { $LAST_STATE_PROPERTY_NAME = it }
    |  }
    |  .share()
    |stateChanges
    |  .observeOn($SCHEDULING_PROPERTY_NAME.uiScheduler)
    |  .subscribe(
    |    {
    |    },
    |    { /* TODO LOG (it, "commands must not throw errors, rather set some error flag in state") */ })
  """.trimMargin(),
    requestStreamProp
  )
}

private fun createRequestStreamProp(requestClassName: ClassName): PropertySpec {
  return PropertySpec.builder(
    "requestStream",
    PublishSubject::class.asClassName().parameterizedBy(requestClassName),
    KModifier.PRIVATE
  )
    .initializer("PublishSubject.create<%T>()", requestClassName)
    .build()
}

private fun generateReactiveGetter(getter: ReactiveGetter): FunSpec {
  return FunSpec.overridingWrapper(getter.element)
    .addCode(
      CodeBlock.builder()
        .addStatement("TODO()")
        .build()
    )
    .build()
}

private fun generateReactiveRequest(request: ReactiveRequest): FunSpec {
  return FunSpec.overridingWrapper(request.element)
    .addCode(
      CodeBlock.builder()
        .addStatement("TODO()")
        .build()
    )
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
  requestClassName: ClassName,
  requestStreamProp: PropertySpec
): FunSpec {
  return FunSpec.builder("scheduleRequest")
    .addModifiers(KModifier.PRIVATE)
    .addParameter("request", requestClassName)
    .addStatement("$SCHEDULING_PROPERTY_NAME.checkIsOnUiThread()")
    .addStatement("%N.onNext(%N)", requestStreamProp, "request")
    .build()
}

private fun createRequestType(): TypeSpec {
  return TypeSpec.classBuilder("Request")
    .addModifiers(KModifier.SEALED, KModifier.PRIVATE)
    .build()
}

private fun createCommandTemplateMethod(requestClassName: ClassName, stateClassName: ClassName, actionClassName: ClassName): FunSpec {
  return FunSpec.builder(CREATE_COMMAND_FUN_NAME)
    .addModifiers(KModifier.PRIVATE)
    .addParameter("request", requestClassName)
    .addParameter("state", stateClassName)
    .addStatement("TODO()")
    .returns(Observable::class.java.asClassName().parameterizedBy(actionClassName))
    .build()
}

private fun createReduceStateMethod(stateClassName: ClassName, actionClassName: ClassName): FunSpec {
  return FunSpec.builder(REDUCE_STATE_FUN_NAME)
    .addModifiers(KModifier.PRIVATE)
    .addParameter("previousState", stateClassName)
    .addParameter("action", actionClassName)
    .addStatement("return previousState")
    .returns(stateClassName)
    .build()
}

private const val OPERATIONS_PROPERTY_NAME = "operations"
private const val SCHEDULING_PROPERTY_NAME = "schedulingSettings"
private const val STATE_CHANGES_PROPERTY_NAME = "stateChanges"
private const val LAST_STATE_PROPERTY_NAME = "lastState"
private const val CREATE_COMMAND_FUN_NAME = "createCommand"
private const val REDUCE_STATE_FUN_NAME = "reduceState"
