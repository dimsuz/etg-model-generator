package com.github.dimsuz.modelgenerator.processor.util

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

fun TypeMirror.asClassName(): ClassName {
  return ((this as? DeclaredType)?.asElement() as? TypeElement)?.asClassName()
    ?: throw IllegalArgumentException("expected $this to refer to the declared class")
}
