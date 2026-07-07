/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode

/**
 * Base class for all NextFTC OpModes.
 *
 * Automatically injects the scanned [NextRobot] instance and handles the lifecycle
 * execution, calling various [OpModeHook]s and managing the internal command scheduler.
 *
 * @param hooks Internal hooks used to manage robot mechanisms and the scheduler loop.
 */
abstract class NextOpMode internal constructor(private val hooks: MutableList<OpModeHook>) :
  LinearOpMode() {
  /**
   * Secondary constructor invoked by the [RobotOpModeScanner] during automatic registration.
   *
   * @param robot The automatically resolved [NextRobot] instance.
   * @param hooks Additional custom hooks to execute during the OpMode lifecycle.
   */
  constructor(robot: NextRobot, vararg hooks: OpModeHook) : this(hooks.toMutableList()) {
    this.hooks += RobotHook(robot)
    this.hooks += SchedulerHook
    this.hooks += TelemetryHook(telemetry)
  }

  final override fun runOpMode() {
    onInit()
    while (opModeInInit()) {
      disabledPeriodic()
    }
    waitForStart()
    hooks.forEach(OpModeHook::beforeStart)
    onStart()
    hooks.forEach(OpModeHook::afterStart)
    while (opModeIsActive()) {
      hooks.forEach(OpModeHook::beforePeriodic)
      periodic()
      hooks.forEach(OpModeHook::afterPeriodic)
    }
    hooks.forEach(OpModeHook::beforeEnd)
    onEnd()
    hooks.forEach(OpModeHook::afterEnd)
  }

  /** Called exactly once after the INIT button is pressed. */
  open fun onInit() {}

  /** Called repeatedly while the OpMode is in the INIT phase. */
  open fun disabledPeriodic() {}

  /** Called exactly once after the PLAY button is pressed. */
  open fun onStart() {}

  /** Called repeatedly while the OpMode is actively running. */
  open fun periodic() {}

  /** Called exactly once when the OpMode finishes execution. */
  open fun onEnd() {}
}
