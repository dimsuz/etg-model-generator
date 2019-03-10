package com.github.dimsuz.modelgenerator.processor.entity

import com.squareup.kotlinpoet.MemberName
import javax.lang.model.type.TypeMirror

internal data class LceStateTypeInfo(
  val type: TypeMirror,
  val contentConstructor: MemberName,
  val errorConstructor: MemberName,
  val loadingConstructor: MemberName
)
