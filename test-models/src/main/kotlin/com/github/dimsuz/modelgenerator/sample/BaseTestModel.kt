package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.model.ReactiveModel
import io.reactivex.Observable

abstract class BaseTestModel<StateType, RequestType, ActionType> : ReactiveModel<StateType, RequestType, ActionType> {
  final override val stateChanges: Observable<StateType> = Observable.never()
  final override fun scheduleRequest(request: RequestType) = Unit
}
