package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.annotation.LceContentConstructor
import com.github.dimsuz.modelgenerator.annotation.LceErrorConstructor
import com.github.dimsuz.modelgenerator.annotation.LceLoadingConstructor
import com.github.dimsuz.modelgenerator.annotation.ReactiveModel
import com.github.dimsuz.modelgenerator.processor.util.fold
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
    return listOf(
      ReactiveModel::class.java,
      LceContentConstructor::class.java,
      LceErrorConstructor::class.java,
      LceLoadingConstructor::class.java
    ).mapTo(mutableSetOf()) { it.canonicalName }
  }

  override fun getSupportedSourceVersion(): SourceVersion {
    return SourceVersion.latest()
  }

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    for (element in roundEnv.getElementsAnnotatedWith(ReactiveModel::class.java)) {
      val lceStateInfo = buildLceStateInfo(roundEnv)
        .fold({ logger.error(it); null }, { it })
        ?: return true

      if (element.kind != ElementKind.INTERFACE) {
        logger.error("${ReactiveModel::class.java.simpleName} can only be applied to interfaces")
        return true
      }
    }
    return true
  }

}
