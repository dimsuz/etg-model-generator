package com.github.dimsuz.modelgenerator.sample

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal class SampleOperationsImpl(private val userIdProvider: () -> String) : SampleModelOperations {
  override fun createFetchMovieDetailsOperation(movieId: String, state: SampleModelImpl.State): Single<MovieDetails> {
    return Single.just(MovieDetails(movieId, "Comedy"))
  }

  override fun createMovieListOperation(
    userId: String,
    filter: Filter?,
    state: SampleModelImpl.State
  ): Single<List<Movie>> {
    return Single.just(emptyList())
  }

  override fun createGetFriendsListOperation(state: SampleModelImpl.State): Completable {
    return Completable.complete()
  }

  override fun createGetFriendsListComplexOperation(
    param: Map<List<Int>, List<MovieDetails>>,
    state: SampleModelImpl.State
  ): Single<List<Set<String>>> {
    return Single.just(emptyList())
  }

  override fun createFindChatMessagesOperation(state: SampleModelImpl.State): Single<Map<String, List<Set<String>>>> {
    return Single.error(NotImplementedError("chat messages not implemented"))
  }

  override fun testNonLceState(stateChanges: Observable<SampleModelImpl.State>): Observable<String> {
    return Observable.just("42")
  }

  override fun testNonLceStateNullableParam(
    stateChanges: Observable<SampleModelImpl.State>,
    filter: Filter?,
    skipCache: Boolean?
  ): Observable<String> {
    return Observable.just("42")
  }

  override fun testNonReactive(stateChanges: Observable<SampleModelImpl.State>): String {
    return "8"
  }

  override fun testNonReactiveWithParams(
    stateChanges: Observable<SampleModelImpl.State>,
    userId: String,
    details: MovieDetails
  ): String {
    return "9"
  }

  override fun testNonReactiveWithParamsVoid(stateChanges: Observable<SampleModelImpl.State>, details: MovieDetails) {

  }

  override fun testNonReactiveWithParamsNullable(stateChanges: Observable<SampleModelImpl.State>, skipCache: Boolean?) {
  }

  override fun testNonReactiveWithNullableReturn(
    stateChanges: Observable<SampleModelImpl.State>,
    userId: String,
    skipCache: Boolean?
  ): Filter? {
    return null
  }

}
