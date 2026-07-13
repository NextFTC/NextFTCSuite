/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.opmode

import com.pedropathing.ivy.Scheduler
import com.qualcomm.hardware.lynx.LynxModule
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.actuators.NextMotor
import dev.nextftc.robot.Mechanism
import dev.nextftc.robot.NextRobot
import dev.nextftc.robot.Telemetry
import dev.nextftc.robot.triggers.Trigger

/**
 * Provides lifecycle hooks that tap into the various stages of a [NextOpMode].
 * This allows internal library features (or custom extensions) to execute logic
 * automatically without requiring the user to clutter their OpMode code.
 */
interface OpModeHook {

  /** Called immediately after the OpMode's onInit phase. */
  fun afterConstruction() {}

  /** Called immediately before the OpMode's disabledPeriodic (init_loop) phase. */
  fun beforeDisabled() {}

  /** Called immediately after the OpMode's disabledPeriodic (init_loop) phase. */
  fun afterDisabled() {}

  /** Called immediately before the OpMode's onStart phase. */
  fun beforeStart() {}

  /** Called immediately after the OpMode's onStart phase. */
  fun afterStart() {}

  /** Called immediately before the OpMode's periodic loop. */
  fun beforePeriodic() {}

  /** Called immediately after the OpMode's periodic loop. */
  fun afterPeriodic() {}

  /** Called immediately before the OpMode finishes execution. */
  fun beforeEnd() {}

  /** Called immediately after the OpMode finishes execution. */
  fun afterEnd() {}
}

/**
 * Internal hook responsible for ticking the robot and its mechanisms.
 */
internal class RobotHook(val robot: NextRobot) : OpModeHook {
  override fun afterPeriodic() {
    robot.periodic()
    robot.mechanisms.forEach(Mechanism::periodic)
  }
}

/**
 * Internal hook responsible for ticking the Trigger event loop and the Ivy Scheduler.
 */
internal object SchedulerHook : OpModeHook {
  override fun afterPeriodic() {
    Trigger.defaultEventLoop.poll()
    Scheduler.execute()
  }
}

/**
 * Internal hook responsible for pushing updates to the unified [dev.nextftc.robot.Telemetry] system.
 * Automatically synchronizes backend telemetry outputs during both the init_loop
 * and active periodic phases.
 */
internal object TelemetryHook : OpModeHook {
  override fun afterDisabled() {
    Telemetry.update()
  }

  override fun afterPeriodic() {
    Telemetry.update()
  }
}

/**
 * Internal hook responsible for ticking the motor event loop.
 * Continuously evaluates and updates all NextMotor control states.
 */
internal object MotorHook : OpModeHook {
  override fun afterPeriodic() {
    NextMotor.motorEventLoop.poll()
  }
}

/**
 * (Optional) Hook responsible for managing bulk hardware reads.
 * Automatically switches Lynx hubs to MANUAL caching mode on start,
 * and clears the bulk cache at the end of each periodic cycle to
 * ensure fresh hardware data per loop while minimizing I/O overhead.
 */
object BulkReadHook : OpModeHook {
  private val lynxHubs: List<LynxModule> by lazy {
    RobotController.hardwareMap.getAll(LynxModule::class.java)
  }

  override fun beforeStart() {
    lynxHubs.forEach { it.bulkCachingMode = LynxModule.BulkCachingMode.MANUAL }
  }

  override fun afterStart() = clearBulkReadCache()

  override fun afterPeriodic() = clearBulkReadCache()

  private fun clearBulkReadCache() {
    lynxHubs.forEach { it.clearBulkCache() }
  }
}
