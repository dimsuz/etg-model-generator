package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.util.Either
import com.github.dimsuz.modelgenerator.processor.util.Left
import com.github.dimsuz.modelgenerator.processor.util.Right
import com.github.dimsuz.modelgenerator.processor.util.enclosedMethods
import com.github.dimsuz.modelgenerator.processor.util.isSameErasedType
import com.github.dimsuz.modelgenerator.processor.util.join
import com.github.dimsuz.modelgenerator.processor.util.map
import io.reactivex.Observable
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal fun findReactiveProperties(
  processingEnv: ProcessingEnvironment,
  lceStateTypeInfo: LceStateTypeInfo,
  element: TypeElement
): Either<String, List<ReactiveProperty>> {
  val properties = element.enclosedMethods.filter { it.returnType.isReactiveLceType(processingEnv, lceStateTypeInfo) }
    .map { it.toReactivePropertyOf(element) }
  return properties.join()
}

private fun ExecutableElement.toReactivePropertyOf(
  reactiveModelElement: TypeElement
): Either<String, ReactiveProperty> {
  val getter = extractGetter(this)
  val request = extractRequest(reactiveModelElement, getter)
  return request.map {
    ReactiveProperty(
      request = it,
      getter = getter
    )
  }
}

private fun extractGetter(element: ExecutableElement): ReactiveGetter {
  return ReactiveGetter(
    name = element.simpleName.toString(),
    element = element,
    contentType = (element.returnType as DeclaredType).typeArguments.single()
  )
}

private fun extractRequest(
  reactiveModelElement: TypeElement,
  getter: ReactiveGetter
): Either<String, ReactiveRequest> {
  val requestElement = reactiveModelElement.enclosedMethods
    .find { it.returnType.kind == TypeKind.VOID && it.simpleName.isRequestNameOf(getter.name) }
    ?: return Left(
      "no request method found for reactive " +
        "getter '${getter.name}()'. Expected to find: '${getter.name.toRequestName()}()'"
    )
  return Right(ReactiveRequest(
    name = requestElement.simpleName.toString(),
    parameters = requestElement.parameters
  ))
}

private fun Name.isRequestNameOf(getterName: String): Boolean {
  return this.toString() == getterName.toRequestName()
}

private fun String.toRequestName() = removeSuffix(GETTER_SUFFIX)

private fun TypeMirror.isReactiveLceType(
  processingEnv: ProcessingEnvironment,
  lceStateTypeInfo: LceStateTypeInfo
): Boolean {
  return this.kind == TypeKind.DECLARED
    && processingEnv.typeUtils.isSameErasedType(this, Observable::class.java, processingEnv.elementUtils)
    && (this as DeclaredType).typeArguments.size == 1
    && processingEnv.typeUtils.isSameErasedType(this.typeArguments.single(), lceStateTypeInfo.type)
}

private const val GETTER_SUFFIX = "State"
