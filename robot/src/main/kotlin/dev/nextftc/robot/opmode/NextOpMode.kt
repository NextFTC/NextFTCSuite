package dev.nextftc.robot.opmode

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.nextftc.robot.NextFTCException
import dev.nextftc.robot.NextRobot
import dev.nextftc.robot.RobotLog
import org.firstinspires.ftc.robotcore.external.Telemetry as SdkTelemetry

/**
 * Base class for all NextFTC OpModes.
 *
 * Automatically injects the scanned [dev.nextftc.robot.NextRobot] instance and handles the lifecycle
 * execution, calling various [OpModeHook]s and managing the internal command scheduler.
 *
 * @param hooks Internal hooks used to manage robot mechanisms and the scheduler loop.
 */
abstract class NextOpMode internal constructor(internal val hooks: MutableList<OpModeHook>) {
  /**
   * Secondary constructor invoked by the [NextFTCOpModeScanner] during automatic registration.
   *
   * @param robot The automatically resolved [dev.nextftc.robot.NextRobot] instance.
   * @param hooks Additional custom hooks to execute during the OpMode lifecycle.
   */
  constructor(robot: NextRobot, vararg hooks: OpModeHook) : this(hooks.toMutableList()) {
    this.hooks += RobotHook(robot)
    this.hooks += SchedulerHook
    this.hooks += MotorHook
    this.hooks += TelemetryHook
  }

  /** The primary gamepad provided by the Driver Station. */
  @JvmField val gamepad1: Gamepad = requireActive(activeGamepad1, "gamepad1")

  /** The secondary gamepad provided by the Driver Station. */
  @JvmField val gamepad2: Gamepad = requireActive(activeGamepad2, "gamepad2")

  /** The standard SDK telemetry provided by the Driver Station. */
  @JvmField val telemetry: SdkTelemetry = requireActive(activeTelemetry, "telemetry")

  /** The hardware map provided by the FTC SDK. */
  @JvmField val hardwareMap: HardwareMap = requireActive(activeHardwareMap, "hardwareMap")

  /** Called repeatedly while the OpMode is in the INIT phase. */
  open fun disabledPeriodic() {}

  /** Called exactly once after the PLAY button is pressed. */
  open fun start() {}

  /** Called repeatedly while the OpMode is actively running. */
  open fun periodic() {}

  /** Called exactly once when the OpMode finishes execution. */
  open fun end() {}

  companion object {
    @JvmSynthetic internal var activeGamepad1: Gamepad? = null

    @JvmSynthetic internal var activeGamepad2: Gamepad? = null

    @JvmSynthetic internal var activeTelemetry: SdkTelemetry? = null

    @JvmSynthetic internal var activeHardwareMap: HardwareMap? = null

    private fun <T : Any> requireActive(value: T?, name: String): T = value ?: throw NextFTCException(
      "Cannot access $name because this OpMode was not started by NextFTC. NextFTC OpModes are " +
        "constructed automatically when the OpMode is run; they cannot be instantiated directly.",
    )
  }
}

internal class BoundNextOpMode(val opModeConstructor: () -> NextOpMode) : LinearOpMode() {
  override fun runOpMode() {
    NextOpMode.activeGamepad1 = this.gamepad1
    NextOpMode.activeGamepad2 = this.gamepad2
    NextOpMode.activeTelemetry = this.telemetry
    NextOpMode.activeHardwareMap = this.hardwareMap

    var opMode: NextOpMode? = null

    try {
      opMode = opModeConstructor()

      opMode.hooks.forEach(OpModeHook::afterConstruction)
      while (opModeInInit()) {
        opMode.hooks.forEach(OpModeHook::beforeDisabled)
        opMode.disabledPeriodic()
        opMode.hooks.forEach(OpModeHook::afterDisabled)
      }
      waitForStart()
      opMode.hooks.forEach(OpModeHook::beforeStart)
      opMode.start()
      opMode.hooks.forEach(OpModeHook::afterStart)
      while (opModeIsActive()) {
        opMode.hooks.forEach(OpModeHook::beforePeriodic)
        opMode.periodic()
        opMode.hooks.forEach(OpModeHook::afterPeriodic)
      }
      opMode.hooks.forEach(OpModeHook::beforeEnd)
      opMode.end()
    } finally {
      // even if the OpMode didn't finish execution we still want to clean up the hooks
      // and clear the static references to the SDK objects
      opMode?.hooks?.forEach { hook ->
        try {
          hook.afterEnd()
        } catch (throwable: Throwable) {
          RobotLog.warn("${hook::class.displayName} threw while cleaning up after the OpMode.", throwable)
        }
      }

      NextOpMode.activeGamepad1 = null
      NextOpMode.activeGamepad2 = null
      NextOpMode.activeTelemetry = null
      NextOpMode.activeHardwareMap = null
    }
  }
}
