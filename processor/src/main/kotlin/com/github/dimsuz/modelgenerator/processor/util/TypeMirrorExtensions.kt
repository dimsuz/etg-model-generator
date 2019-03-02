package com.github.dimsuz.modelgenerator.processor.util

import javax.lang.model.type.TypeMirror

internal fun TypeMirror.hasClass(c: Class<*>): Boolean {
  return this.toString() == c.name
}
