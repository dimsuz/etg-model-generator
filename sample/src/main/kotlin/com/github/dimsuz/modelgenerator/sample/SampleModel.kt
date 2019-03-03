package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.annotation.ReactiveModel
import io.reactivex.Observable

@ReactiveModel
interface SampleModel {
  fun testState(): Observable<LceState<Unit>>
  fun testNonLceState(): Observable<String>

  fun testNonReactive(): String
}

fun main() {
  println("Hello, world!")
}
