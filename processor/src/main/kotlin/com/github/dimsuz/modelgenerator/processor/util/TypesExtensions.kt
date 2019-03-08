package com.github.dimsuz.modelgenerator.processor.util

import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

internal fun Types.isAssignable(type: TypeMirror, c: Class<*>, elementUtils: Elements): Boolean {
  return isAssignable(
    type,
    elementUtils.getTypeElement(c.canonicalName).asType()
  )
}

internal fun Types.isSameType(type: TypeMirror, c: Class<*>, elementUtils: Elements): Boolean {
  return isSameType(
    type,
    elementUtils.getTypeElement(c.canonicalName).asType()
  )
}

internal fun Types.isSubtype(type: TypeMirror, c: Class<*>, elementUtils: Elements): Boolean {
  return isSubtype(
    type,
    elementUtils.getTypeElement(c.canonicalName).asType()
  )
}

internal fun Types.isSameErasedType(type: TypeMirror, c: Class<*>, elementUtils: Elements): Boolean {
  return isSameType(
    erasure(type),
    erasure(elementUtils.getTypeElement(c.canonicalName).asType())
  )
}

internal fun Types.isSameErasedType(first: TypeMirror, second: TypeMirror): Boolean {
  return isSameType(
    erasure(first),
    erasure(second)
  )
}
