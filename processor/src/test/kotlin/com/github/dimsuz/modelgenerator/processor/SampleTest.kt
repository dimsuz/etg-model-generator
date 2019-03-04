package com.github.dimsuz.modelgenerator.processor

import com.github.dimsuz.modelgenerator.processor.compiler.Compiler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SampleTest {
  @Rule
  @JvmField var temporaryFolder: TemporaryFolder = TemporaryFolder()

//  @Test
//  fun anotherTest() {
//    val call = KotlinCompilerCall(temporaryFolder.root)
//    call.inheritClasspath = true
//    call.addService(Processor::class, ModelGeneratorProcessor::class)
//    call.addKt("source.kt", """
//      |@com.github.dimsuz.modelgenerator.annotation.ReactiveModel
//      |class TestClass {
//      |}
//      |""".trimMargin())
//
//    val result = call.execute()
//    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
//    assertThat(result.systemErr).contains("can only be applied to interfaces")
//  }

  @Test
  fun testTriggersErrorWhenAppliedToClass() {

    val rootDir = createTempDir()
    rootDir.deleteOnExit()
    Compiler(
      rootDir
    )
      .withProcessors(ModelGeneratorProcessor())
      .addKotlin(
        "TestModel1.kt", """

interface TestModel1 {
}
      """.trimIndent()
      )
      .compile()
      .failed()
      .withErrorContaining("blah")
  }
}
