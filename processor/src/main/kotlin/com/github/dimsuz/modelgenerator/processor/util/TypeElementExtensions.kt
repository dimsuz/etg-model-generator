package com.github.dimsuz.modelgenerator.processor.util

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

fun TypeElement.constructors(): List<ExecutableElement> {
  return ElementFilter.constructorsIn(this.enclosedElements)
}
