package com.github.dimsuz.modelgenerator.processor.util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.asTypeVariableName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeVariable
import kotlin.reflect.jvm.internal.impl.builtins.jvm.JavaToKotlinClassMap
import kotlin.reflect.jvm.internal.impl.name.FqName

// Extensions contained here are meant to work around the fact that
// in absence of kotlin metadata wrong types get generated, i.e. instead of kotlin.String, kotlin.List
// we'd have java.lang.String and java.collections.List which would fail compilation of generated classes
//
// See https://github.com/square/kotlinpoet/issues/236 for details

internal fun TypeName.javaToKotlinType(): TypeName {
  return if (this is ParameterizedTypeName) {
    (rawType.javaToKotlinType() as ClassName).parameterizedBy(
      *typeArguments.map { it.javaToKotlinType() }.toTypedArray()
    )
  } else {
    val className = JavaToKotlinClassMap.INSTANCE
      .mapJavaToKotlin(FqName(toString()))?.asSingleFqName()?.asString()
    if (className == null) this
    else ClassName.bestGuess(className)
  }
}

internal fun ParameterSpec.Companion.getWrapper(element: VariableElement): ParameterSpec {
  return ParameterSpec
    .builder(
      element.simpleName.toString(),
      element.asType().asTypeName().javaToKotlinType()
    )
    .jvmModifiers(element.modifiers).build()
}

fun FunSpec.Companion.overridingWrapper(method: ExecutableElement): FunSpec.Builder {
  var modifiers: Set<Modifier> = method.modifiers
  require(
    Modifier.PRIVATE !in modifiers
      && Modifier.FINAL !in modifiers
      && Modifier.STATIC !in modifiers
  ) {
    "cannot override method with modifiers: $modifiers"
  }

  val methodName = method.simpleName.toString()
  val funBuilder = FunSpec.builder(methodName)

  funBuilder.addModifiers(KModifier.OVERRIDE)

  modifiers = modifiers.toMutableSet()
  modifiers.remove(Modifier.ABSTRACT)
  funBuilder.jvmModifiers(modifiers)

  method.typeParameters
    .map { it.asType() as TypeVariable }
    .map { it.asTypeVariableName() }
    .forEach { funBuilder.addTypeVariable(it) }

  funBuilder.returns(method.returnType.asTypeName().javaToKotlinType())
  funBuilder.addParameters(method.parameters.map { ParameterSpec.getWrapper(it) })
  if (method.isVarArgs) {
    funBuilder.parameters[funBuilder.parameters.lastIndex] = funBuilder.parameters.last()
      .toBuilder()
      .addModifiers(KModifier.VARARG)
      .build()
  }

  if (method.thrownTypes.isNotEmpty()) {
    val throwsValueString = method.thrownTypes.joinToString { "%T::class" }
    funBuilder.addAnnotation(
      AnnotationSpec.builder(Throws::class)
        .addMember(throwsValueString, *method.thrownTypes.toTypedArray())
        .build()
    )
  }

  return funBuilder
}
