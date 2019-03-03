package com.github.dimsuz.modelgenerator.processor.util

sealed class Either<out L, out R>

data class Left<out T>(val value: T) : Either<T, Nothing>()
data class Right<out T>(val value: T) : Either<Nothing, T>()

inline fun <L, R, T> Either<L, R>.fold(left: (L) -> T, right: (R) -> T): T =
  when (this) {
    is Left -> left(value)
    is Right -> right(value)
  }

inline fun <L, R, T> Either<L, R>.flatMap(f: (R) -> Either<L, T>): Either<L, T> =
  fold({ this as Left }, f)

inline fun <L, R, T> Either<L, R>.map(f: (R) -> T): Either<L, T> =
  flatMap { Right(f(it)) }

inline fun <L, R, T> Either<L, R>.mapLeft(f: (L) -> T): Either<T, R> =
  fold({ Left(f(it)) }, { this as Right })

fun <L, R> List<Either<L, R>>.join(): Either<L, List<R>> {
  if (isEmpty()) return Right(emptyList())
  val initial = first().map { mutableListOf(it) }
  return if (size == 1) initial else {
    drop(1).fold(initial) { acc, either ->
      acc.flatMap { list -> either.map { list.add(it); list } }
    }
  }
}
