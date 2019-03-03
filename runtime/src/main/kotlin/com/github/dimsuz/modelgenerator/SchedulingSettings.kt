package com.github.dimsuz.modelgenerator

import io.reactivex.Scheduler

interface SchedulingSettings {
  val uiScheduler: Scheduler
  fun checkIsOnUiThread()
}
