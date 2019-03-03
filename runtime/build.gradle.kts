val deps: Map<String, String> by rootProject.extra

dependencies {
  api(project(":annotations"))
  implementation(deps.getValue("rxJava"))
}

