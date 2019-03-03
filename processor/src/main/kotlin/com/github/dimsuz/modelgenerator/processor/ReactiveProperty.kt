package com.github.dimsuz.modelgenerator.processor

import javax.lang.model.element.VariableElement
import javax.lang.model.type.TypeMirror

internal data class ReactiveProperty(
  val request: ReactiveRequest,
  val getter: ReactiveGetter
)

internal data class ReactiveRequest(
  val name: String,
  val parameters: List<VariableElement>
)

internal data class ReactiveGetter(
  val name: String,
  val returnType: TypeMirror
)
