val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":runtime"))
  implementation(deps.getValue("kotlinpoet"))
  implementation(kotlin("reflect"))
}

