package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.annotation.GenerateReducingImplementation
import com.github.dimsuz.modelgenerator.sample.modelimpl.SerialReactiveModel
import io.reactivex.Observable

@GenerateReducingImplementation(baseClass = SerialReactiveModel::class)
interface SampleModel {

  fun fetchMovieDetails(movieId: String)
  fun fetchMovieDetailsState(userId: String): Observable<LceState<MovieDetails>>

  fun movieList(userId: String, filter: Filter) // several args
  fun movieListState(): Observable<LceState<List<Movie>>> // generics in content state

  fun getFriendsList() // no args
  fun getFriendsListState(): Observable<LceState<Unit>>

  fun getFriendsListComplex(param: Map<List<Int>, List<MovieDetails>>) // complex params type
  fun getFriendsListComplexState(): Observable<LceState<List<Set<String>>>> // complex content type

  fun testNonLceState(): Observable<String>

  fun testNonReactive(): String
  fun testNonReactiveWithParams(userId: String, details: MovieDetails): String
  fun testNonReactiveWithParamsVoid(details: MovieDetails)
}

data class Filter(val query: String)
data class Movie(val id: String)
data class MovieDetails(val id: String, val genre: String)

fun main() {
  println("Hello, world!")
}
