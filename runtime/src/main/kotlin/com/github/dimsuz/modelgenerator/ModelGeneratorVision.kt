package com.github.dimsuz.modelgenerator

// generated by processor
fun ModelGenerator.createModel(
  modelOperations: SampleModelOperations,
  schedulingSettings: SchedulingSettings,
  additionalArg: String
): MovieModel {
  TODO()
}

// generated by processor
interface SampleModelOperations : ModelOperations<MovieModel> {
  fun createMovieDetailsCommand(userId: String): List<String>
}

class Repository
class MovieModel

// provided by client
private class SampleModelOperationsImpl(
  val repository: Repository
) : SampleModelOperations {
  override fun createMovieDetailsCommand(userId: String): List<String> {
    TODO()
  }

}

interface Provider<T> {
  fun get(): T
}

// provided by client
class ModelProvider(
  private val repository: Repository,
  private val schedulingSettings: SchedulingSettings
): Provider<MovieModel> {
  override fun get(): MovieModel {
    return ModelGenerator.createModel(SampleModelOperationsImpl(repository), schedulingSettings, "hello")
  }
}

