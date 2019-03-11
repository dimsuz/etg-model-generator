package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.lcestate.LceStateFactory
import com.github.dimsuz.modelgenerator.model.ReactiveModel
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.LceStateTypeInfo
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveGetter
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.util.constructors
import com.github.dimsuz.modelgenerator.processor.util.getWrapper
import com.github.dimsuz.modelgenerator.processor.util.javaToKotlinType
import com.github.dimsuz.modelgenerator.processor.util.overridingWrapper
import com.github.dimsuz.modelgenerator.processor.util.primaryConstructor
import com.github.dimsuz.modelgenerator.processor.util.writeFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.reactivex.Observable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import kotlin.reflect.full.valueParameters

internal fun generateModelImplementation(
  processingEnv: ProcessingEnvironment,
  modelDescription: ReactiveModelDescription,
  lceStateTypeInfo: LceStateTypeInfo,
  operations: ClassName
): Either<String, TypeSpec> {
  val reactiveGetters = modelDescription.reactiveProperties.map { it.getter }
  val reactiveRequests = modelDescription.reactiveProperties.map { it.request }
  val constructorParameters = modelDescription.superTypeElement.constructors().single().parameters
  val className = modelDescription.className
  val stateClassName = modelDescription.stateClassName
  val requestClassName = modelDescription.requestClassName
  val actionClassName = modelDescription.actionClassName
  val modelTypeSpec = TypeSpec.classBuilder(className)
    .superclass(
      modelDescription.superTypeElement.asClassName()
        .parameterizedBy(stateClassName, requestClassName, actionClassName)
    )
    .apply {
      constructorParameters.forEach {
        addSuperclassConstructorParameter(it.simpleName.toString())
      }
    }
    .primaryConstructor(
      listOf(
        PropertySpec.builder(
          OPERATIONS_PROPERTY_NAME,
          operations
        ).addModifiers(KModifier.PRIVATE).build(),
        PropertySpec.builder(
          LCE_FACTORY_PROPERTY_NAME,
          LceStateFactory::class.asClassName().parameterizedBy(lceStateTypeInfo.className.parameterizedBy(STAR))
        ).addModifiers(KModifier.PRIVATE).build()
      ),
      parameters = constructorParameters.map { ParameterSpec.getWrapper(it) }
    )
    .addSuperinterface(modelDescription.modelElement.asClassName())
    .addModifiers(KModifier.INTERNAL)
    .addFunctions(reactiveRequests.map { generateReactiveRequest(it, requestClassName) })
    .addFunctions(reactiveGetters.map { generateReactiveGetter(it) })
    .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
    .addFunction(createBindRequestsMethod(modelDescription, lceStateTypeInfo))
    .addFunction(createReduceStateMethod(modelDescription))
    .addFunction(createCreateInitialStateMethod(stateClassName))
    .addType(createRequestType(requestClassName, reactiveRequests))
    .addType(
      createStateType(
        stateClassName,
        reactiveGetters,
        lceStateTypeInfo
      )
    )
    .addType(
      createActionType(
        actionClassName,
        reactiveGetters,
        lceStateTypeInfo
      )
    )
    .build()

  val fileSpec = FileSpec
    .builder(modelDescription.className.packageName, modelDescription.className.simpleName + ".kt")
    .addType(modelTypeSpec)
    .build()
  return writeFile(processingEnv, fileSpec).map { modelTypeSpec }
}

private fun createActionType(
  actionClassName: ClassName,
  getters: List<ReactiveGetter>,
  lceStateTypeInfo: LceStateTypeInfo
): TypeSpec {
  return TypeSpec.classBuilder(actionClassName)
    .addModifiers(KModifier.INTERNAL, KModifier.SEALED)
    .addTypes(getters.map { getter ->
      TypeSpec.classBuilder(actionElementTypeName(getter))
        .superclass(actionClassName)
        .addModifiers(KModifier.DATA)
        .primaryConstructor(listOf(
          PropertySpec.builder("state", lceStateTypeInfo.className.parameterizedBy(getter.contentType)).build()
        ))
        .build()
    })
    .build()
}

