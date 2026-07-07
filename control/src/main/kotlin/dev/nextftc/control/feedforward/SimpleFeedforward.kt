/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.feedforward

import dev.nextftc.control.model.MotionState
import dev.nextftc.units.Seconds
import dev.nextftc.units.Unit
import kotlin.math.sign

/**
 * Coefficients for a simple feedforward controller.
 *
 * These coefficients model the relationship between desired motion and motor output:
 * `output = kS * sign(velocity) + kV * velocity + kA * acceleration`
 *
 * @property kS Static friction gain. The minimum output needed to overcome static friction
 *  and start moving. Applied in the direction of desired velocity.
 * @property kV Velocity gain. Multiplied by the desired velocity to produce the output
 *  needed to maintain that velocity against back-EMF and viscous friction.
 * @property kA Acceleration gain. Multiplied by the desired acceleration to produce the
 *  additional output needed to accelerate the system (overcomes inertia).
 */
data class SimpleFFCoefficients @JvmOverloads constructor(
  @JvmField var kS: Double,
  @JvmField var kV: Double,
  @JvmField var kA: Double = 0.0,
)

/**
 * A simple feedforward controller for velocity control.
 *
 * Feedforward control predicts the motor output needed to achieve a desired motion,
 * without waiting for error to accumulate (unlike feedback/PID control). This is
 * typically used in combination with a feedback controller for best results.
 *
 * The feedforward equation is:
 * ```
 * output = kS * sign(velocity) + kV * velocity + kA * acceleration
 * ```
 *
 * Where:
 * - `kS` compensates for static friction
 * - `kV` compensates for back-EMF and viscous friction
 * - `kA` compensates for inertia during acceleration
 *
 * @param coefficients The [SimpleFFCoefficients] containing kS, kV, and kA gains.
 */
class SimpleFeedforward(val coefficients: SimpleFFCoefficients) {
  /**
   * Constructs a SimpleFeedforward instance using individual feedforward coefficients.
   *
   * @param kS The static friction gain, applied to overcome static friction and initiate movement.
   *           Corresponds to the `kS` parameter in `SimpleFFCoefficients`.
   * @param kV The velocity gain, applied to oppose back-EMF and viscous friction.
   *           Corresponds to the `kV` parameter in `SimpleFFCoefficients`.
   * @param kA The acceleration gain, applied to overcome inertia and provide additional output
   *           for changes in velocity. Defaults to 0.0 for systems without acceleration compensation.
   *           Corresponds to the `kA` parameter in `SimpleFFCoefficients`.
   */
  constructor(
    kS: Double,
    kV: Double,
    kA: Double = 0.0,
  ) : this(SimpleFFCoefficients(kS, kV, kA))

  /**
   * Calculates the feedforward output for a desired velocity and acceleration.
   *
   * @param velocity The desired velocity.
   * @param acceleration The desired acceleration (defaults to 0.0 for constant velocity).
   * @return The feedforward output value.
   */
  @JvmOverloads
  fun calculate(velocity: Double, acceleration: Double = 0.0): Double =
    coefficients.kS * velocity.sign + coefficients.kV * velocity +
      coefficients.kA * acceleration

  /**
   * Calculates the feedforward output from a [MotionState].
   *
   * @param state the target motion state containing velocity and acceleration
   * @return the feedforward output
   */
  fun <U : Unit<U>> calculate(state: MotionState<U>) = calculate(
    state.velocity.into(state.velocity.unit),
    state.acceleration.into(state.velocity.unit.per(Seconds)),
  )

  /**
   * Calculates the maximum achievable velocity given voltage and acceleration constraints.
   *
   * @param maxVoltage The maximum voltage (or output) available.
   * @param acceleration The current or desired acceleration.
   * @return The maximum velocity achievable under these constraints.
   */
  fun maxAchievableVelocity(maxVoltage: Double, acceleration: Double): Double =
    (maxVoltage - coefficients.kS - acceleration * coefficients.kA) / coefficients.kV

  /**
   * Calculates the minimum achievable velocity given voltage and acceleration constraints.
   *
   * @param maxVoltage The maximum voltage (or output) available.
   * @param acceleration The current or desired acceleration.
   * @return The minimum (most negative) velocity achievable under these constraints.
   */
  fun minAchievableVelocity(maxVoltage: Double, acceleration: Double): Double =
    (-maxVoltage + coefficients.kS - acceleration * coefficients.kA) / coefficients.kV

  /**
   * Calculates the maximum achievable acceleration given voltage and velocity constraints.
   *
   * @param maxVoltage The maximum voltage (or output) available.
   * @param velocity The current or desired velocity.
   * @return The maximum acceleration achievable under these constraints.
   */
  fun maxAchievableAcceleration(maxVoltage: Double, velocity: Double): Double =
    (maxVoltage - coefficients.kS * velocity.sign - velocity * coefficients.kV) /
      coefficients.kA

  /**
   * Calculates the minimum achievable acceleration given voltage and velocity constraints.
   *
   * @param maxVoltage The maximum voltage (or output) available.
   * @param velocity The current or desired velocity.
   * @return The minimum (most negative) acceleration achievable under these constraints.
   */
  fun minAchievableAcceleration(maxVoltage: Double, velocity: Double): Double =
    maxAchievableAcceleration(-maxVoltage, velocity)
}
