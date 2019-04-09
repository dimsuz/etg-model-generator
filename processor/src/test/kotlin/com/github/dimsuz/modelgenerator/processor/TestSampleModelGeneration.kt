package com.github.dimsuz.modelgenerator.processor

class TestSampleModelGeneration : APTest("com.github.dimsuz.modelgenerator") {
  fun testSampleModelGeneration() {
    testProcessor(
      AnnotationProcessor(
        sourceFiles = listOf("SampleModel.java"),
        expectedFiles = listOf(
          "ModelGeneratorExtensions.kt.txt",
          "SampleModelImpl.kt.txt",
          "SampleModelOperations.kt.txt"
        ),
        processor = ModelGeneratorProcessor()
      )
    )
  }
}
