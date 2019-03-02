plugins {
  application
  kotlin("kapt")
}

application {
  mainClassName = "com.github.dimsuz.modelgenerator.sample.SampleModelKt"
}

dependencies {
  implementation(project(":runtime"))

  kapt(project(":processor"))
}
