val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":annotations"))
  implementation(project(":runtime"))
  implementation(deps.getValue("rxJava"))
  implementation(deps.getValue("kotlinpoet"))
}

