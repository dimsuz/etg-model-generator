val versions: Map<String, String> by rootProject.extra
val deps: Map<String, String> by rootProject.extra

dependencies {
  implementation(project(":annotations"))
  implementation(project(":runtime"))
  implementation(deps.getValue("rxJava"))
  implementation(deps.getValue("kotlinpoet"))
  implementation(kotlin("reflect"))

  testImplementation("org.permissionsdispatcher:kompile-testing:0.1.2")
  testImplementation("com.squareup.okio:okio:2.1.0")
  testImplementation("commons-io:commons-io:2.6")
  testImplementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:${versions.getValue("kotlin")}")
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${versions.getValue("kotlin")}")
  testImplementation("org.assertj:assertj-core:3.11.1")
}

