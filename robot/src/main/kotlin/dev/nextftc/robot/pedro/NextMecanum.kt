/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.pedro

import com.pedropathing.drivetrain.DrivePowers
import com.pedropathing.drivetrain.Drivetrain
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.nextftc.hardware.actuators.NextMotor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Drop-in replacement for Pedro's own [com.pedropathing.revhub.drivetrains.Mecanum].
 *
 * Two ways to build one:
 * - `NextMecanum(hardwareMap, config)` — builds its own [NextMotor]s from a [NextMecanumConfig],
 *   mirroring Pedro's own `Mecanum(HardwareMap, MecanumConfig)` constructor shape.
 * - `NextMecanum(frontLeft, frontRight, backLeft, backRight)` — reuses [NextMotor]s you've
 *   already constructed elsewhere, e.g. an existing drivetrain [dev.nextftc.robot.Mechanism].
 */
class NextMecanum @JvmOverloads constructor(
  private val frontLeft: NextMotor,
  private val frontRight: NextMotor,
  private val backLeft: NextMotor,
  private val backRight: NextMotor,
  private val manualBrakeMode: Boolean = true,
) : Drivetrain {

  constructor(hardwareMap: HardwareMap, config: NextMecanumConfig) : this(
    NextMotor(
      config.frontLeftModule.get(),
      config.frontLeftPort.get(),
      cacheTolerance = config.powerThreshold.get(),
    ).apply { direction = toNextDirection(config.frontLeftDirection.get()) },
    NextMotor(
      config.frontRightModule.get(),
      config.frontRightPort.get(),
      cacheTolerance = config.powerThreshold.get(),
    ).apply { direction = toNextDirection(config.frontRightDirection.get()) },
    NextMotor(
      config.backLeftModule.get(),
      config.backLeftPort.get(),
      cacheTolerance = config.powerThreshold.get(),
    ).apply { direction = toNextDirection(config.backLeftDirection.get()) },
    NextMotor(
      config.backRightModule.get(),
      config.backRightPort.get(),
      cacheTolerance = config.powerThreshold.get(),
    ).apply { direction = toNextDirection(config.backRightDirection.get()) },
    config.manualBrakeMode.get(),
  )

  private val motors = arrayOf(frontLeft, frontRight, backLeft, backRight)

  /** Last-applied normalized wheel powers, in front-left/front-right/back-left/back-right order. */
  val wheelPowers = DoubleArray(4)

  private var drivePowers = DrivePowers.zero()
  private var powerScale = 1.0
  private var currentZeroPowerBehavior: NextMotor.ZeroPowerBehavior? = null

  private fun computeWheelPowersUnnormalized(powers: DrivePowers): DoubleArray {
    val forward = powers.forward()
    val strafe = powers.strafe()
    val turn = powers.turn()
    return doubleArrayOf(
      forward - strafe - turn,
      forward + strafe + turn,
      forward + strafe - turn,
      forward - strafe + turn,
    )
  }

  private fun applyDrive(powers: DrivePowers) {
    drivePowers = powers
    val raw = computeWheelPowersUnnormalized(powers)
    val maxPower = raw.maxOf { abs(it) }.coerceAtLeast(1.0)
    powerScale = 1.0 / maxPower
    for (i in motors.indices) {
      wheelPowers[i] = raw[i] / maxPower
      motors[i].throttle = wheelPowers[i]
    }
  }

  override fun drive(powers: DrivePowers, manual: Boolean) {
    setZeroPowerBehavior(
      if (manual &&
        manualBrakeMode
      ) {
        NextMotor.ZeroPowerBehavior.BRAKE
      } else {
        NextMotor.ZeroPowerBehavior.FLOAT
      },
    )
    applyDrive(powers)
  }

  override fun maxScaling(current: DrivePowers, delta: DrivePowers): Double {
    var lambda = 1.0
    val currentPowers = computeWheelPowersUnnormalized(current)
    val deltaPowers = computeWheelPowersUnnormalized(delta)
    for (i in 0 until 4) {
      val a = currentPowers[i]
      val b = deltaPowers[i]
      if (abs(b) < 1e-9) continue
      val t1 = (1.0 - a) / b
      val t2 = (-1.0 - a) / b
      if (t1 in 0.0..lambda) lambda = t1
      if (t2 in 0.0..lambda) lambda = t2
    }
    return lambda.coerceIn(0.0, 1.0)
  }

  override fun stop() = stop(manualBrakeMode)

  override fun stop(brake: Boolean) {
    setZeroPowerBehavior(
      if (brake) NextMotor.ZeroPowerBehavior.BRAKE else NextMotor.ZeroPowerBehavior.FLOAT,
    )
    motors.forEach { it.throttle = 0.0 }
  }

  private fun setZeroPowerBehavior(behavior: NextMotor.ZeroPowerBehavior) {
    if (currentZeroPowerBehavior == behavior) return
    currentZeroPowerBehavior = behavior
    motors.forEach { it.zeroPowerBehavior = behavior }
  }

  override fun debug(): Map<String, Any> = mapOf(
    "forward" to drivePowers.forward(),
    "strafe" to drivePowers.strafe(),
    "turn" to drivePowers.turn(),
    "powerScale" to powerScale,
    "leftFrontWheelPower" to wheelPowers[0],
    "rightFrontWheelPower" to wheelPowers[1],
    "leftBackWheelPower" to wheelPowers[2],
    "rightBackWheelPower" to wheelPowers[3],
  )

  override fun interpolateVelocity(xRadius: Double, yRadius: Double, theta: Double): Double =
    1.0 / (abs(cos(theta)) / xRadius + abs(sin(theta)) / yRadius)

  companion object {
    private fun toNextDirection(d: DcMotorSimple.Direction): NextMotor.Direction = if (d ==
      DcMotorSimple.Direction.REVERSE
    ) {
      NextMotor.Direction.REVERSE
    } else {
      NextMotor.Direction.FORWARD
    }
  }
}
