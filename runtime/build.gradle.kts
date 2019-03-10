val deps: Map<String, String> by rootProject.extra

dependencies {
  api(deps.getValue("rxJava"))
}

