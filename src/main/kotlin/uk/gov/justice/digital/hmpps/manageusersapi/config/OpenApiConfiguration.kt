package uk.gov.justice.digital.hmpps.manageusersapi.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotatedElementUtils
import java.lang.reflect.Method

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version!!

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://manage-users-api.hmpps.service.justice.gov.uk").description("Prod"),
        Server().url("https://manage-users-api-preprod.hmpps.service.justice.gov.uk").description("PreProd"),
        Server().url("https://manage-users-api-dev.hmpps.service.justice.gov.uk").description("Development"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("Manage Users")
        .version(version)
        .description("Manage Users Facility")
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )

  @Bean
  fun internalApi(): GroupedOpenApi = groupFor(Audience.INTERNAL)

  @Bean
  fun externalApi(): GroupedOpenApi = groupFor(Audience.EXTERNAL)

  private fun groupFor(audience: Audience): GroupedOpenApi = GroupedOpenApi.builder()
    .group(audience.groupName)
    .displayName(audience.groupName.replaceFirstChar { it.uppercase() })
    .addOpenApiMethodFilter { method -> audienceOf(method) == audience }
    .addOpenApiCustomizer { openApi ->
      openApi.info.description = "${openApi.info.description}\n\n${audience.description}"
    }
    .build()

  companion object {
    // Method-level annotation overrides class-level; untagged endpoints default to INTERNAL
    fun audienceOf(method: Method): Audience = AnnotatedElementUtils.findMergedAnnotation(method, ApiAudience::class.java)?.value
      ?: AnnotatedElementUtils.findMergedAnnotation(method.declaringClass, ApiAudience::class.java)?.value
      ?: Audience.INTERNAL
  }
}
