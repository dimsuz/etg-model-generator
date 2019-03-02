package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.annotation.ReactiveModel
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

class ModelGeneratorProcessor : AbstractProcessor() {
  private lateinit var logger: Logger

  override fun init(processingEnv: ProcessingEnvironment) {
    super.init(processingEnv)

    logger = Logger(processingEnv.messager)
  }

  override fun getSupportedAnnotationTypes(): Set<String> {
    return setOf(ReactiveModel::class.java.canonicalName)
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latest()
  }

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    for (element in roundEnv.getElementsAnnotatedWith(ReactiveModel::class.java)) {
      if (element.kind != ElementKind.CLASS) {
        logger.error("${ReactiveModel::class.java.simpleName} can only be applied to interfaces")
        return true
      }
    }
    return true
  }
}
