/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.feedback

import dev.nextftc.control.model.MotionState
import dev.nextftc.units.Unit
import kotlin.math.sign
import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

/**
 * Coefficients for a PID Controller/
 *
 * @param kP proportional gain, multiplied by the error
 * @param kI integral gain, multiplied by the integral of the error over time
 * @param kD derivative gain, multiplied by the derivative of the error
 */
data class PIDCoefficients @JvmOverloads constructor(
  @JvmField var kP: Double,
  @JvmField var kI: Double = 0.0,
  @JvmField var kD: Double = 0.0,
)

/**
 * Traditional proportional-integral-derivative controller.
 *
 * @param coefficients the [PIDCoefficients] that contains the PID gains
 * @param resetIntegralOnZeroCrossover whether to reset the integral term when the error crosses
 */
class PIDController @JvmOverloads constructor(
  val coefficients: PIDCoefficients,
  val resetIntegralOnZeroCrossover: Boolean = true,
) {
  var lastError: Double = 0.0
    private set
  val error: Double get() = lastError
  private var errorSum = 0.0
  private var lastTimestamp: ComparableTimeMark? = null

  /**
   * Returns true if continuous input is enabled.
   */
  var isContinuousInputEnabled: Boolean = false
    private set

  private var minimumInput: Double = 0.0
  private var maximumInput: Double = 0.0

  private var minimumIntegral: Double = Double.NEGATIVE_INFINITY
  private var maximumIntegral: Double = Double.POSITIVE_INFINITY

  /**
   * Creates a PIDController with the given coefficients.
   *
   * @param kP proportional gain, multiplied by the error
   * @param kI integral gain, multiplied by the integral of the error over time
   * @param kD derivative gain, multiplied by the derivative of the error
   */
  @JvmOverloads constructor(
    kP: Double,
    kI: Double = 0.0,
    kD: Double = 0.0,
    resetIntegralOnZeroCrossover: Boolean = true,
  ) : this(
    PIDCoefficients(kP, kI, kD),
    resetIntegralOnZeroCrossover,
  )

  /**
   * Enables continuous input.
   *
   * Rather then using the max and min input range as constraints, it considers
   * them to be the same point and automatically calculates the shortest route
   * to the setpoint.
   *
   * @param minimumInput The minimum value expected from the input.
   * @param maximumInput The maximum value expected from the input.
   */
  fun enableContinuousInput(minimumInput: Double, maximumInput: Double) {
    require(maximumInput > minimumInput) {
      "maximumInput ($maximumInput) must be greater than minimumInput ($minimumInput)"
    }
    this.isContinuousInputEnabled = true
    this.minimumInput = minimumInput
    this.maximumInput = maximumInput
  }

  /**
   * Disables continuous input.
   */
  fun disableContinuousInput() {
    this.isContinuousInputEnabled = false
  }

  /**
   * Sets the range the accumulated error (integral term) is clamped to, to prevent unbounded
   * windup while the error stays the same sign (e.g. an arm held against a mechanical stop, or
   * fighting a constant load). Defaults to unbounded.
   *
   * @param minimumIntegral the minimum value of the accumulated error
   * @param maximumIntegral the maximum value of the accumulated error
   */
  fun setIntegratorRange(minimumIntegral: Double, maximumIntegral: Double) {
    require(maximumIntegral > minimumIntegral) {
      "maximumIntegral ($maximumIntegral) must be greater than minimumIntegral ($minimumIntegral)"
    }
    this.minimumIntegral = minimumIntegral
    this.maximumIntegral = maximumIntegral
    errorSum = errorSum.coerceIn(minimumIntegral, maximumIntegral)
  }

  private fun inputModulus(input: Double, minimumInput: Double, maximumInput: Double): Double {
    val modulus = maximumInput - minimumInput
    var result = input
    val numMax = ((result - minimumInput) / modulus).toInt()
    result -= numMax * modulus
    val numMin = ((result - maximumInput) / modulus).toInt()
    result -= numMin * modulus
    return result
  }

  private fun getContinuousError(error: Double): Double {
    if (isContinuousInputEnabled) {
      val errorBound = (maximumInput - minimumInput) / 2.0
      return inputModulus(error, -errorBound, errorBound)
    }
    return error
  }

  /**
   * Calculates the PID output
   *
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @param error the error in the target state; the difference between the desired
   *  state and the current state
   * @param errorDerivative the derivative of the error, or `null` to compute it automatically
   *  from the change in error over time. This is typically the difference between the desired
   *  and current velocity when controlling position, or the difference in acceleration when
   *  controlling velocity.
   *
   * @return the PID output
   */
  @JvmOverloads
  fun calculate(
    timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(),
    error: Double,
    errorDerivative: Double? = null,
  ): Double {
    val wrappedError = getContinuousError(error)

    if (lastTimestamp == null) {
      lastError = wrappedError
      lastTimestamp = timestamp
      // On first call with no derivative provided, ignore D term
      val derivative = errorDerivative ?: 0.0
      return coefficients.kP * wrappedError + coefficients.kD * derivative
    }

    if (resetIntegralOnZeroCrossover && lastError.sign != wrappedError.sign) {
      errorSum = 0.0
    }

    val deltaT = (timestamp - lastTimestamp!!).toDouble(DurationUnit.SECONDS)
    errorSum = (errorSum + wrappedError * deltaT).coerceIn(minimumIntegral, maximumIntegral)

    val derivative = errorDerivative ?: if (deltaT > 1e-6) ((wrappedError - lastError) / deltaT) else 0.0

    lastError = wrappedError
    lastTimestamp = timestamp

    return coefficients.kP * wrappedError + coefficients.kI * errorSum + coefficients.kD *
      derivative
  }

  /**
   * Calculates the PID output from an error [MotionState].
   *
   * This overload extracts the error and error derivative from the motion state's position
   * and velocity components. The position magnitude is used as the error, and the velocity
   * magnitude is used as the error derivative.
   *
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @param error the error motion state containing position (error) and velocity (error derivative)
   *
   * @return the PID output
   */
  @JvmOverloads
  fun calculate(timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(), error: MotionState<*>) =
    calculate(
      timestamp,
      error.position.magnitude,
      error.velocity.magnitude,
    )

  /**
   * Calculates the PID output from a reference (setpoint) and measured value.
   *
   * This overload assumes the reference derivative is zero (i.e., the setpoint is constant).
   *
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @param reference the desired/target value (setpoint)
   * @param measured the current measured value
   * @param measuredDerivative the derivative of the measured value, or `null` to compute the
   *  error derivative automatically from the change in error over time.
   *
   * @return the PID output
   */
  @JvmOverloads
  @JvmName("calculateFromReference")
  fun calculate(
    timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(),
    reference: Double,
    measured: Double,
    measuredDerivative: Double? = null,
  ): Double = calculate(
    timestamp,
    reference - measured,
    measuredDerivative?.let { -it },
  )

  /**
   * Calculates the PID output from a reference (setpoint) and measured value, with their
   * respective derivatives.
   *
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @param reference the desired/target value (setpoint)
   * @param measured the current measured value
   * @param referenceDerivative the derivative of the reference value (e.g., desired velocity)
   * @param measuredDerivative the derivative of the measured value (e.g., current velocity)
   *
   * @return the PID output
   */
  @JvmOverloads
  @JvmName("calculateFromReference")
  fun calculate(
    timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(),
    reference: Double,
    measured: Double,
    referenceDerivative: Double,
    measuredDerivative: Double,
  ): Double = calculate(
    timestamp,
    error = reference - measured,
    errorDerivative = referenceDerivative - measuredDerivative,
  )

  /**
   * Calculates the PID output from reference and measured [MotionState]s.
   *
   * This overload computes the error motion state by subtracting the measured state from the
   * reference state. The position difference becomes the error, and the velocity difference
   * becomes the error derivative.
   *
   * @param timestamp the current time (default: TimeSource.Monotonic.markNow())
   * @param reference the desired/target motion state (setpoint)
   * @param measured the current measured motion state
   * @param U the unit type for the motion states (must be the same for both)
   *
   * @return the PID output
   */
  @JvmOverloads
  fun <U : Unit<U>> calculate(
    timestamp: ComparableTimeMark = TimeSource.Monotonic.markNow(),
    reference: MotionState<U>,
    measured: MotionState<U>,
  ) = calculate(
    timestamp,
    reference - measured,
  )

  /**
   * Resets the PID controller
   */
  fun reset() {
    errorSum = 0.0
    lastError = 0.0
    lastTimestamp = null
  }
}
