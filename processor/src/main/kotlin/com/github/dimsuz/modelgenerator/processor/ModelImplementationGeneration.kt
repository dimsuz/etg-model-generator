package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.model.ReactiveModel
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveGetter
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
import com.github.dimsuz.modelgenerator.processor.util.constructors
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.getWrapper
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
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.reactivex.Observable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import kotlin.reflect.full.valueParameters

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
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveRequest(it.request) })
        .addFunctions(modelDescription.reactiveProperties.map { generateReactiveGetter(it.getter) })
        .addFunctions(modelDescription.nonReactiveMethods.map { generateNonReactiveMethodImpl(it) })
        .addFunction(createBindRequestsMethod(requestClassName, stateClassName, actionClassName))
        .addFunction(createReduceStateMethod(stateClassName, actionClassName))
        .addFunction(createCreateInitialStateMethod(stateClassName))
        .addType(
          TypeSpec.classBuilder(requestClassName)
            .addModifiers(KModifier.INTERNAL)
            .build()
        )
        .addType(
          TypeSpec.classBuilder(stateClassName)
            .addModifiers(KModifier.INTERNAL)
            .build()
        )
        .addType(
          TypeSpec.classBuilder(actionClassName)
            .addModifiers(KModifier.INTERNAL)
            .build()
        )
        .build()
    )
    .build()
  return writeFile(processingEnv, fileSpec)
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
  actionClassName: ClassName
): FunSpec {
  val method = ReactiveModel<*, *, *>::reduceState
  // no way to auto-override parameterized method of non-DeclaredType, have to rely on "expected"
  // shape of this method.
  // TODO check that this method is of expected shape and give a pretty error instead of generating erroneous code?
  return FunSpec
    .builder(method.name)
    .addModifiers(KModifier.OVERRIDE)
    .addParameter(ParameterSpec.builder(method.valueParameters[0].name!!, stateClassName).build())
    .addParameter(ParameterSpec.builder(method.valueParameters[1].name!!, actionClassName).build())
    .addStatement("TODO()")
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
    .addStatement("TODO()")
    .returns(stateClassName)
    .build()
}

private const val OPERATIONS_PROPERTY_NAME = "operations"
