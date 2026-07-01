/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import dev.nextftc.control.feedback.PIDCoefficients
import dev.nextftc.control.feedback.PIDController
import dev.nextftc.control.feedforward.SimpleFFCoefficients
import dev.nextftc.control.feedforward.SimpleFeedforward
import dev.nextftc.hardware.Caching
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.motorController
import dev.nextftc.units.inches
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.measuretypes.LinearVelocity
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.seconds
import kotlin.math.sign

/**
 * Comprehensive wrapper around a [DcMotorImplEx] supporting multiple control modes.
 *
 * [NextMotor] provides a high-level interface for DC motor control including:
 * - **Throttle control**: Direct power/PWM percentage.
 * - **Voltage control**: Set power as a voltage relative to the input rail voltage.
 * - **Position control**: PID-based regulation to reach and hold a target encoder position.
 * - **Velocity control**: PID + feedforward to reach and hold a target velocity.
 *
 * The motor wraps a [DcMotorImplEx] obtained lazily through an initializer, supporting
 * construction from a Lynx module/port, hardware map name, or a raw initializer function.
 *
 * Encoder readings are automatically converted to [Distance] and [LinearVelocity] using
 * the configured [distancePerCount] conversion factor.
 *
 * Example:
 * ```
 * val motor = NextMotor("driveMotor", distancePerCount = 0.05.inches)
 * motor.setVelocitySetpoint(12.0.inchesPerSecond)
 * ```
 *
 * @param initializer A function that returns the backing [DcMotorImplEx]. Invoked lazily.
 * @param distancePerCount Conversion factor from encoder counts to distance. Defaults to 1.0 inch.
 * @param cacheTolerance Power caching tolerance to reduce redundant hardware writes. Defaults to 0.01.
 */
class NextMotor(
  initializer: () -> DcMotorImplEx,
  var distancePerCount: Distance = 1.0.inches,
  cacheTolerance: Double = 0.01,
) {
  /**
   * Constructs a motor from a Lynx hub module and port number.
   *
   * Wraps the port within the given module's motor controller.
   *
   * @param module The Lynx module housing this motor port.
   * @param port The motor port on the module (0-based).
   * @param distancePerCount Encoder count to distance conversion factor.
   * @param cacheTolerance Power caching tolerance.
   */
  @JvmOverloads constructor(
    module: LynxModule,
    port: Int,
    distancePerCount: Distance = 1.0.inches,
    cacheTolerance: Double = 0.01,
  ) : this(
    { DcMotorImplEx(module.motorController, port) },
    distancePerCount,
    cacheTolerance,
  )

  /**
   * Constructs a motor by name from the current hardware map.
   *
   * Looks up the motor in [RobotController.hardwareMap].
   *
   * @param name Hardware map device name.
   * @param distancePerCount Encoder count to distance conversion factor.
   * @param cacheTolerance Power caching tolerance.
   */
  @JvmOverloads constructor(
    name: String,
    distancePerCount: Distance = 1.0.inches,
    cacheTolerance: Double = 0.01,
  ) : this({ RobotController.hardwareMap[name] as DcMotorImplEx }, distancePerCount, cacheTolerance)

  private val motor by LazyHardware(initializer)

  /**
   * Position control constants (PID and feedforward gains).
   *
   * Modify [kP], [kI], [kD], [kS], [kV], [kA] properties and gravity/cos terms
   * to tune position setpoint tracking.
   */
  val positionConstants: MotorPositionConstants = MotorPositionConstants()

  /**
   * PID controller used for position setpoint tracking.
   */
  val positionPID = PIDController(positionConstants.pidConstants)

  /**
   * Velocity control constants (PID and feedforward gains).
   *
   * Modify [kP], [kI], [kD], [kS], [kV], [kA] properties to tune
   * velocity setpoint tracking.
   */
  val velocityConstants: MotorVelocityConstants = MotorVelocityConstants()

  /**
   * PID controller used for velocity setpoint tracking.
   */
  val velocityPID = PIDController(velocityConstants.pidConstants)

  /**
   * Feedforward calculator used in conjunction with velocity PID.
   */
  val velocityFF = SimpleFeedforward(velocityConstants.ffCoefficients)

  /**
   * Current active control mode (THROTTLE, VOLTAGE, POSITION, or VELOCITY).
   */
  var controlType: ControlType = ControlType.THROTTLE
    private set

  /**
   * Raw motor power (throttle) in the range [-1.0, 1.0].
   *
   * This backing field is managed by the caching delegate to reduce
   * redundant hardware writes.
   */
  private var power by Caching(cacheTolerance) {
    if (it != null) {
      motor.power = it
    }
  }

  /**
   * Motor rotation direction (FORWARD or REVERSE).
   *
   * When set, automatically updates the underlying motor's direction.
   */
  var direction = NextMotor.Direction.FORWARD
    set(value) {
      field = value
      motor.direction = value.sdkDirection
    }

  /**
   * Current encoder position in physical distance units.
   *
   * Computed from the raw encoder count scaled by [distancePerCount].
   */
  val encoderPosition: Distance
    get() = (distancePerCount * motor.currentPosition) as Distance

  /**
   * Current encoder velocity in physical distance per time units.
   *
   * Computed from the raw motor velocity scaled by [distancePerCount]
   * and divided by 1 second to convert to the expected time unit.
   */
  val encoderVelocity: LinearVelocity
    get() = distancePerCount * motor.velocity / 1.0.seconds

  /**
   * Set throttle control mode and power.
   *
   * This is the simplest control mode: power is applied directly.
   *
   * @param throttle Power in the range [-1.0, 1.0]. Positive is forward.
   */
  fun setThrottle(throttle: Double) {
    controlType = ControlType.THROTTLE
    power = throttle
  }

  /**
   * Set voltage control mode and voltage setpoint.
   *
   * Adjusts power to maintain the specified voltage, accounting for the
   * current input rail voltage (to remain relatively hardware-independent).
   *
   * @param voltage Target voltage setpoint.
   */
  fun setVoltage(voltage: Voltage) {
    controlType = ControlType.VOLTAGE
    power = (voltage / RobotController.inputVoltage).magnitude
  }

  /**
   * Set position control mode and position setpoint.
   *
   * Uses [positionPID] to calculate closed-loop power and adds a static
   * friction term ([positionConstants.kS]) scaled by the sign of error.
   *
   * @param setpoint Target encoder position.
   */
  fun setPositionSetpoint(setpoint: Distance) {
    controlType = ControlType.POSITION
    power =
      positionPID.calculate(
        reference = setpoint.magnitude,
        measured = encoderPosition.into(setpoint.unit),
      ) +
      positionConstants.kS * (setpoint.magnitude - encoderPosition.magnitude).sign
  }

  /**
   * Set velocity control mode and velocity setpoint.
   *
   * Uses [velocityPID] and [velocityFF] to calculate closed-loop power.
   * The feedforward component helps overcome friction and inertia.
   *
   * @param setpoint Target velocity.
   */
  fun setVelocitySetpoint(setpoint: LinearVelocity) {
    controlType = ControlType.VELOCITY
    power =
      velocityPID.calculate(
        reference = setpoint.magnitude,
        measured = encoderVelocity.into(setpoint.unit),
      ) +
      velocityFF.calculate(setpoint.magnitude)
  }

  /**
   * Motor control mode.
   *
   * Determines how the motor interprets and uses the power value.
   */
  enum class ControlType {
    /**
     * Direct power/duty cycle control. Power is applied as-is.
     */
    THROTTLE,

    /**
     * Voltage-based control. Power is adjusted to account for rail voltage.
     */
    VOLTAGE,

    /**
     * Closed-loop position tracking using PID and feedforward.
     */
    POSITION,

    /**
     * Closed-loop velocity tracking using PID and feedforward.
     */
    VELOCITY,
  }

  /**
   * Motor rotation direction.
   */
  enum class Direction(val sdkDirection: DcMotorSimple.Direction) {
    /**
     * Motor spins forward (in its default/positive direction).
     */
    FORWARD(DcMotorSimple.Direction.FORWARD),

    /**
     * Motor spins in reverse (negated).
     */
    REVERSE(DcMotorSimple.Direction.REVERSE),
  }
}