private fun createStateType(
  stateClassName: ClassName,
  getters: List<ReactiveGetter>,
  lceStateTypeInfo: LceStateTypeInfo
): TypeSpec {
  return TypeSpec.classBuilder(stateClassName)
    .addModifiers(KModifier.DATA, KModifier.INTERNAL)
    .primaryConstructor(FunSpec.constructorBuilder()
      .addParameters(
        getters.map { getter ->
          ParameterSpec
            .builder(
              getter.name,
              lceStateTypeInfo.className.parameterizedBy(getter.contentType).copy(nullable = true)
            )
            .defaultValue("null")
            .build()
        }
      )
      .build())
    .addProperties(getters.map { getter ->
      PropertySpec
        .builder(
          getter.name,
          lceStateTypeInfo.className.parameterizedBy(getter.contentType).copy(nullable = true)
        )
        .initializer(getter.name)
        .build()
    })
    .build()
}

private fun createRequestType(requestClassName: ClassName, requests: List<ReactiveRequest>): TypeSpec {
  return TypeSpec.classBuilder(requestClassName)
    .addModifiers(KModifier.INTERNAL, KModifier.SEALED)
    .addTypes(requests.map { request ->
      if (request.element.parameters.isEmpty()) {
        TypeSpec.objectBuilder(requestElementTypeName(request))
          .superclass(requestClassName)
          .build()
      } else {
        TypeSpec.classBuilder(requestElementTypeName(request))
          .superclass(requestClassName)
          .primaryConstructor(request.element.parameters.map {
            PropertySpec.builder(
              it.simpleName.toString(),
              it.asType().asTypeName().javaToKotlinType(omitVarianceModifiers = true)
            ).build()
          })
          .build()
      }
    })
    .build()
}

private fun generateReactiveGetter(getter: ReactiveGetter): FunSpec {
  val stateChangesPropertyName = ReactiveModel<*, *, *>::stateChanges.name
  return FunSpec.overridingWrapper(getter.element)
    .addCode(
      CodeBlock.builder()
        .addStatement(
          """
          |return $stateChangesPropertyName
          |    .filter { it.${getter.name} != null }
          |    .map { it.${getter.name}!! }
          |    .distinctUntilChanged()
        """.trimMargin()
        )
        .build()
    )
    .build()
}

