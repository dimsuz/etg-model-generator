package com.github.dimsuz.modelgenerator

object ModelGenerator {
  fun <T> createModel(modelOperations: ModelOperations<Any>, schedulingSettings: SchedulingSettings): T {
    throw IllegalStateException("must not be called directly, has annotation processing failed?")
  }
}
