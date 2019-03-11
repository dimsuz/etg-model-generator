package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.annotation.GenerateReducingImplementation
import com.github.dimsuz.modelgenerator.model.ReactiveModel
import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.LceStateTypeInfo
import com.github.dimsuz.modelgenerator.processor.entity.Left
import com.github.dimsuz.modelgenerator.processor.entity.Right
import com.github.dimsuz.modelgenerator.processor.entity.flatMap
import com.github.dimsuz.modelgenerator.processor.entity.fold
import com.github.dimsuz.modelgenerator.processor.util.error
import com.github.dimsuz.modelgenerator.processor.util.isAssignable
import com.squareup.kotlinpoet.asClassName
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException

class ModelGeneratorProcessor : AbstractProcessor() {

  override fun getSupportedAnnotationTypes(): Set<String> {
    return listOf(
      GenerateReducingImplementation::class.java
    ).mapTo(mutableSetOf()) { it.canonicalName }
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latest()
  }

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    for (element in roundEnv.getElementsAnnotatedWith(GenerateReducingImplementation::class.java)) {
      if (element.kind != ElementKind.INTERFACE) {
        processingEnv.messager.error("${GenerateReducingImplementation::class.java.simpleName} can only be applied to interfaces")
        return true
      }

      findLceType(element)
        .flatMap { lceStateTypeInfo ->
          findModelSupertype(element).flatMap { supertype ->
            val modelDesc = findReactiveProperties(processingEnv, lceStateTypeInfo, element as TypeElement, supertype)
            modelDesc
              .flatMap { desc ->
                generateModelOperations(processingEnv, desc)
                  .flatMap { operationsClass ->
                    generateModelImplementation(
                      processingEnv,
                      desc,
                      lceStateTypeInfo,
                      operationsClass
                    )
                  }
                  .flatMap { modelTypeSpec -> generateFactoryExtension(desc, modelTypeSpec, processingEnv) }
              }
          }
        }
        .fold({ processingEnv.messager.error(it) }, {})
    }
    return true
  }

  private fun findModelSupertype(element: Element): Either<String, TypeElement> {
    val baseClassType = try {
      element.getAnnotation(GenerateReducingImplementation::class.java).baseClass
      throw RuntimeException("expected ${MirroredTypeException::class.java.simpleName} to be thrown")
    } catch (e: MirroredTypeException) {
      e.typeMirror
    }
    return if (!processingEnv.typeUtils.isAssignable(
        baseClassType,
        ReactiveModel::class.java,
        processingEnv.elementUtils
      )
    ) {
      Left("baseClass must implement ${ReactiveModel::class.java.simpleName} interface. Found: $baseClassType!")
    } else {
      Right((baseClassType as DeclaredType).asElement() as TypeElement)
    }
  }

  private fun findLceType(element: Element): Either<String, LceStateTypeInfo> {
    val lceStateClassType = try {
      element.getAnnotation(GenerateReducingImplementation::class.java).lceState
      throw RuntimeException("expected ${MirroredTypeException::class.java.simpleName} to be thrown")
    } catch (e: MirroredTypeException) {
      e.typeMirror
    }
    val typeElement = (lceStateClassType as DeclaredType).asElement() as TypeElement
    return if (typeElement.typeParameters.size != 1) {
      Left("lceState class must have exactly 1 type parameter, it has ${typeElement.typeParameters.size}")
    } else {
      Right(
        LceStateTypeInfo(
          element = typeElement,
          className = typeElement.asClassName()
        )
      )
    }
  }

}
