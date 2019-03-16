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

  fun movieList(userId: String, filter: Filter) // several args
  fun movieListState(): Observable<LceState<List<Movie>>> // generics in content state

  fun getFriendsList() // no args
  fun getFriendsListState(): Observable<LceState<Unit>>

  fun getFriendsListComplex(param: Map<List<Int>, List<MovieDetails>>) // complex params type
  fun getFriendsListComplexState(): Observable<LceState<List<Set<String>>>> // complex content type

  fun findChatMessages()
  fun findChatMessagesState(): Observable<LceState<Map<String, List<Set<String>>>>> // complex content type with map

  fun testNonLceState(): Observable<String>

  fun testNonReactive(): String
  fun testNonReactiveWithParams(userId: String, details: MovieDetails): String
  fun testNonReactiveWithParamsVoid(details: MovieDetails)
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

fun main() {
  val schedulers = object : AppSchedulers { }
  val model = ModelGenerator.createModel(
    SampleOperationsImpl { "userId" },
    LceStateFactoryImpl(),
    schedulers,
    { println(it) })

  println("model is created: $model")

  testModel(model)
}
