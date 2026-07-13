/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.servoController
import dev.nextftc.hardware.util.Caching
import dev.nextftc.hardware.util.LazyHardware

/**
 * Lightweight wrapper around a [ServoImplEx] that provides a more user-friendly interface for controlling servo position and PWM range.
 * controls how sensitive the [position] caching delegate is to small changes.
 *
 * Example:
 * ```
 * val servo = NextServo("armServo")
 * servo.position = 0.5
 * servo.setPwmRange(500.0, 2500.0)
 * ```
 *
 * @param initializer A function returning the backing [ServoImplEx]. It will be
 * invoked lazily the first time the servo is accessed.
 * @param cacheTolerance Tolerance used by the [Caching] delegate for
 * position updates; defaults to 0.01.
 * @param direction The initial direction of the servo defined by the user
 * as a [Servo.Direction] which defaults to [Servo.Direction.FORWARD]
 */
open class NextServo @JvmOverloads constructor(
  initializer: () -> ServoImplEx,
  val cacheTolerance: Double = 0.01,
  direction: Servo.Direction = Servo.Direction.FORWARD,
) {
  /**
   * Constructor to create a NextServo using a LynxModule and port number.
   *
   * Example:
   * ```
   * val servo = NextServo(RobotController.controlHub, 0) // Creates a NextServo on the Control Hub port 0
   * ```
   *
   * @param module The Lynx Module, see [RobotController.controlHub], [RobotController.expansionHub],
   * and [RobotController.servoHubs]
   * @param port The servo port (in the range [0, 5])
   * @param cacheTolerance Tolerance used by the [Caching] delegate for position updates; defaults to 0.01.
   * @param direction The initial direction of the servo defined by the user
   * as a [Servo.Direction] which defaults to [Servo.Direction.FORWARD]
   */
  @JvmOverloads constructor(
    module: LynxModule,
    port: Int,
    cacheTolerance: Double = 0.01,
    direction: Servo.Direction = Servo.Direction.FORWARD,
  ) : this(
    { ServoImplEx(module.servoController, port, ServoConfigurationType.getStandardServoType()) },
    cacheTolerance,
    direction,
  )

  /**
   * Constructor to create a NextServo using configuration name
   *
   * Example:
   * ```
   * // Creates a NextServo with the config name: "armServo"
   * val servo = NextServo("armServo")
   * ```
   *
   * @param name The configuration name for the servo, usually found on the Driver Station
   * @param cacheTolerance Tolerance used by the [Caching] delegate for position updates; defaults to 0.01.
   * @param direction The initial direction of the servo defined by the user
   * as a [Servo.Direction] which defaults to [Servo.Direction.FORWARD]
   */
  @JvmOverloads constructor(
    name: String,
    cacheTolerance: Double = 0.01,
    direction: Servo.Direction = Servo.Direction.FORWARD,
  ) : this(
    { RobotController.hardwareMap[name] as ServoImplEx },
    cacheTolerance,
    direction,
  )

  private val lazyServo = LazyHardware(initializer).apply {
    applyAfterInit { it.direction = direction }
  }
  private val servo by lazyServo

  /**
   * Allows user to change servo's direction configuration
   *
   * It updates the internal direction of the servo
   *
   * Getter returns the current direction of the servo
   * Setter lazily updates the servo after initialization
   *
   * This property allows indirect access to the direction property of the [Servo] class
   */
  var direction: Servo.Direction
    get() = servo.direction
    set(direction) {
      if (lazyServo.isInitialized) {
        servo.direction = direction
      } else {
        lazyServo.applyAfterInit { it.direction = direction }
      }
    }

  /**
   * The commanded servo position in the range `[0.0, 1.0]`.
   *
   * Assigning a value writes through to the backing [ServoImplEx], while reads
   * are handled by the [Caching] delegate.
   */
  var position: Double by Caching(cacheTolerance) {
    if (it != null) {
      servo.position = it
    }
  }

  /**
   * Provides access to the servo's PWM range configuration.
   *
   * This property allows reading and updating the current PWM range of the associated servo.
   * The PWM range determines the range of pulse signals that the servo can accept and is used to control its behavior.
   *
   * The getter retrieves the current PWM range from the servo, while the setter updates it with a new range.
   *
   * This property is backed by the `pwmRange` property of the `ServoImplEx` instance associated with this servo.
   */
  var pwmRange: PwmControl.PwmRange
    get() = servo.pwmRange
    set(range) {
      if (lazyServo.isInitialized) {
        servo.pwmRange = range
      } else {
        lazyServo.applyAfterInit { it.pwmRange = range }
      }
    }

  /**
   * Sets the PWM range of the associated servo.
   *
   * This method updates the PWM range of the servo to the specified range.
   *
   * @param lower The lower bound of the PWM range (in microseconds).
   * @param upper The upper bound of the PWM range (in microseconds).
   */
  fun setPwmRange(lower: Double, upper: Double) {
    pwmRange = PwmControl.PwmRange(lower, upper)
  }

  /**
   * Enables PWM output for the underlying servo.
   */
  fun enable() {
    servo.setPwmEnable()
  }

  /**
   * Disables PWM output for the underlying servo.
   */
  fun disable() {
    servo.setPwmDisable()
  }
}
