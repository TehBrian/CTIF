plugins {
  id("java")
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.im4java:im4java:1.4.0")
  implementation("com.beust:jcommander:1.82")
}

java.sourceSets["main"].java {
  srcDir("src")
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
