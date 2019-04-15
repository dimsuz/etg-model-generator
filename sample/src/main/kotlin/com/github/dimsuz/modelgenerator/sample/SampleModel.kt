package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.ModelGenerator
import com.github.dimsuz.modelgenerator.annotation.GenerateReducingImplementation
import com.github.dimsuz.modelgenerator.sample.modelimpl.AppSchedulers
import com.github.dimsuz.modelgenerator.sample.modelimpl.SerialReactiveModel
import io.reactivex.Observable
import io.reactivex.observers.TestObserver

@GenerateReducingImplementation(
  baseClass = SerialReactiveModel::class,
  lceState = LceState::class
)
interface SampleModel {

  fun fetchMovieDetails(movieId: String)
  fun fetchMovieDetailsState(userId: String): Observable<LceState<MovieDetails>>

  fun movieList(userId: String, filter: Filter?) // several args
  fun movieListState(): Observable<LceState<List<Movie>>> // generics in content state

  fun getFriendsList() // no args
  fun getFriendsListState(): Observable<LceState<Unit>>

  fun getFriendsListComplex(param: Map<List<Int>, List<MovieDetails>>) // complex params type
  fun getFriendsListComplexState(): Observable<LceState<List<Set<String>>>> // complex content type

  fun findChatMessages()
  fun findChatMessagesState(): Observable<LceState<Map<String, List<Set<String>>>>> // complex content type with map

  fun testNonLceState(): Observable<String>
  fun testNonLceStateNullableParam(filter: Filter?, skipCache: Boolean?): Observable<String>

  fun testNonReactive(): String
  fun testNonReactiveWithParams(userId: String, details: MovieDetails): String
  fun testNonReactiveWithParamsVoid(details: MovieDetails)
  fun testNonReactiveWithParamsNullable(skipCache: Boolean?)
  fun testNonReactiveWithNullableReturn(userId: String, skipCache: Boolean?): Filter?
}

data class Filter(val query: String)
data class Movie(val id: String)
data class MovieDetails(val id: String, val genre: String)

fun testModel(model: SampleModel) {
  val movieDetailsObserver: TestObserver<LceState<MovieDetails>> = TestObserver()
  model.fetchMovieDetailsState("user-id").subscribe(movieDetailsObserver)
  model.fetchMovieDetails("example-test-movie-id")
  movieDetailsObserver.awaitCount(2, { println("await failed" ) }, 2000)
  movieDetailsObserver.assertValues(
    LceState.Loading(),
    LceState.Content(MovieDetails("example-test-movie-id", "Comedy"))
  )

  val friendListObserver: TestObserver<LceState<Unit>> = TestObserver()
  model.getFriendsListState().subscribe(friendListObserver)
  model.getFriendsList()
  friendListObserver.awaitCount(2, { println("await failed" ) }, 2000)
  friendListObserver.assertValues(
    LceState.Loading(),
    LceState.Content(Unit)
  )

  val chatMessagesObserver: TestObserver<LceState<Map<String, List<Set<String>>>>> = TestObserver()
  model.findChatMessagesState().subscribe(chatMessagesObserver)
  model.findChatMessages()
  chatMessagesObserver.awaitCount(2, { println("await failed" ) }, 2000)
  chatMessagesObserver.assertValueAt(0, LceState.Loading())
  chatMessagesObserver.assertValueAt(1) {
    // NotImplementedError has no equals() implementation, have to manual-check
    it.error != null && it.error is NotImplementedError && it.error.message == "chat messages not implemented"
  }

  println("tests passed")
}

// Tests that after emitting L+C events, corresponding field in State gets cleared back to null,
// so that future subscribers won't see it laying around: only ones subscribed during L+C state changes will
// be able to see it.
//
// This tests for the fix of "unwanted C/E state" situation which often manifested, for example:
//
// * model has 'lceProperty1' and 'lceProperty2'
// * presenter A subscribes to 'lceProperty1', watches it to change from L to E state
// * presenter B is launched, subscribes to 'lceProperty1'. It's in E state, stored in model's State,
//   but no changes happened to it, since B subscribed, so B doesn't receive anything
// * presenter C is launched, subscribes to 'lceProperty2'. It emits L, model's 'State' changes
// * This causes presenter B to receive 'lceProperty1' E state! without receiving its L state.
//   This situation often causes problems when presenter B is not ready for this behavior and expects E to come after L
// * Note that presenter A won't get E in this case, because distinctUntilChanged() inside model will
//   discard it for presenter A, since it subscribed earlier and observed L+E happening. Not so with presenter B
//
// solution for this: model must clean up lce state after receiving C or E. They will be observed by whoever
// was subscribed, but not by new subscribers
fun testClearsStateAfterEmittingContentOrError(model: SampleModel) {
  val friendListObserver: TestObserver<LceState<Unit>> = TestObserver()
  model.getFriendsListState().subscribe(friendListObserver)
  model.getFriendsList()
  // as per description above, this is presenter A, receiving L + C
  friendListObserver.awaitCount(2, { println("await failed" ) }, 2000)

  // this is presenter B, subscribing to changes of 'lceProperty1' (getFriendsListState)
  val friendListObserver1: TestObserver<LceState<Unit>> = TestObserver()
  model.getFriendsListState().subscribe(friendListObserver1)

  // this is presenter C, triggering changes to 'lceProperty2' (fetchMovieDetailsState)
  val movieDetailsObserver: TestObserver<LceState<MovieDetails>> = TestObserver()
  model.fetchMovieDetailsState("user-id").subscribe(movieDetailsObserver)
  model.fetchMovieDetails("example-test-movie-id")
  movieDetailsObserver.awaitCount(2, { println("await failed" ) }, 2000)

  // Check that presenter C doesn't receive C-state to prevent described operation from happening
  friendListObserver1.assertNoValues()
}

private fun createModel(): SampleModel {
  val schedulers = object : AppSchedulers { }
  return ModelGenerator.createModel(
    SampleOperationsImpl { "userId" },
    LceStateFactoryImpl(),
    schedulers,
    { println(it) })
}

fun main() {
  testModel(createModel())
  testClearsStateAfterEmittingContentOrError(createModel())
}
