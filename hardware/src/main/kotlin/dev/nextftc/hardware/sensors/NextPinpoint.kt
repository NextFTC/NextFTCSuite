/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import dev.nextftc.control.geometry.Pose2d
import dev.nextftc.control.geometry.PoseVelocity2d
import dev.nextftc.control.geometry.Vector2d
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import dev.nextftc.units.Inches
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.radiansPerSecond
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit

/**
 * A wrapper for the GoBilda Pinpoint Odometry Computer that uses lazy initialization.
 */
class NextPinpoint(initializer: () -> GoBildaPinpointDriver) {
  constructor(name: String) : this({ RobotController.hardwareMap[name] as GoBildaPinpointDriver })

  private val driver by LazyHardware(initializer)

  val device: GoBildaPinpointDriver get() = driver

  /** Updates the position data from the device. Call this each loop. */
  fun update() = driver.update()

  /** Resets the odometry position tracking. */
  fun resetPosAndIMU() = driver.resetPosAndIMU()

  val pose: Pose2d get() {
    val p = driver.position
    return Pose2d(
      p.getX(DistanceUnit.INCH),
      p.getY(DistanceUnit.INCH),
      p.getHeading(AngleUnit.RADIANS),
    )
  }

  val velocity: PoseVelocity2d get() {
    return PoseVelocity2d(
      Vector2d(
        driver.getVelX(DistanceUnit.INCH).inchesPerSecond,
        driver.getVelY(DistanceUnit.INCH).inchesPerSecond,
      ),
      driver.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS).radiansPerSecond,
    )
  }
}
