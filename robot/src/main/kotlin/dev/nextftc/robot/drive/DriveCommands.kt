/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.drive

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.commands.Commands
import com.qualcomm.robotcore.hardware.Gamepad
import dev.nextftc.control.drive.DriveInput
import dev.nextftc.control.drive.MecanumKinematics
import dev.nextftc.control.drive.TankKinematics
import dev.nextftc.hardware.actuators.NextMotor
import java.util.function.Supplier
import kotlin.math.cos
import kotlin.math.sin

/** Multiplier applied to all drive input; re-read live each loop so it can be adjusted mid-match (e.g. slow-mode). */
var scalar: Double = 1.0
  set(value) {
    field = value
  }

/** Command that drives a mecanum drivetrain from [gamepad]'s sticks. */
fun mecanumDrive(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  gamepad: Gamepad,
  kinematics: MecanumKinematics = MecanumKinematics(),
): Command = Commands.infinite {
  val powers = kinematics.calculate(
    DriveInput(
      y = gamepad.left_stick_x.toDouble() * scalar,
      x = -gamepad.left_stick_y.toDouble() * scalar,
      rx = gamepad.right_stick_x.toDouble() * scalar,
    ),
  )
  frontLeft.throttle = powers.frontLeft
  frontRight.throttle = powers.frontRight
  backLeft.throttle = powers.backLeft
  backRight.throttle = powers.backRight
}

/**
 * Command that drives a mecanum drivetrain from [gamepad]'s sticks, field-centric.
 *
 * Rotates stick input by [heading] (radians, field-relative) so "forward" on the
 * stick always moves the robot away from the driver, regardless of robot orientation.
 *
 * @param heading Supplies the robot's current field-relative heading in radians —
 *   e.g. `{ follower.pose.heading }` from Pedro Pathing, or an IMU reading zeroed
 *   to match the robot's starting orientation on the field.
 */
fun mecanumDriveFieldCentric(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  gamepad: Gamepad,
  heading: Supplier<Double>,
  kinematics: MecanumKinematics = MecanumKinematics(),
): Command = Commands.infinite {
  val rawY = gamepad.left_stick_x.toDouble()
  val rawX = -gamepad.left_stick_y.toDouble()
  val h = heading.get()
  val c = cos(-h)
  val sinH = sin(-h)

  val powers = kinematics.calculate(
    DriveInput(
      y = (rawX * c - rawY * sinH) * scalar,
      x = (rawX * sinH + rawY * c) * scalar,
      rx = gamepad.right_stick_x.toDouble() * scalar,
    ),
  )
  frontLeft.throttle = powers.frontLeft
  frontRight.throttle = powers.frontRight
  backLeft.throttle = powers.backLeft
  backRight.throttle = powers.backRight
}

/** Command that drives an arcade drivetrain from [gamepad]'s sticks. */
fun arcadeDrive(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  gamepad: Gamepad,
  kinematics: TankKinematics = TankKinematics(),
): Command = Commands.infinite {
  val powers = kinematics.calculate(
    DriveInput(
      y = 0.0,
      x = -gamepad.left_stick_y.toDouble() * scalar,
      rx = gamepad.right_stick_x.toDouble() * scalar,
    ),
  )
  frontLeft.throttle = powers.left
  backLeft.throttle = powers.left
  frontRight.throttle = powers.right
  backRight.throttle = powers.right
}

/** Command that drive a tank drivetrain from [gamepad]'s sticks */
fun tankDrive(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  gamepad: Gamepad,
): Command = Commands.infinite {
  val leftPower = -gamepad.left_stick_y.toDouble() * scalar
  val rightPower = -gamepad.right_stick_y.toDouble() * scalar

  frontLeft.throttle = leftPower
  frontRight.throttle = rightPower
  backLeft.throttle = leftPower
  backRight.throttle = rightPower
}
