package com.github.dimsuz.modelgenerator.processor.util

import org.jetbrains.annotations.Nullable
import javax.lang.model.element.Element

internal fun Element.isNullable(): Boolean {
  return getAnnotation(Nullable::class.java) != null
}

internal fun Element.isNotNull(): Boolean {
  return !isNullable()
}
