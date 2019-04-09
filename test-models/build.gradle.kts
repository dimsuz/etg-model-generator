plugins {
  kotlin("kapt")
}

val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":runtime"))
  implementation(deps.getValue("rxJava"))
  implementation(deps.getValue("autoservice"))

  kapt(deps.getValue("autoservice"))
  kapt(project(":processor"))
}
