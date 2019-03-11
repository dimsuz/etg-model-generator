package com.github.dimsuz.modelgenerator.sample

import com.github.dimsuz.modelgenerator.ModelGenerator
import com.github.dimsuz.modelgenerator.ModelOperations
import com.github.dimsuz.modelgenerator.lcestate.LceStateFactory
import io.reactivex.Observable

fun ModelGenerator.createModel(operations: ModelOperations<Any>, lceStateFactory: LceStateFactory<LceState<*>>): SampleModel {
  return object : SampleModel {
    override fun fetchMovieDetails(movieId: String) {
      val content: LceState<String> = lceStateFactory.createLceContent(movieId) as LceState<String>
      TODO("not implemented")
    }

    override fun fetchMovieDetailsState(userId: String): Observable<LceState<MovieDetails>> {
      TODO("not implemented")
    }

    override fun movieList(userId: String, filter: Filter) {
      TODO("not implemented")
    }

    override fun movieListState(): Observable<LceState<List<Movie>>> {
      TODO("not implemented")
    }

    override fun getFriendsList() {
      TODO("not implemented")
    }

    override fun getFriendsListState(): Observable<LceState<Unit>> {
      TODO("not implemented")
    }

    override fun getFriendsListComplex(param: Map<List<Int>, List<MovieDetails>>) {
      TODO("not implemented")
    }

    override fun getFriendsListComplexState(): Observable<LceState<List<Set<String>>>> {
      TODO("not implemented")
    }

    override fun findChatMessages() {
      TODO("not implemented")
    }

    override fun findChatMessagesState(): Observable<LceState<Map<String, List<Set<String>>>>> {
      TODO("not implemented")
    }

    override fun testNonLceState(): Observable<String> {
      TODO("not implemented")
    }

    override fun testNonReactive(): String {
      TODO("not implemented")
    }

    override fun testNonReactiveWithParams(userId: String, details: MovieDetails): String {
      TODO("not implemented")
    }

    override fun testNonReactiveWithParamsVoid(details: MovieDetails) {
      TODO("not implemented")
    }

  }
}
