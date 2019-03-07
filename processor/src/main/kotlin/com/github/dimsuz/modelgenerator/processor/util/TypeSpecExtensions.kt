package com.github.dimsuz.modelgenerator.processor.util

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal fun TypeSpec.Builder.primaryConstructor(
  vararg properties: PropertySpec,
  code: CodeBlock? = null
): TypeSpec.Builder {
  val propertySpecs = properties.map { p -> p.toBuilder().initializer(p.name).build() }
  val parameters = propertySpecs.map { ParameterSpec.builder(it.name, it.type).build() }
  val constructor = FunSpec.constructorBuilder()
    .addParameters(parameters)
    .apply { code?.let { this.addCode(it) } }
    .build()

  return this
    .primaryConstructor(constructor)
    .addProperties(propertySpecs)
}
