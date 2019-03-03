package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.util.Either
import com.github.dimsuz.modelgenerator.processor.util.Right
import com.github.dimsuz.modelgenerator.processor.util.enclosedMethods
import com.github.dimsuz.modelgenerator.processor.util.isSameErasedType
import com.github.dimsuz.modelgenerator.processor.util.warning
import io.reactivex.Observable
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

internal fun findReactiveProperties(
  roundEnv: RoundEnvironment,
  processingEnv: ProcessingEnvironment,
  lceStateTypeInfo: LceStateTypeInfo,
  element: TypeElement
): Either<String, List<ReactiveProperty>> {
  element.enclosedMethods.filter { it.returnType.isReactiveLceType(processingEnv, lceStateTypeInfo) }
    .onEach {
      processingEnv.messager.warning("found getter: ${it.simpleName}")
    }
  return Right(emptyList())
}

private fun TypeMirror.isReactiveLceType(
  processingEnv: ProcessingEnvironment,
  lceStateTypeInfo: LceStateTypeInfo
): Boolean {
  return this.kind == TypeKind.DECLARED
    && processingEnv.typeUtils.isSameErasedType(this, Observable::class.java, processingEnv.elementUtils)
    && (this as DeclaredType).typeArguments.size == 1
    && processingEnv.typeUtils.isSameErasedType(this.typeArguments.single(), lceStateTypeInfo.type)
}

