package dev.nextftc.robot.opmode

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.hardware.Gamepad
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.nextftc.robot.NextRobot
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
  @JvmField val gamepad1: Gamepad = activeGamepad1!!

  /** The secondary gamepad provided by the Driver Station. */
  @JvmField val gamepad2: Gamepad = activeGamepad2!!

  /** The standard SDK telemetry provided by the Driver Station. */
  @JvmField val telemetry: SdkTelemetry = activeTelemetry!!

  /** The hardware map provided by the FTC SDK. */
  @JvmField val hardwareMap: HardwareMap = activeHardwareMap!!

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
  }
}

internal class BoundNextOpMode(val opModeConstructor: () -> NextOpMode) : LinearOpMode() {
  override fun runOpMode() {
    NextOpMode.activeGamepad1 = this.gamepad1
    NextOpMode.activeGamepad2 = this.gamepad2
    NextOpMode.activeTelemetry = this.telemetry
    NextOpMode.activeHardwareMap = this.hardwareMap

    try {
      val opMode = opModeConstructor()

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
      opMode.hooks.forEach(OpModeHook::afterEnd)
    } finally {
      NextOpMode.activeGamepad1 = null
      NextOpMode.activeGamepad2 = null
      NextOpMode.activeTelemetry = null
      NextOpMode.activeHardwareMap = null
    }
  }
}
