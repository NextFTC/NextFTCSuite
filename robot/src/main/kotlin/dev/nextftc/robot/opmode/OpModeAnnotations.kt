package dev.nextftc.robot.opmode

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NextAutonomous(
  val name: String = "",
  val group: String = "",
  val preselectTeleop: String = "",
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NextTeleop(val name: String = "", val group: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class NextUtility(val name: String = "", val description: String = "")
