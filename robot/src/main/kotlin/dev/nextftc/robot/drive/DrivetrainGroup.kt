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
import dev.nextftc.control.drive.DriveKinematics
import dev.nextftc.hardware.actuators.NextMotor
import java.util.function.Supplier
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

/**
 * Creates a 4-wheel drivetrain's group using the motors with a [DriveKinematics] implementation and
 * exposes a ready-to-schedule driver-controlled [Command] via [driverControlled].
 *
 * Works for any drivetrain type whose kinematics produces [WheelPowers] (mecanum, tank,
 * X-drive) — edit [kinematics] to fit your drive train
 *
 * @param frontLeft The front-left drive motor.
 * @param frontRight The front-right drive motor.
 * @param backLeft The back-left drive motor.
 * @param backRight The back-right drive motor.
 * @param kinematics The kinematics implementation used to convert stick input into wheel
 *   powers (e.g. [MecanumKinematics], [TankKinematics]).
 * @param headingSupplier Supplies the robot's current field-relative heading in radians,
 *   used to rotate stick input for field-centric driving. Pass `null` or nothing at all for
 *   robot-centric driving.
 */
class DrivetrainGroup(
  private val frontLeft: NextMotor,
  private val frontRight: NextMotor,
  private val backLeft: NextMotor,
  private val backRight: NextMotor,
  var kinematics: DriveKinematics,
) {
  private var headingSupplier: Supplier<Double>? = null

  constructor(
    frontLeft: NextMotor,
    frontRight: NextMotor,
    backLeft: NextMotor,
    backRight: NextMotor,
    kinematics: DriveKinematics,
    headingSupplier: Supplier<Double>,
  ) : this(frontLeft, frontRight, backLeft, backRight, kinematics) {
    this.headingSupplier = headingSupplier
  }

  /** Multiplier applied to all drive input before kinematics. 1.0 = full speed. */
  var scalar: Double = 1.0
    set(value) {
      field = value.coerceIn(0.0, 1.0)
    }

  /** Stick values with magnitude below this are treated as zero. Clamped to [0.0, 1.0). */
  var deadZone: Double = 0.05
    set(value) {
      field = value.coerceIn(0.0, 0.999)
    }

  private fun checkDeadzone(input: Double): Double = if (input.absoluteValue < deadZone) 0.0 else input

  /** Returns a command that drives this group from the given gamepad's sticks, indefinitely. */
  fun driverControlled(gamepad: Gamepad): Command = Commands.infinite {
    var x = checkDeadzone(gamepad.left_stick_x.toDouble())
    var y = checkDeadzone(-gamepad.left_stick_y.toDouble())
    val rx = checkDeadzone(gamepad.right_stick_x.toDouble())

    headingSupplier?.let { supplier ->
      val heading = supplier.get()
      val rotatedX = x * cos(-heading) - y * sin(-heading)
      val rotatedY = x * sin(-heading) + y * cos(-heading)
      x = rotatedX
      y = rotatedY
    }

    val powers = kinematics.calculate(DriveInput(x * scalar, y * scalar, rx * scalar))
    frontLeft.setThrottle(powers.frontLeft)
    frontRight.setThrottle(powers.frontRight)
    backLeft.setThrottle(powers.backLeft)
    backRight.setThrottle(powers.backRight)
  }
}
