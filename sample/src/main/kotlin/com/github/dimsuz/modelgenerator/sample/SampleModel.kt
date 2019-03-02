package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.annotation.ReactiveModel

@ReactiveModel
interface SampleModel {
  fun test()
}

fun main() {
  val obj = object : SampleModel {
    override fun test() {
    }

  }
  println("Hello, world!")
}
