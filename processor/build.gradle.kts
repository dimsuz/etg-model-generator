val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":annotations"))
  implementation(deps.getValue("rxJava"))
}

