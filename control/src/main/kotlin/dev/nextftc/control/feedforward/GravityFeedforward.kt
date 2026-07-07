/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.feedforward

import dev.nextftc.control.model.MotionState
import dev.nextftc.units.Radians
import dev.nextftc.units.RadiansPerSecond
import dev.nextftc.units.RadiansPerSecondSquared
import dev.nextftc.units.Seconds
import dev.nextftc.units.Unit
import dev.nextftc.units.unittypes.AngleUnit
import kotlin.math.cos
import kotlin.math.sign

/**
 * Parameters for [ElevatorFeedforward] and [ArmFeedforward]
 *
 * @param kG gravity value, added to overcome gravity
 * @param kS static gain, used to overcome static friction (multiplied by the sign of velocity)
 * @param kV velocity gain, multiplied by the target velocity
 * @param kA acceleration gain, multiplied by the target acceleration
 */
data class GravityFeedforwardParameters @JvmOverloads constructor(
  @JvmField var kG: Double = 0.0,
  @JvmField var kS: Double = 0.0,
  @JvmField var kV: Double = 0.0,
  @JvmField var kA: Double = 0.0,
)

/**
 * Feedforward controller for elevator mechanisms.
 *
 * This feedforward applies a constant gravity compensation term plus velocity and acceleration
 * feedforward terms. It is suitable for linear mechanisms like elevators where gravity exerts
 * a constant force regardless of position.
 *
 * @param coefficients the [GravityFeedforwardParameters] containing the feedforward gains
 */
class ElevatorFeedforward(val coefficients: GravityFeedforwardParameters) {
  /**
   * Secondary constructor for initializing the [ElevatorFeedforward] with individual feedforward parameters.
   *
   * @param kG gravity feedforward gain, used to compensate for constant gravitational forces
   * @param kS static friction feedforward gain, used to overcome static friction (multiplied by the sign of velocity)
   * @param kV velocity feedforward gain, multiplied by the desired velocity
   * @param kA acceleration feedforward gain, multiplied by the desired acceleration
   */
  constructor(
    kG: Double,
    kS: Double = 0.0,
    kV: Double = 0.0,
    kA: Double = 0.0,
  ) : this(
    GravityFeedforwardParameters(kG, kS, kV, kA),
  )

  /**
   * Calculates the feedforward output for the given velocity and acceleration.
   *
   * @param velocity the target velocity
   * @param acceleration the target acceleration
   * @return the feedforward output: `kG + kS * sign(velocity) + kV * velocity + kA * acceleration`
   */
  fun calculate(velocity: Double, acceleration: Double): Double = coefficients.kG +
    coefficients.kS * velocity.sign +
    coefficients.kV * velocity +
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
}

/**
 * Feedforward controller for arm mechanisms.
 *
 * This feedforward applies a gravity compensation term that varies with arm angle (using cosine),
 * plus velocity and acceleration feedforward terms. It is suitable for rotational mechanisms
 * like arms where the gravity torque depends on the arm's angular position.
 *
 * @param coefficients the [GravityFeedforwardParameters] containing the feedforward gains
 */
class ArmFeedforward(val coefficients: GravityFeedforwardParameters) {
  /**
   * Secondary constructor for initializing the feedforward gains using individual parameters.
   *
   * This constructor creates an instance of [GravityFeedforwardParameters] internally
   * using the provided gain values for gravity, static friction, velocity, and acceleration.
   *
   * @param kG gravity gain, used to compensate for the constant force due to gravity
   * @param kS static gain, used to overcome static friction (multiplied by the sign of velocity)
   * @param kV velocity gain, multiplied by the target velocity
   * @param kA acceleration gain, multiplied by the target acceleration
   */
  constructor(
    kG: Double,
    kS: Double = 0.0,
    kV: Double = 0.0,
    kA: Double = 0.0,
  ) : this(
    GravityFeedforwardParameters(kG, kS, kV, kA),
  )

  /**
   * Calculates the feedforward output for the given position, velocity, and acceleration.
   *
   * @param position the arm position in radians (0 = horizontal, π/2 = vertical up)
   * @param velocity the target angular velocity
   * @param acceleration the target angular acceleration
   * @return the feedforward output: `kG * cos(position) + kS * sign(velocity) + kV * velocity + kA * acceleration`
   */
  fun calculate(position: Double, velocity: Double, acceleration: Double) =
    coefficients.kG * cos(position) +
      coefficients.kS * velocity.sign +
      coefficients.kV * velocity +
      coefficients.kA * acceleration

  /**
   * Calculates the feedforward output from a [MotionState].
   *
   * @param state the target motion state containing position, velocity, and acceleration
   * @return the feedforward output
   */
  fun calculate(state: MotionState<AngleUnit>) = calculate(
    state.position.into(Radians),
    state.velocity.into(RadiansPerSecond),
    state.acceleration.into(RadiansPerSecondSquared),
  )
}
