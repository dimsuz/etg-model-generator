package com.github.dimsuz.modelgenerator.processor.entity

import com.squareup.kotlinpoet.ClassName
import javax.lang.model.element.TypeElement

internal data class LceStateTypeInfo(
  val element: TypeElement,
  val className: ClassName
)
