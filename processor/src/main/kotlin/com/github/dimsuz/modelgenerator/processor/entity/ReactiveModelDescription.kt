package com.github.dimsuz.modelgenerator.processor.entity

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

internal data class ReactiveModelDescription(
  val modelElement: TypeElement,
  val reactiveProperties: List<ReactiveProperty>,
  val nonReactiveMethods: List<ExecutableElement>
)
