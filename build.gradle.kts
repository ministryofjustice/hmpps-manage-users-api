import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.1"
  kotlin("plugin.spring") version "2.0.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.8.0")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")
  implementation("org.apache.commons:commons-text:1.12.0")
  implementation("com.pauldijou:jwt-core_2.11:5.0.0")
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
  implementation("uk.gov.service.notify:notifications-java-client:5.1.0-RELEASE")

  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
  testImplementation("org.wiremock:wiremock-standalone:3.7.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("javax.xml.bind:jaxb-api:2.3.1")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<KotlinCompile> {
    kotlinOptions {
      compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
  }
}
