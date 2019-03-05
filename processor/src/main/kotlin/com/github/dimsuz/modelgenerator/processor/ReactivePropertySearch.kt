package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.LceStateTypeInfo
import com.github.dimsuz.modelgenerator.processor.entity.Left
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveGetter
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveModelDescription
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveProperty
import com.github.dimsuz.modelgenerator.processor.entity.ReactiveRequest
import com.github.dimsuz.modelgenerator.processor.entity.Right
import com.github.dimsuz.modelgenerator.processor.entity.join
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.util.enclosedMethods
import com.github.dimsuz.modelgenerator.processor.util.isSameErasedType
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
): Either<String, ReactiveModelDescription> {
  val propertyEithers = mutableListOf<Either<String, ReactiveProperty>>()
  for (m in element.enclosedMethods) {
    if (m.returnType.isReactiveLceType(processingEnv, lceStateTypeInfo)) {
      propertyEithers.add(m.toReactivePropertyOf(element))
    }
  }
  return propertyEithers.join().map { properties ->
    val nonReactiveMethods = element.enclosedMethods
      .filter { m -> properties.none { it.getter.element == m || it.request.element == m } }
    ReactiveModelDescription(element, properties, nonReactiveMethods)
  }
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
    // return type will be Observable<LceState<T>> reach into Observable then into LceState
    contentType = element.returnType.firstTypeArgument().firstTypeArgument()
  )
}

private fun TypeMirror.firstTypeArgument(): TypeMirror {
  return (this as DeclaredType).typeArguments.first()
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
  return Right(
    ReactiveRequest(
      name = requestElement.simpleName.toString(),
      element = requestElement,
      parameters = requestElement.parameters
    )
  )
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
