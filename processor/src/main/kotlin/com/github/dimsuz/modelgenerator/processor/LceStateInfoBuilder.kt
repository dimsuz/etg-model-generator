package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.annotation.LceContentConstructor
import com.github.dimsuz.modelgenerator.annotation.LceErrorConstructor
import com.github.dimsuz.modelgenerator.annotation.LceLoadingConstructor
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.LceStateTypeInfo
import com.github.dimsuz.modelgenerator.processor.entity.Left
import com.github.dimsuz.modelgenerator.processor.entity.Right
import com.github.dimsuz.modelgenerator.processor.entity.flatMap
import com.github.dimsuz.modelgenerator.processor.entity.map
import com.github.dimsuz.modelgenerator.processor.entity.mapLeft
import com.github.dimsuz.modelgenerator.processor.util.enclosingPackageName
import com.github.dimsuz.modelgenerator.processor.util.firstOrFailure
import com.github.dimsuz.modelgenerator.processor.util.isNotNull
import com.github.dimsuz.modelgenerator.processor.util.isSameType
import com.squareup.kotlinpoet.MemberName
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier

internal fun buildLceStateInfo(
  roundEnv: RoundEnvironment,
  processingEnv: ProcessingEnvironment
): Either<String, LceStateTypeInfo> {
  // TODO check count, should be only 1 of each annotation
  val contentElement = roundEnv.getElementsAnnotatedWith(LceContentConstructor::class.java)
    .firstOrFailure().mapLeft { "lce content constructor not found" }
    .flatMap {
      when (it.kind) {
        ElementKind.METHOD -> checkContentConstructorUsable(it as ExecutableElement)
        else -> Left("lce content constructor must be a function")
      }
    }
  val errorElement = roundEnv.getElementsAnnotatedWith(LceErrorConstructor::class.java)
    .firstOrFailure().mapLeft { "lce error constructor not found" }
    .flatMap {
      when (it.kind) {
        ElementKind.METHOD -> checkErrorConstructorUsable(it as ExecutableElement, processingEnv)
        else -> Left("lce error constructor must be a function")
      }
    }
  val loadingElement = roundEnv.getElementsAnnotatedWith(LceLoadingConstructor::class.java)
    .firstOrFailure().mapLeft { "lce loading constructor not found" }
    .flatMap {
      when (it.kind) {
        ElementKind.METHOD -> checkLoadingConstructorUsable(it as ExecutableElement)
        else -> Left("lce loading constructor must be a function")
      }
    }

  return contentElement
    .flatMap { contentConstructor ->
      errorElement.flatMap { errorConstructor ->
        loadingElement.map { loadingConstructor ->
          val lceType = contentConstructor.returnType
          LceStateTypeInfo(
            lceType,
            MemberName(contentConstructor.enclosingPackageName, contentConstructor.simpleName.toString()),
            MemberName(errorConstructor.enclosingPackageName, errorConstructor.simpleName.toString()),
            MemberName(loadingConstructor.enclosingPackageName, loadingConstructor.simpleName.toString())
          )
        }
      }
    }
}

private fun checkContentConstructorUsable(element: ExecutableElement): Either<String, ExecutableElement> {
  if (element.modifiers.none { it == Modifier.STATIC }) {
    return Left("expected content constructor to be a top-level function")
  }
  return if (element.parameters.size > 1)
    Left("expected exactly 1 parameter on lce content constructor") else Right(element)
}

private fun checkLoadingConstructorUsable(element: ExecutableElement): Either<String, ExecutableElement> {
  if (element.modifiers.none { it == Modifier.STATIC }) {
    return Left("expected loading constructor to be a top-level function")
  }
  return if (element.parameters.size > 1
    || (element.parameters.size == 1 && element.parameters.single().isNotNull)
  ) {
    Left("expected lce loading constructor to have no parameters or one nullable parameter")
  } else {
    Right(element)
  }
}

private fun checkErrorConstructorUsable(
  element: ExecutableElement,
  processingEnv: ProcessingEnvironment
): Either<String, ExecutableElement> {
  if (element.modifiers.none { it == Modifier.STATIC }) {
    return Left("expected error constructor to be a top-level function")
  }
  return if (element.parameters.size > 2
    || (element.parameters.size >= 1 && !processingEnv.typeUtils.isSameType(
      element.parameters.first().asType(),
      Throwable::class.java,
      processingEnv.elementUtils
    ))
    || (element.parameters.size == 2 && (element.parameters[1].isNotNull))
  ) {
    Left("expected lce error constructor to be of shape (Throwable, T?)")
  } else {
    Right(element)
  }
}