private fun generateReactiveRequest(request: ReactiveRequest, requestClassName: ClassName): FunSpec {
  val scheduleRequestFunName = ReactiveModel<*, *, *>::scheduleRequest.name
  val args = request.parameters.takeIf { it.isNotEmpty() }?.joinToString(
    separator = ", ",
    prefix = "(",
    postfix = ")",
    transform = { it.simpleName.toString() })
    ?: ""
  return FunSpec.overridingWrapper(request.element)
    .addCode(
      CodeBlock.builder()
        .addStatement("$scheduleRequestFunName(%T$args)", requestClassName.nestedClass(requestElementTypeName(request)))
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

private fun createBindRequestsMethod(
  modelDescription: ReactiveModelDescription,
  lceStateTypeInfo: LceStateTypeInfo
): FunSpec {
  // no way to auto-override parameterized method of non-DeclaredType, have to rely on "expected"
  // shape of this method.
  // TODO check that this method is of expected shape and give a pretty error instead of generating erroneous code?
  val method = ReactiveModel<*, *, *>::bindRequest
  val requestParamName = method.valueParameters[0].name!!
  val stateParamName = method.valueParameters[1].name!!
  return FunSpec.builder(method.name)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(requestParamName, modelDescription.requestClassName)
    .addParameter(stateParamName, modelDescription.stateClassName)
    .beginControlFlow("return when ($requestParamName)")
    .apply {
      modelDescription.reactiveProperties.forEach { property ->
        addBindRequestWhenBranch(
          property,
          modelDescription.requestClassName,
          modelDescription.actionClassName,
          requestParamName,
          stateParamName,
          lceStateTypeInfo
        )
      }
    }
    .endControlFlow()
    .returns(Observable::class.java.asClassName().parameterizedBy(modelDescription.actionClassName))
    .build()
}

private fun FunSpec.Builder.addBindRequestWhenBranch(
  property: ReactiveProperty,
  requestClassName: ClassName,
  actionClassName: ClassName,
  requestParamName: String,
  stateParamName: String,
  lceStateTypeInfo: LceStateTypeInfo
) {
  val request = property.request
  val requestElementClass = requestClassName.nestedClass(requestElementTypeName(request))
  beginControlFlow("is %T ->", requestElementClass)
  val args = request.parameters.map { it.simpleName.toString() }
    .joinToString(", ", transform = { "$requestParamName.$it" })
    .let { if (it.isEmpty()) stateParamName else "$it, $stateParamName" }
  addStatement("@Suppress(\"UNCHECKED_CAST\")")
  if (!property.getter.hasUnitContent) {
    addStatement(
      """
    |$OPERATIONS_PROPERTY_NAME.${requestOperationFunName(request)}($args)
    |  .map<%1T> { %2T($LCE_FACTORY_PROPERTY_NAME.createLceContent(it) as %3T) }
    |  .onErrorReturn { %2T($LCE_FACTORY_PROPERTY_NAME.createLceError(it) as %3T) }
    |  .toObservable()
    |  .startWith(%2T($LCE_FACTORY_PROPERTY_NAME.createLceLoading() as %3T))
    """.trimMargin(),
      actionClassName,
      actionClassName.nestedClass(actionElementTypeName(property.getter)),
      lceStateTypeInfo.className.parameterizedBy(property.getter.contentType)
    )
  } else {
    addStatement(
      """
    |$OPERATIONS_PROPERTY_NAME.${requestOperationFunName(request)}($args)
    |  .andThen(Observable.fromCallable<%1T> { %2T($LCE_FACTORY_PROPERTY_NAME.createLceContent(Unit) as %3T) })
    |  .onErrorReturn { %2T($LCE_FACTORY_PROPERTY_NAME.createLceError(it) as %3T) }
    |  .startWith(%2T($LCE_FACTORY_PROPERTY_NAME.createLceLoading() as %3T))
    """.trimMargin(),
      actionClassName,
      actionClassName.nestedClass(actionElementTypeName(property.getter)),
      lceStateTypeInfo.className.parameterizedBy(property.getter.contentType)
    )
  }
  endControlFlow()
}

private fun createReduceStateMethod(
  modelDescription: ReactiveModelDescription
): FunSpec {
  val method = ReactiveModel<*, *, *>::reduceState
  // no way to auto-override parameterized method of non-DeclaredType, have to rely on "expected"
  // shape of this method.
  // TODO check that this method is of expected shape and give a pretty error instead of generating erroneous code?
  val previousStateParamName = method.valueParameters[0].name!!
  val actionParamName = method.valueParameters[1].name!!
  return FunSpec
    .builder(method.name)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec.builder(previousStateParamName, modelDescription.stateClassName).build())
    .addParameter(ParameterSpec.builder(actionParamName, modelDescription.actionClassName).build())
    .beginControlFlow("return when ($actionParamName)")
    .apply {
      modelDescription.reactiveProperties.map { it.getter }.forEach {
        addStatement(
          "is %T -> $previousStateParamName.copy(${it.name} = $actionParamName.state)",
          modelDescription.actionClassName.nestedClass(actionElementTypeName(it))
        )
      }
    }
    .endControlFlow()
    .returns(modelDescription.stateClassName)
    .build()
}

private fun createCreateInitialStateMethod(
  stateClassName: ClassName
): FunSpec {
  val method = ReactiveModel<*, *, *>::createInitialState
  // no way to auto-override parameterized method of non-DeclaredType, have to rely on "expected"
  // shape of this method.
  // TODO check that this method is of expected shape and give a pretty error instead of generating erroneous code?
  return FunSpec
    .builder(method.name)
    .addModifiers(KModifier.OVERRIDE)
    .addStatement("return %T()", stateClassName)
    .returns(stateClassName)
    .build()
}

private fun requestElementTypeName(request: ReactiveRequest): String {
  return request.name.capitalize() + "Request"
}

private fun actionElementTypeName(getter: ReactiveGetter): String {
  return "Update${getter.name.capitalize()}Action"
}

private const val OPERATIONS_PROPERTY_NAME = "operations"
private const val LCE_FACTORY_PROPERTY_NAME = "lceStateFactory"
