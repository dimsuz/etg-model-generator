package com.github.dimsuz.modelgenerator

object ModelGenerator {
  fun <T> createModel(operations: ModelOperations<Any>): T {
    throw IllegalStateException("must not be called directly, has annotation processing failed?")
  }
}
