package com.github.dimsuz.modelgenerator.sample

data class LceState<C>(
  val isLoading: Boolean,
  val content: C?,
  val error: Throwable?
) {
  companion object {
    @Suppress("FunctionName", "FunctionNaming") // constructor function, caps ok
    fun <C> Loading(content: C? = null): LceState<C> {
      return LceState(isLoading = true, content = content, error = null)
    }

    @Suppress("FunctionName", "FunctionNaming") // constructor function, caps ok
    fun <C> Content(content: C): LceState<C> {
      return LceState(isLoading = false, content = content, error = null)
    }

    @Suppress("FunctionName", "FunctionNaming") // constructor function, caps ok
    fun <C> Error(error: Throwable, content: C? = null): LceState<C> {
      return LceState(isLoading = false, content = content, error = error)
    }
  }
}
