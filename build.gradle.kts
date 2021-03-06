import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  var versions: Map<String, String> by extra
  @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
  var deps: Map<String, String> by extra

  versions = mapOf(
    "kotlin" to "1.3.21", // see also plugin block below
    "rxJava" to "2.2.7",
    "kotlinpoet" to "1.1.0",
    "autoservice" to "1.0-rc4",
    "junit" to "4.12",
    "compiletesting" to "0.15"
  )
  @Suppress("UNUSED_VALUE")
  deps = mapOf(
    "rxJava" to "io.reactivex.rxjava2:rxjava:${versions["rxJava"]}",
    "kotlinpoet" to "com.squareup:kotlinpoet:${versions["kotlinpoet"]}",
    "autoservice" to "com.google.auto.service:auto-service:${versions["autoservice"]}",
    "junit" to "junit:junit:${versions["junit"]}",
    "compiletesting" to "com.google.testing.compile:compile-testing:${versions["compiletesting"]}"
  )
}

plugins {
  java
  kotlin("jvm") version "1.3.21"
}

allprojects {
  group = "com.github.dimsuz"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
    jcenter()
  }
}

subprojects {
  apply {
    plugin("kotlin")
  }
  dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("junit", "junit", "4.12")
  }
  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
  }
}
