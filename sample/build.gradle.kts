plugins {
  application
  kotlin("kapt")
}

application {
  mainClassName = "com.github.dimsuz.modelgenerator.sample.SampleModelKt"
}

val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":runtime"))
  implementation(deps.getValue("rxJava"))

  kapt(project(":processor"))
}
