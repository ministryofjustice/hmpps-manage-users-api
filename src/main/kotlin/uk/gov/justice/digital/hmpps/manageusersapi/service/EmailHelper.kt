package uk.gov.justice.digital.hmpps.manageusersapi.service

import org.apache.commons.lang3.StringUtils.lowerCase
import org.apache.commons.lang3.StringUtils.replaceChars
import org.apache.commons.lang3.StringUtils.trim

object EmailHelper {

  @JvmStatic
  fun format(emailInput: String?): String? = replaceChars(lowerCase(trim(emailInput)), '’', '\'')
}
