package com.github.dimsuz.modelgenerator.processor.util

internal fun <T> Iterable<T>.firstOrFailure(): Either<String, T> {
  return this.firstOrNull()?.let { Right(it) } ?: Left("collection is empty")
}

