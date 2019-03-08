package com.github.dimsuz.modelgenerator.sample.modelimpl

import com.github.dimsuz.modelgenerator.model.ReactiveModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Реактивная модель, которая запускает последовательность команд в режиме serial execution, то есть
 * одна за другой - по мере поступления (FIFO)
 */
abstract class SerialReactiveModel<StateType, RequestType, ActionType>(
  initialState: StateType
): ReactiveModel<StateType, RequestType, ActionType> {

  protected val stateChanges: Observable<StateType>
  private val requestStream = PublishSubject.create<RequestType>()
  // TODO document why it is private and shouldn't leak
  @Volatile
  private var lastState: StateType = initialState

  init {
    stateChanges = requestStream
      .concatMap { request ->
        createCommand(request, lastState)
          .map { r ->
            reduceState(lastState, r)
          }
          .doOnNext { lastState = it }
      }
      .share()

    @Suppress("CheckResult") // нет необходимости вызывать dispose здесь
    stateChanges
      .subscribe(
        {
          //Timber.d("state updated to: $it")
        },
        {})
  }

  override fun scheduleRequest(request: RequestType) {
    requestStream.onNext(request)
  }

  override fun bindRequest(request: RequestType, state: StateType): Observable<ActionType> {
    return createCommand(request, state)
  }

  // TODO document that Observable can emit no result - that's ok, means no command needed for this request
  abstract fun createCommand(request: RequestType, state: StateType): Observable<ActionType>

  abstract override fun reduceState(previousState: StateType, action: ActionType): StateType
}
