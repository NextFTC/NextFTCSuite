/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license file in the root directory of this project.
 *
 * Copyright (c) 2026 NextFTC Team
 * Portions of this file are original code or adaptations by the NextFTC Team.
 * Use of this source code is governed by an BSD-3-clause
 * license that can be found in the LICENSE.md file at the root of this repository or at
 * https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.profiles

import dev.nextftc.control.model.MotionState
import dev.nextftc.units.Unit
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * A trapezoidal motion profile generator.
 *
 * A trapezoidal motion profile is a velocity profile that accelerates at a constant rate,
 * maintains a constant velocity, then decelerates at a constant rate. This creates a
 * trapezoid shape when velocity is plotted over time.
 *
 * The profile handles truncated motion profiles (with nonzero initial or final velocity)
 * and profiles that never reach maximum velocity (triangular profiles).
 *
 * @param constraints The [TrapezoidProfileConstraints] that define the maximum velocity and
 *  acceleration for the profile.
 */
class TrapezoidProfile<U : Unit<U>>(private val constraints: TrapezoidProfileConstraints<U>) {
  private val maxAccel = constraints.maxAcceleration.magnitude

  private var direction = 0

  private lateinit var currentState: MotionState<U>

  private var startTimestamp: ComparableTimeMark? = null
  private var endAccel = 0.0
  private var endVel = 0.0
  private var endDecel = 0.0

  /**
   * The total time required to complete the motion profile.
   */
  val totalTime: Double
    get() = endDecel

  /**
   * Calculates the state of the profile at a given timestamp.
   *
   * On the first call, this method records the start timestamp and uses it to compute
   * elapsed time on subsequent calls.
   *
   * @param timestamp The current timestamp.
   * @param current The current state of the system.
   * @param goal The desired goal state.
   *
   * @return The state of the profile at the given timestamp.
   */
  @JvmOverloads
  fun calculate(
    timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(),
    current: MotionState<U>,
    goal: MotionState<U>,
  ): MotionState<U> {
    if (startTimestamp == null) {
      startTimestamp = timestamp
    }
    val elapsed = timestamp - startTimestamp!!
    return calculate(elapsed, current, goal)
  }

  /**
   * Calculates the state of the profile at a given time.
   *
   * @param t The time since the beginning of the profile
   * @param current The current state of the system.
   * @param goal The desired goal state.
   *
   * @return The state of the profile at time [t].
   */
  fun calculate(t: Duration, current: MotionState<U>, goal: MotionState<U>): MotionState<U> {
    direction = if (shouldFlipAcceleration(current, goal)) -1 else 1
    currentState = direct(current)
    val directGoal = direct(goal)

    val timeSeconds = t.toDouble(DurationUnit.SECONDS)

    if (currentState.velocity.absoluteValue > constraints.maxVelocity) {
      currentState =
        currentState.copy(
          velocity = constraints.maxVelocity.withSign(
            currentState.velocity,
          ) as Per<U, TimeUnit>,
        )
    }

    // Deal with a possibly truncated motion profile (with nonzero initial or
    // final velocity) by calculating the parameters as if the profile began and
    // ended at zero velocity
    val cutoffBegin = currentState.velocity.magnitude / maxAccel
    val cutoffDistBegin = cutoffBegin * cutoffBegin * maxAccel / 2.0

    val goalPosition = directGoal.position.into(current.position.unit)
    val goalVelocity = directGoal.velocity.into(current.velocity.unit)

    val cutoffEnd = goalVelocity / maxAccel
    val cutoffDistEnd = cutoffEnd * cutoffEnd * maxAccel / 2.0

    // Now we can calculate the parameters as if it was a full trapezoid instead
    // of a truncated one
    val fullTrapezoidDist =
      cutoffDistBegin + (goalPosition - currentState.position.magnitude) +
        cutoffDistEnd
    var accelerationTime = constraints.maxVelocity.magnitude / maxAccel

    var fullSpeedDist =
      fullTrapezoidDist - accelerationTime * accelerationTime * maxAccel

    // Handle the case where the profile never reaches full speed
    if (fullSpeedDist < 0) {
      accelerationTime = sqrt(fullTrapezoidDist / maxAccel)
      fullSpeedDist = 0.0
    }

    endAccel = accelerationTime - cutoffBegin
    endVel = endAccel + fullSpeedDist / constraints.maxVelocity.magnitude
    endDecel = endVel + accelerationTime - cutoffEnd

    val position: Double
    val velocity: Double
    val accel: Double

    if (timeSeconds < endAccel) {
      velocity = currentState.velocity.magnitude + timeSeconds * maxAccel
      position =
        currentState.position.magnitude +
        (currentState.velocity.magnitude + timeSeconds * maxAccel / 2.0) *
        timeSeconds
      accel = maxAccel
    } else if (timeSeconds < endVel) {
      velocity = constraints.maxVelocity.magnitude
      position = currentState.position.magnitude +
        (
          (currentState.velocity.magnitude + endAccel * maxAccel / 2.0) *
            endAccel +
            constraints.maxVelocity.magnitude * (timeSeconds - endAccel)
          )
      accel = 0.0
    } else if (timeSeconds <= endDecel) {
      velocity = goalVelocity + (endDecel - timeSeconds) * maxAccel
      val timeLeft = endDecel - timeSeconds
      position =
        goalPosition -
        (goalVelocity + timeLeft * maxAccel / 2.0) * timeLeft
      accel = -maxAccel
    } else {
      velocity = goalVelocity
      position = goalPosition
      accel = 0.0
    }

    return direct(current.copy(position, velocity, accel))
  }

