/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.pedropathing.ivy.Scheduler
import com.qualcomm.hardware.lynx.LynxModule
import dev.nextftc.hardware.RobotController
import dev.nextftc.robot.RobotScanner.robot
import dev.nextftc.robot.triggers.Trigger
import org.firstinspires.ftc.robotcore.external.Telemetry

/**
 * Provides lifecycle hooks that tap into the various stages of a [NextOpMode].
 * This allows internal library features (or custom extensions) to execute logic
 * automatically without requiring the user to clutter their OpMode code.
 */
interface OpModeHook {
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
 * (Optional) Hook responsible for bulk-reading the hubs
 */
class BulkReadHook() : OpModeHook {
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
 * Hook responsible for updating the telemetry.
 */
class TelemetryHook(val telemetry: Telemetry) : OpModeHook {
  override fun afterPeriodic() {
    telemetry.update()
  }
}
