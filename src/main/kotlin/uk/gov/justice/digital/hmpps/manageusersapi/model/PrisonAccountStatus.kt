package uk.gov.justice.digital.hmpps.manageusersapi.model
enum class PrisonAccountStatus(
  val code: Int,
  val desc: String,
  val isExpired: Boolean,
  val isLocked: Boolean,
  val isGracePeriod: Boolean,
  val isUserLocked: Boolean,
) {
  OPEN(0, "OPEN", false, false, false, false),
  EXPIRED(1, "EXPIRED", true, false, false, false),
  EXPIRED_GRACE(2, "EXPIRED(GRACE)", true, false, true, false),
  LOCKED_TIMED(4, "LOCKED(TIMED)", false, true, false, true),
  LOCKED(8, "LOCKED", false, true, false, true),
  EXPIRED_LOCKED_TIMED(5, "EXPIRED & LOCKED(TIMED)", true, true, false, true),
  EXPIRED_GRACE_LOCKED_TIMED(6, "EXPIRED(GRACE) & LOCKED(TIMED)", true, true, true, true),
  EXPIRED_LOCKED(9, "EXPIRED & LOCKED", true, true, false, false),
  EXPIRED_GRACE_LOCKED(10, "EXPIRED(GRACE) & LOCKED", true, true, true, false),
  ;

  companion object {
    fun get(code: Int): PrisonAccountStatus = values().first { it.code == code }

    fun get(desc: String): PrisonAccountStatus = values().first { it.desc == desc }

    fun activeStatuses() = PrisonAccountStatus.values().filter { !(it.isLocked || (it.isExpired && !it.isGracePeriod)) }

    fun inActiveStatuses() = PrisonAccountStatus.values().filter { it.isLocked || (it.isExpired && !it.isGracePeriod) }
  }
}
