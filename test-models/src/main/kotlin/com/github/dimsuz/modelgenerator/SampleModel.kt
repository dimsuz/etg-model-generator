package com.github.dimsuz.modelgenerator

import com.github.dimsuz.modelgenerator.annotation.GenerateReducingImplementation
import io.reactivex.Observable

@GenerateReducingImplementation(
  baseClass = BaseTestModel::class,
  lceState = LceState::class
)
interface SampleModel {
  fun test(): Observable<LceState<String>>
  fun test(param: String)
  fun testNonLceState(): Observable<String>
  fun testNonReactive(): String
  fun testNonReactiveWithParams(userId: String, details: String): String
  fun testNonReactiveWithParamsUnit(details: String)
}
