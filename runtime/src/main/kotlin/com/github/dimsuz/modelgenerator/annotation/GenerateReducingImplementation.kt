package com.github.dimsuz.modelgenerator.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateReducingImplementation(val baseClass: KClass<*>)
