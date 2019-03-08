package com.github.dimsuz.modelgenerator.model

import io.reactivex.Observable

interface ReactiveModel<S, R, A> {
  fun scheduleRequest(request: R)
  fun bindRequest(request: R, state: S): Observable<A>
  fun reduceState(previousState: S, action: A): S
}
