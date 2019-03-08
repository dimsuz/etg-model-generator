package com.github.dimsuz.modelgenerator.sample.modelimpl

import com.github.dimsuz.modelgenerator.model.ReactiveModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

interface AppSchedulers

/**
 * Реактивная модель, которая запускает последовательность команд в режиме serial execution, то есть
 * одна за другой - по мере поступления (FIFO)
 */
abstract class SerialReactiveModel<StateType, RequestType, ActionType>(
  schedulers: AppSchedulers,
  logger: () -> String
): ReactiveModel<StateType, RequestType, ActionType> {

  protected val stateChanges: Observable<StateType>
  private val requestStream = PublishSubject.create<RequestType>()
  // TODO document why it is private and shouldn't leak
  @Volatile
  private var lastState: StateType? = null

  init {
    stateChanges = requestStream
      .concatMap { request ->
        val state = lastState ?: createInitialState()
        createCommand(request, state)
          .map { r -> reduceState(state, r) }
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

  // TODO document that Observable can emit no result - that's ok, means no command needed for this request
  fun createCommand(request: RequestType, state: StateType): Observable<ActionType> {
    return bindRequest(request, state)
  }
}