  /**
   * Calculates the time remaining until the profile reaches a target position.
   *
   * @param target The target position to reach.
   *
   * @return The time remaining until the target is reached, in seconds.
   */
  fun timeLeftUntil(target: Double): Double {
    val position = currentState.position.magnitude * direction
    var velocity = currentState.velocity.magnitude * direction

    var endAccel = endAccel * direction
    var endFullSpeed = endVel * direction - endAccel

    if (target < position) {
      endAccel = -endAccel
      endFullSpeed = -endFullSpeed
      velocity = -velocity
    }

    endAccel = max(endAccel, 0.0)
    endFullSpeed = max(endFullSpeed, 0.0)

    val acceleration = maxAccel
    val deceleration = -maxAccel

    val distToTarget = abs(target - position)
    if (distToTarget < 1e-6) {
      return 0.0
    }

    var accelDist = velocity * endAccel + 0.5 * acceleration * endAccel * endAccel

    val decelVelocity: Double = if (endAccel > 0) {
      sqrt(abs(velocity * velocity + 2 * acceleration * accelDist))
    } else {
      velocity
    }

    var fullSpeedDist = constraints.maxVelocity.magnitude * endFullSpeed
    val decelDist: Double

    if (accelDist > distToTarget) {
      accelDist = distToTarget
      fullSpeedDist = 0.0
      decelDist = 0.0
    } else if (accelDist + fullSpeedDist > distToTarget) {
      fullSpeedDist = distToTarget - accelDist
      decelDist = 0.0
    } else {
      decelDist = distToTarget - fullSpeedDist - accelDist
    }

    val accelTime =
      (-velocity + sqrt(abs(velocity * velocity + 2 * acceleration * accelDist))) /
        acceleration

    val decelTime =
      (
        -decelVelocity +
          sqrt(abs(decelVelocity * decelVelocity + 2 * deceleration * decelDist))
        ) /
        deceleration

    val fullSpeedTime = fullSpeedDist / constraints.maxVelocity.magnitude

    return accelTime + fullSpeedTime + decelTime
  }

  /**
   * Checks if the profile has finished at the given timestamp.
   *
   * @param timestamp The current timestamp.
   *
   * @return true if the profile has finished, false otherwise.
   */
  fun isFinished(timestamp: ComparableTimeMark): Boolean {
    val start = startTimestamp ?: return false
    val elapsed = (timestamp - start).toDouble(DurationUnit.SECONDS)
    return elapsed >= totalTime
  }

  /**
   * Checks if the profile has finished at the given time.
   *
   * @param t The time since the beginning of the profile, in seconds.
   *
   * @return true if the profile has finished, false otherwise.
   */
  fun isFinished(t: Double): Boolean = t >= totalTime

  /**
   * Resets the profile, clearing the start timestamp.
   *
   * Call this method before starting a new motion profile to ensure
   * the elapsed time is computed correctly from the next [calculate] call.
   */
  fun reset() {
    startTimestamp = null
  }

  /**
   * Flips the sign of the velocity and position if the profile is inverted.
   * Used internally to handle backward motion.
   *
   * @param state The state to transform.
   *
   * @return The transformed state with signs adjusted based on [direction].
   */
  private fun direct(state: MotionState<U>): MotionState<U> {
    val position = state.position * direction.toDouble()
    val velocity = state.velocity * direction.toDouble()
    val acceleration = state.acceleration * direction.toDouble()
    return state.copy(position, velocity, acceleration)
  }

  companion object {
    /**
     * Determines if the acceleration should be flipped based on the initial and goal states.
     *
     * @param initial The initial state.
     * @param goal The goal state.
     *
     * @return true if the goal position is less than the initial position, false otherwise.
     */
    private fun <U : Unit<U>> shouldFlipAcceleration(
      initial: MotionState<U>,
      goal: MotionState<U>,
    ): Boolean = initial.position > goal.position
  }
}
