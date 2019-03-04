package com.github.dimsuz.modelgenerator.processor.util

import com.github.dimsuz.modelgenerator.processor.entity.Either
import com.github.dimsuz.modelgenerator.processor.entity.Left
import com.github.dimsuz.modelgenerator.processor.entity.Right

internal fun <T> Iterable<T>.firstOrFailure(): Either<String, T> {
  return this.firstOrNull()?.let { Right(it) } ?: Left("collection is empty")
}

