package uk.gov.justice.digital.hmpps.manageusersapi.config

/** Who an endpoint is intended for. Used to split the swagger docs into groups. */
enum class Audience(val groupName: String, val description: String) {
  INTERNAL(
    "internal",
    "**Internal API**: these endpoints are intended for use only by HMPPS Auth and Manage Users services. " +
      "If you need access to these endpoints, please contact the Auth & Audit team on Slack at #hmpps-auth-audit-registers.",
  ),
  EXTERNAL(
    "external",
    "**External API**: these endpoints are intended for use by all clients.",
  ),
}

/** Can be applied to a controller class or an individual endpoint; the method-level value wins. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ApiAudience(val value: Audience)
