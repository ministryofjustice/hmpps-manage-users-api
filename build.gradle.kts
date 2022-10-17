plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.5.5"
  kotlin("plugin.spring") version "1.7.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.11")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.11")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.11")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.4")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.4")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.code.gson:gson:2.9.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")
  implementation("uk.gov.service.notify:notifications-java-client:3.17.3-RELEASE")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.mockito:mockito-inline:4.8.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
    }
  }
}
