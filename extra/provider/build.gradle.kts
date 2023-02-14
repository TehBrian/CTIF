plugins {
  id("application")
  id("com.github.ben-manes.versions") version "0.45.0"
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

application {
  mainClass.set("dev.tehbrian.ctif.converter.Main")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.im4java:im4java:1.4.0")
  implementation("com.beust:jcommander:1.82")
  implementation("com.github.kokorin.jaffree:jaffree:2022.06.03")
  implementation("io.javalin:javalin:5.3.1")
  implementation("org.jspecify:jspecify:0.3.0")

  implementation("org.apache.logging.log4j:log4j-api:2.19.0")
  runtimeOnly("org.apache.logging.log4j:log4j-core:2.19.0")
  runtimeOnly("org.slf4j:slf4j-simple:2.0.6")
}
