package com.github.dimsuz.modelgenerator.processor.entity

import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.squareup.kotlinpoet.ClassName
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

internal data class ReactiveModelDescription(
  val modelElement: TypeElement,
  val superTypeElement: TypeElement,
  val reactiveProperties: List<ReactiveProperty>,
  val nonReactiveMethods: List<ExecutableElement>
) {
  val className get() = ClassName(modelElement.enclosingPackageName, modelElement.simpleName.toString() + "Impl")
  val stateClassName get() = className.nestedClass("State")
  val requestClassName get() = className.nestedClass("Request")
  val actionClassName get() = className.nestedClass("Action")
}
