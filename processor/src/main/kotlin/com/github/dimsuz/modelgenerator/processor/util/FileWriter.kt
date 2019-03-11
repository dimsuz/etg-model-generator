package com.github.dimsuz.modelgenerator.processor.util

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.Left
import com.github.dimsuz.modelgenerator.processor.entity.Right
import com.squareup.kotlinpoet.FileSpec
import java.io.File
import javax.annotation.processing.ProcessingEnvironment

internal fun writeFile(
  processingEnv: ProcessingEnvironment,
  fileSpec: FileSpec
): Either<String, Unit> {
  val kaptKotlinGeneratedDir = processingEnv.options["kapt.kotlin.generated"]
    ?: return Left("Can't find the target directory for generated Kotlin files.")
  File(
    "$kaptKotlinGeneratedDir/${fileSpec.packageName.replace(".", "/")}",
    fileSpec.name
  ).apply {
    parentFile.mkdirs()
    writeText(fileSpec.toBuilder().indent(" ").build().toString())
  }
  return Right(Unit)
}
