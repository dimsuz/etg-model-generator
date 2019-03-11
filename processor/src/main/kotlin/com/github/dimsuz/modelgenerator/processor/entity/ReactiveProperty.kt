package com.github.dimsuz.modelgenerator.processor.entity

import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

internal data class ReactiveProperty(
  val request: ReactiveRequest,
  val getter: ReactiveGetter
)

internal data class ReactiveRequest(
  val name: String,
  val element: ExecutableElement,
  val parameters: List<VariableElement>
)

internal data class ReactiveGetter(
  val name: String,
  val element: ExecutableElement,
  val contentType: TypeName
) {
  val hasUnitContent get() = contentType == Unit::class.asTypeName()
}
