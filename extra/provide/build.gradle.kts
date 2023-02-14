plugins {
  id("application")
  id("com.github.ben-manes.versions") version "0.45.0"
}

group = "dev.tehbrian.ctif"
version = "0.1.0"

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

application {
  mainClass.set("dev.tehbrian.ctif.provide.Main")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation("pl.asie.ctif:ctif-convert:0.2.0")
  implementation("org.im4java:im4java:1.4.0")
  implementation("com.beust:jcommander:1.82")
  implementation("com.github.kokorin.jaffree:jaffree:2022.06.03")
  implementation("io.javalin:javalin:5.3.1")
  implementation("org.jspecify:jspecify:0.3.0")

  implementation("org.apache.logging.log4j:log4j-api:2.19.0")
  runtimeOnly("org.apache.logging.log4j:log4j-core:2.19.0")
  runtimeOnly("org.slf4j:slf4j-simple:2.0.6")
}
