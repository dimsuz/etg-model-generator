package com.github.dimsuz.modelgenerator.processor

import javax.annotation.processing.Processor

data class AnnotationProcessor(
  val sourceFiles: List<String>,
  val expectedFiles: List<String>? = null,
  val processor: Processor,
  val errorMessage: String? = null
)
