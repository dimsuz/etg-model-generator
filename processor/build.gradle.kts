plugins {
  `maven-publish`
  signing
}

val deps: Map<String, String> by rootProject.extra

group = property("groupId")!!
version = property("versionName")!!

base {
  archivesBaseName = "etg-model-generator-processor"
}

dependencies {
  implementation(project(":runtime"))
  implementation(deps.getValue("kotlinpoet"))
  implementation(kotlin("reflect"))
}

tasks.register<Jar>("sourcesJar") {
  from(sourceSets.main.get().allJava)
  archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
  from(tasks.javadoc)
  archiveClassifier.set("javadoc")
}

publishing {
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      credentials {
        username = property("NEXUS_USERNAME") as String
        password = property("NEXUS_PASSWORD") as String
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])

      artifactId = property("artifactId") as String

      pom {
        name.set(property("pom.name") as String)
        description.set(property("pom.description") as String)
        url.set(property("pom.url") as String)
        licenses {
          license {
            name.set(property("pom.license.name") as String)
            url.set(property("pom.license.url") as String)
          }
        }
        developers {
          developer {
            id.set(property("pom.developer.id") as String)
            name.set(property("pom.developer.name") as String)
          }
        }
        scm {
          connection.set(property("pom.scm.connection") as String)
          developerConnection.set(property("pom.scm.devconnection") as String)
          url.set(property("pom.scm.url") as String)
        }
      }
    }
  }
}

signing {
  sign(publishing.publications["maven"])
}
