package com.github.dimsuz.modelgenerator.processor.entity

import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.TypeMirror

internal data class LceStateTypeInfo(
  val type: TypeMirror,
  val contentConstructor: ExecutableElement,
  val errorConstructor: ExecutableElement,
  val loadingConstructor: ExecutableElement
)
