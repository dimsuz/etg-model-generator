package com.github.dimsuz.modelgenerator.lcestate

interface LceStateFactory<S> {
  fun createLceContent(content: Any): S
  fun createLceError(error: Any): S
  fun createLceLoading(): S
}