/**
 * Tuning constants for position control.
 *
 * Exposes PID gains (kP, kI, kD), feedforward gains (kS, kV, kA), and additional
 * compensation terms (kG for gravity, kCos/kCosRatio for cosine compensation).
 *
 * @param pidConstants The underlying PID coefficient holder.
 * @param ffCoefficients The underlying feedforward coefficient holder.
 * @param kG Gravity compensation constant. Defaults to 0.0.
 * @param kCos Cosine amplitude compensation. Defaults to 0.0.
 * @param kCosRatio Ratio for cosine compensation (e.g., linkage ratio). Defaults to 0.0.
 */
data class MotorPositionConstants(
  internal val pidConstants: PIDCoefficients = PIDCoefficients(0.0),
  internal val ffCoefficients: SimpleFFCoefficients = SimpleFFCoefficients(0.0, 0.0, 0.0),
  @JvmField var kG: Double = 0.0,
  @JvmField var kCos: Double = 0.0,
  @JvmField var kCosRatio: Double = 0.0,
) {
  /** Proportional gain for position PID. */
  var kP by pidConstants::kP

  /** Integral gain for position PID. */
  var kI by pidConstants::kI

  /** Derivative gain for position PID. */
  var kD by pidConstants::kD

  /** Static friction compensation for position control. */
  var kS by ffCoefficients::kS

  /** Velocity feedforward gain for position control. */
  var kV by ffCoefficients::kV

  /** Acceleration feedforward gain for position control. */
  var kA by ffCoefficients::kA
}

/**
 * Tuning constants for velocity control.
 *
 * Exposes PID gains (kP, kI, kD) and feedforward gains (kS, kV, kA).
 *
 * @param pidConstants The underlying PID coefficient holder.
 * @param ffCoefficients The underlying feedforward coefficient holder.
 */
data class MotorVelocityConstants(
  internal val pidConstants: PIDCoefficients = PIDCoefficients(0.0, 0.0, 0.0),
  internal val ffCoefficients: SimpleFFCoefficients = SimpleFFCoefficients(0.0, 0.0, 0.0),
) {
  /** Proportional gain for velocity PID. */
  var kP by pidConstants::kP

  /** Integral gain for velocity PID. */
  var kI by pidConstants::kI

  /** Derivative gain for velocity PID. */
  var kD by pidConstants::kD

  /** Static friction compensation for velocity control. */
  var kS by ffCoefficients::kS

  /** Velocity feedforward gain for velocity control. */
  var kV by ffCoefficients::kV

  /** Acceleration feedforward gain for velocity control. */
  var kA by ffCoefficients::kA
}
