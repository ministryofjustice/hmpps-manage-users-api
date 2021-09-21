package uk.gov.justice.digital.hmpps.manageusersapi.service

import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class AdminType(val adminTypeCode: String, val adminTypeName: String) {
  DPS_LSA("DPS_LSA", "DPS Local System Administrator"),
  DPS_ADM("DPS_ADM", "DPS Central Administrator"),
  EXT_ADM("EXT_ADM", "External Administrator"),
}

data class AdminTypeReturn(val adminTypeCode: String, val adminTypeName: String)
