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

/** Command that drives a mecanum drivetrain from [gamepad]'s sticks. */
fun mecanumDrive(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  kinematics: MecanumKinematics,
  gamepad: Gamepad,
): Command = Commands.infinite {
  val powers = kinematics.calculate(
    DriveInput(
      x = gamepad.left_stick_x.toDouble(),
      y = -gamepad.left_stick_y.toDouble(),
      rx = gamepad.right_stick_x.toDouble(),
    ),
  )
  frontLeft.setThrottle(powers.frontLeft)
  frontRight.setThrottle(powers.frontRight)
  backLeft.setThrottle(powers.backLeft)
  backRight.setThrottle(powers.backRight)
}

/**
 * Command that drives a mecanum drivetrain from [gamepad]'s sticks, field-centric.
 *
 * Rotates stick input by [heading] (radians, field-relative) so "forward" on the
 * stick always moves the robot away from the driver, regardless of robot orientation.
 * @param heading Supplies the robot's current field-relative heading in radians
 */
fun mecanumDriveFieldCentric(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  kinematics: MecanumKinematics,
  gamepad: Gamepad,
  heading: Supplier<Double>,
): Command = Commands.infinite {
  val rawX = gamepad.left_stick_x.toDouble()
  val rawY = -gamepad.left_stick_y.toDouble()
  val h = heading.get()
  val c = cos(-h)
  val s = sin(-h)

  val powers = kinematics.calculate(
    DriveInput(
      x = rawX * c - rawY * s,
      y = rawX * s + rawY * c,
      rx = gamepad.right_stick_x.toDouble(),
    ),
  )
  frontLeft.setThrottle(powers.frontLeft)
  frontRight.setThrottle(powers.frontRight)
  backLeft.setThrottle(powers.backLeft)
  backRight.setThrottle(powers.backRight)
}

/** Command that drives a tank drivetrain from [gamepad]'s sticks. */
fun tankDrive(
  frontLeft: NextMotor,
  frontRight: NextMotor,
  backLeft: NextMotor,
  backRight: NextMotor,
  kinematics: TankKinematics,
  gamepad: Gamepad,
): Command = Commands.infinite {
  val powers = kinematics.calculate(
    DriveInput(
      x = 0.0,
      y = -gamepad.left_stick_y.toDouble(),
      rx = gamepad.right_stick_x.toDouble(),
    ),
  )
  frontLeft.setThrottle(powers.left)
  backLeft.setThrottle(powers.left)
  frontRight.setThrottle(powers.right)
  backRight.setThrottle(powers.right)
}
