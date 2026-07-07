/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import com.qualcomm.robotcore.hardware.IMU
import dev.nextftc.control.geometry.Rotation2d
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles

/**
 * A wrapper for the FTC [IMU] that uses lazy initialization.
 */
class NextIMU(initializer: () -> IMU) {
  @JvmOverloads constructor(name: String = "imu") : this({ RobotController.hardwareMap[name] as IMU })

  private val imu by LazyHardware(initializer)

  val device: IMU get() = imu

  fun resetYaw() = imu.resetYaw()

  fun initialize(parameters: IMU.Parameters) = imu.initialize(parameters)

  val yawPitchRollAngles: YawPitchRollAngles get() = imu.robotYawPitchRollAngles

  val angularVelocity: AngularVelocity get() = imu.getRobotAngularVelocity(AngleUnit.RADIANS)

  val yaw: Double get() = yawPitchRollAngles.getYaw(AngleUnit.RADIANS)
  val pitch: Double get() = yawPitchRollAngles.getPitch(AngleUnit.RADIANS)
  val roll: Double get() = yawPitchRollAngles.getRoll(AngleUnit.RADIANS)

  val rotation2d: Rotation2d get() = Rotation2d.exp(yaw)
}
