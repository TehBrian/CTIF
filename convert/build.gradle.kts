plugins {
  id("java-library")
  id("maven-publish")
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "pl.asie.ctif"
version = "0.1.0"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.im4java:im4java:1.4.0")
  implementation("com.beust:jcommander:1.82")
  implementation("org.jspecify:jspecify:0.3.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))

  sourceSets["main"].java {
    srcDir("src")
  }
}

tasks {
  jar {
    manifest {
      attributes["Main-Class"] = "pl.asie.ctif.convert.Main"
    }
  }

  build {
    dependsOn(shadowJar)
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifactId = "convert"
      from(components["java"])

      pom {
        name.set(rootProject.name)
        url.set("https://github.com/TehBrian/CTIF")
      }
    }
  }
}
