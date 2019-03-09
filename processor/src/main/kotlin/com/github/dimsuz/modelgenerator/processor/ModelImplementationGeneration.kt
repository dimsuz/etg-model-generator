package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.model.ReactiveModel
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.LceStateTypeInfo
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveGetter
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
import com.github.dimsuz.modelgenerator.processor.util.asClassName
import com.github.dimsuz.modelgenerator.processor.util.constructors
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
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
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
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
): Either<String, Unit> {
  val modelElement = modelDescription.modelElement
  val modelName = modelElement.simpleName.toString()
  val className = ClassName(modelElement.enclosingPackageName, modelName + "Impl")
  val stateClassName = className.nestedClass("State")
  val requestClassName = className.nestedClass("Request")
  val actionClassName = className.nestedClass("Action")
  val fileSpec = FileSpec
    .builder(modelElement.enclosingPackageName, "${className.simpleName}.kt")
    .addType(
      TypeSpec.classBuilder(className)
        .superclass(
          modelDescription.superTypeElement.asClassName()
            .parameterizedBy(stateClassName, requestClassName, actionClassName)
        )
        .apply {
          modelDescription.superTypeElement.constructors().single().parameters.forEach {
            addSuperclassConstructorParameter(it.simpleName.toString())
          }
        }
        .primaryConstructor(
          listOf(
            PropertySpec.builder(
              OPERATIONS_PROPERTY_NAME,
              operations
            ).addModifiers(KModifier.PRIVATE).build()
          ),
          parameters = modelDescription.superTypeElement.constructors().single()
            .parameters.map { ParameterSpec.getWrapper(it) }
        )
        .addSuperinterface(modelElement.asClassName())
        .addModifiers(KModifier.INTERNAL)
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveRequest(it.request, requestClassName) })
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveGetter(it.getter) })
        .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
        .addFunction(createBindRequestsMethod(requestClassName, stateClassName, actionClassName))
        .addFunction(createReduceStateMethod(stateClassName, actionClassName, modelDescription.reactiveProperties.map { it.getter }))
        .addFunction(createCreateInitialStateMethod(stateClassName))
        .addType(createRequestType(requestClassName, modelDescription.reactiveProperties.map { it.request }))
        .addType(
          createStateType(
            stateClassName,
            modelDescription.reactiveProperties.map { it.getter },
            lceStateTypeInfo
          )
        )
        .addType(
          createActionType(
            actionClassName,
            modelDescription.reactiveProperties.map { it.getter },
            lceStateTypeInfo
          )
        )
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
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
        .primaryConstructor(listOf(PropertySpec.builder("state", lceStateTypeInfo.parameterizedBy(getter)).build()))
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
              lceStateTypeInfo.parameterizedBy(getter).copy(nullable = true)
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
          lceStateTypeInfo.parameterizedBy(getter).copy(nullable = true)
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
        .addStatement("""
          |return $stateChangesPropertyName
          |    .filter { it.${getter.name} != null }
          |    .map { it.${getter.name}!! }
          |    .distinctUntilChanged()
        """.trimMargin())
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
        .addStatement("$scheduleRequestFunName(%T$args)",requestClassName.nestedClass(requestElementTypeName(request)))
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
  requestClassName: ClassName,
  stateClassName: ClassName,
  actionClassName: ClassName
): FunSpec {
  // no way to auto-override parameterized method of non-DeclaredType, have to rely on "expected"
  // shape of this method.
  // TODO check that this method is of expected shape and give a pretty error instead of generating erroneous code?
  val method = ReactiveModel<*, *, *>::bindRequest
  return FunSpec.builder(method.name)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(method.valueParameters[0].name!!, requestClassName)
    .addParameter(method.valueParameters[1].name!!, stateClassName)
    .addStatement("TODO()")
    .returns(Observable::class.java.asClassName().parameterizedBy(actionClassName))
    .build()
}

private fun createReduceStateMethod(
  stateClassName: ClassName,
  actionClassName: ClassName,
  getters: List<ReactiveGetter>
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
    .addParameter(ParameterSpec.builder(previousStateParamName, stateClassName).build())
    .addParameter(ParameterSpec.builder(actionParamName, actionClassName).build())
    .beginControlFlow("return when ($actionParamName)")
    .apply {
      getters.forEach {
        addStatement(
          "is %T -> $previousStateParamName.copy(${it.name} = $actionParamName.state)",
          actionClassName.nestedClass(actionElementTypeName(it))
        )
      }
    }
    .endControlFlow()
    .returns(stateClassName)
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

private fun LceStateTypeInfo.parameterizedBy(getter: ReactiveGetter): ParameterizedTypeName {
  return this.type.asClassName()
    .parameterizedBy(getter.contentType.asTypeName().javaToKotlinType(omitVarianceModifiers = true))
}

private fun requestElementTypeName(request: ReactiveRequest): String {
  return request.name.capitalize() + "Request"
}

private fun actionElementTypeName(getter: ReactiveGetter): String {
  return "Update${getter.name.capitalize()}Action"
}


private const val OPERATIONS_PROPERTY_NAME = "operations"
