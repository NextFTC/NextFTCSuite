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
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.motorController
import dev.nextftc.hardware.util.AnalogFeedback
import dev.nextftc.hardware.util.Caching
import dev.nextftc.hardware.util.EventLoop
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.AngularVelocity
import dev.nextftc.units.radians
import dev.nextftc.units.seconds
import kotlin.math.sign
import dev.nextftc.units.measuretypes.Voltage as VoltageMeasure

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
 * Encoder readings are automatically converted to [Angle] and [AngularVelocity] using
 * the configured [anglePerCount] conversion factor.
 *
 * Example:
 * ```
 * val motor = NextMotor("driveMotor", anglePerCount = 0.05.radians)
 * motor.setVelocitySetpoint(12.0.radiansPerSecond)
 * ```
 *
 * @param initializer A function that returns the backing [DcMotorImplEx]. Invoked lazily.
 * @param anglePerCount Conversion factor from encoder counts to angle. Defaults to 1.0 radian.
 * @param cacheTolerance Power caching tolerance to reduce redundant hardware writes. Defaults to 0.01.
 */
class NextMotor @JvmOverloads constructor(
  initializer: () -> DcMotorImplEx,
  var anglePerCount: Angle = 1.0.radians,
  cacheTolerance: Double = 0.01,
) {
  /**
   * Constructs a motor from a Lynx hub module and port number.
   *
   * Wraps the port within the given module's motor controller.
   *
   * @param module The Lynx module housing this motor port.
   * @param port The motor port on the module (0-based).
   * @param anglePerCount Encoder count to angle conversion factor.
   * @param cacheTolerance Power caching tolerance.
   */
  @JvmOverloads constructor(
    module: LynxModule,
    port: Int,
    anglePerCount: Angle = 1.0.radians,
    cacheTolerance: Double = 0.01,
  ) : this(
    { DcMotorImplEx(module.motorController, port) },
    anglePerCount,
    cacheTolerance,
  )

  /**
   * Constructs a motor by name from the current hardware map.
   *
   * Looks up the motor in [RobotController.hardwareMap].
   *
   * @param name Hardware map device name.
   * @param anglePerCount Encoder count to angle conversion factor.
   * @param cacheTolerance Power caching tolerance.
   */
  @JvmOverloads constructor(
    name: String,
    anglePerCount: Angle = 1.0.radians,
    cacheTolerance: Double = 0.01,
  ) : this({ RobotController.hardwareMap[name] as DcMotorImplEx }, anglePerCount, cacheTolerance)

  init {
    motorEventLoop.bind(this::update)
  }

  private val lazyMotor = LazyHardware(initializer)
  private val motor by lazyMotor

  /**
   * Position control constants (PID and feedforward gains).
   *
   * Modify [MotorPositionConstants.kP], [MotorPositionConstants.kI], [MotorPositionConstants.kD],
   * [MotorPositionConstants.kS], [MotorPositionConstants.kV], [MotorPositionConstants.kA]
   * properties and gravity/cos terms to tune position setpoint tracking.
   */
  val positionConstants: MotorPositionConstants = MotorPositionConstants()

  /**
   * PID controller used for position setpoint tracking.
   */
  val positionPID = PIDController(positionConstants.pidConstants)

  /**
   * Velocity control constants (PID and feedforward gains).
   *
   * Modify [MotorVelocityConstants.kP], [MotorVelocityConstants.kI], [MotorVelocityConstants.kD],
   * [MotorVelocityConstants.kS], [MotorVelocityConstants.kV], [MotorVelocityConstants.kA]
   * properties to tune velocity setpoint tracking.
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
  var controlType: ControlType = ControlType.Throttle(0.0)
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
  var direction = Direction.FORWARD
    set(value) {
      field = value
      if (lazyMotor.isInitialized) {
        motor.direction = value.sdkDirection
      } else {
        lazyMotor.applyAfterInit { it.direction = value.sdkDirection }
      }
    }

  /**
   * Current encoder position in physical angle units.
   *
   * Computed from the raw encoder count scaled by [anglePerCount].
   */
  val encoderPosition: Angle
    get() = (anglePerCount * motor.currentPosition) as Angle

  /**
   * Optional external absolute encoder via analog feedback.
   *
   * If provided, [absoluteEncoderPosition] will return this encoder's reading.
   * If null, [absoluteEncoderPosition] will fall back to the built-in [encoderPosition].
   */
  var absoluteEncoder: AnalogFeedback? = null

  /**
   * Current absolute encoder position.
   *
   * Reads from [absoluteEncoder] if one is configured; otherwise falls back
   * to the built-in relative [encoderPosition].
   */
  val absoluteEncoderPosition: Angle
    get() = absoluteEncoder?.getValue(this, ::absoluteEncoder)?.radians ?: encoderPosition

  /**
   * Current encoder velocity in physical angle per time units.
   *
   * Computed from the raw motor velocity scaled by [anglePerCount]
   * and divided by 1 second to convert to the expected time unit.
   */
  val encoderVelocity: AngularVelocity
    get() = anglePerCount * motor.velocity / 1.0.seconds

  /**
   * Updates the motor power based on the current active control mode.
   *
   * This method should be called periodically (e.g., in an EventLoop)
   * to recalculate closed-loop controllers.
   */
  fun update() {
    when (val mode = controlType) {
      is ControlType.Throttle -> {
        power = mode.throttle
      }
      is ControlType.Voltage -> {
        power = (mode.voltage / RobotController.inputVoltage).magnitude
      }
      is ControlType.Position -> {
        positionPID.disableContinuousInput()
        val setpoint = mode.setpoint
        power =
          positionPID.calculate(
            reference = setpoint.magnitude,
            measured = encoderPosition.into(setpoint.unit),
          ) +
          positionConstants.kS * positionPID.error.sign +
          positionConstants.kG +
          positionConstants.kCos * kotlin.math.cos(encoderPosition.magnitude * positionConstants.kCosRatio)
      }
      is ControlType.AbsolutePosition -> {
        val setpoint = mode.setpoint
        positionPID.enableContinuousInput(
          minimumInput = setpoint.unit.fromBaseUnits(-Math.PI),
          maximumInput = setpoint.unit.fromBaseUnits(Math.PI),
        )
        val measuredPos = absoluteEncoderPosition.into(setpoint.unit)
        power =
          positionPID.calculate(
            reference = setpoint.magnitude,
            measured = measuredPos,
          ) +
          positionConstants.kS * positionPID.error.sign +
          positionConstants.kG +
          positionConstants.kCos *
          kotlin.math.cos(absoluteEncoderPosition.magnitude * positionConstants.kCosRatio)
      }
      is ControlType.Velocity -> {
        val setpoint = mode.setpoint
        power =
          velocityPID.calculate(
            reference = setpoint.magnitude,
            measured = encoderVelocity.into(setpoint.unit),
          ) +
          velocityFF.calculate(setpoint.magnitude)
      }
      is ControlType.Follow -> {
        power = if (mode.direction == Direction.FORWARD) {
          mode.motor.power
        } else {
          -mode.motor.power
        }
      }
    }
  }

  /**
   * Set throttle control mode and power.
   *
   * This is the simplest control mode: power is applied directly.
   *
   * @param throttle Power in the range [-1.0, 1.0]. Positive is forward.
   */
  fun setThrottle(throttle: Double) {
    controlType = ControlType.Throttle(throttle)
  }

  /**
   * Set voltage control mode and voltage setpoint.
   *
   * Adjusts power to maintain the specified voltage, accounting for the
   * current input rail voltage (to remain relatively hardware-independent).
   *
   * @param voltage Target voltage setpoint.
   */
  fun setVoltage(voltage: VoltageMeasure) {
    controlType = ControlType.Voltage(voltage)
  }

  /**
   * Set position control mode and position setpoint.
   *
   * Uses [positionPID] to calculate closed-loop power and adds a static
   * friction term ([MotorPositionConstants.kS]) scaled by the sign of error.
   * This mode targets a relative setpoint that tracks total rotations.
   *
   * @param setpoint Target encoder position.
   */
  fun setPositionSetpoint(setpoint: Angle) {
    controlType = ControlType.Position(setpoint)
  }

  /**
   * Set absolute position control mode and position setpoint.
   *
   * Similar to standard position control, but wraps the setpoint and measured position
   * to a continuous range (typically [-180, 180] degrees) to find the shortest path
   * to the target angle, ignoring the number of full rotations the motor has made.
   *
   * @param setpoint Target absolute angle within one rotation.
   */
  fun setAbsolutePositionSetpoint(setpoint: Angle) {
    controlType = ControlType.AbsolutePosition(setpoint)
  }

  /**
   * Set velocity control mode and velocity setpoint.
   *
   * Uses [velocityPID] and [velocityFF] to calculate closed-loop power.
   * The feedforward component helps overcome friction and inertia.
   *
   * @param setpoint Target velocity.
   */
  fun setVelocitySetpoint(setpoint: AngularVelocity) {
    controlType = ControlType.Velocity(setpoint)
  }

  /**
   * Configures this motor to follow another motor's behavior and align its rotation direction.
   *
   * The following motor uses the provided [NextMotor] instance and the specified [Direction]
   * to align its movements with the target motor.
   *
   * @param motor The motor to follow. This motor will mimic the behavior and control mode
   *              of the provided motor.
   * @param direction The rotation direction to use when following the target motor. Defaults
   *                  to [Direction.FORWARD].
   */
  @JvmOverloads fun follow(motor: NextMotor, direction: Direction = Direction.FORWARD) {
    controlType = ControlType.Follow(motor, direction)
  }

  /**
   * Motor control mode.
   *
   * Determines how the motor interprets and uses the power value.
   */
  sealed class ControlType {

    /**
     * Direct power/duty cycle control. Power is applied as-is.
     */
    data class Throttle(val throttle: Double) : ControlType() {
      init {
        require(throttle in -1.0..1.0)
      }
    }

    /**
     * Voltage-based control. Power is adjusted to account for rail voltage.
     */
    data class Voltage(val voltage: VoltageMeasure) : ControlType()

    /**
     * Closed-loop position tracking using PID and feedforward.
     */
    data class Position(val setpoint: Angle) : ControlType()

    /**
     * Continuous closed-loop absolute position tracking using PID and feedforward.
     * Takes the shortest path to the angle regardless of current rotations.
     */
    data class AbsolutePosition(val setpoint: Angle) : ControlType()

    /**
     * Closed-loop velocity tracking using PID and feedforward.
     */
    data class Velocity(val setpoint: AngularVelocity) : ControlType()

    /**
     * Follows another motor's behavior.
     */
    data class Follow(val motor: NextMotor, val direction: Direction) : ControlType()
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

  companion object {
    val motorEventLoop: EventLoop = EventLoop()
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
  var kG: Double = 0.0,
  var kCos: Double = 0.0,
  var kCosRatio: Double = 0.0,
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

  fun withP(kP: Double) = apply { this.kP = kP }
  fun withI(kI: Double) = apply { this.kI = kI }
  fun withD(kD: Double) = apply { this.kD = kD }
  fun withS(kS: Double) = apply { this.kS = kS }
  fun withV(kV: Double) = apply { this.kV = kV }
  fun withA(kA: Double) = apply { this.kA = kA }
  fun withG(kG: Double) = apply { this.kG = kG }
  fun withCos(kCos: Double) = apply { this.kCos = kCos }
  fun withCosRatio(kCosRatio: Double) = apply { this.kCosRatio = kCosRatio }
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

  fun withP(kP: Double) = apply { this.kP = kP }
  fun withI(kI: Double) = apply { this.kI = kI }
  fun withD(kD: Double) = apply { this.kD = kD }
  fun withS(kS: Double) = apply { this.kS = kS }
  fun withV(kV: Double) = apply { this.kV = kV }
  fun withA(kA: Double) = apply { this.kA = kA }
}
