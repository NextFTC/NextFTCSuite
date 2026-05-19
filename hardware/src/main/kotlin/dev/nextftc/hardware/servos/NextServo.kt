/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.servos

import com.qualcomm.robotcore.hardware.PwmControl
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.ServoImplEx
import dev.nextftc.hardware.Caching
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * Lightweight wrapper around a [ServoImplEx] that provides a more user-friendly interface for controlling servo position and PWM range.
 * controls how sensitive the [position] caching delegate is to small changes.
 *
 * Example:
 *
 * val servo = NextServo("armServo")
 * servo.position = 0.5
 * servo.setPwmRange(500.0, 2500.0)
 *
 * @param initializer A function returning the backing [ServoImplEx]. It will be
 * invoked lazily the first time the servo is accessed.
 * @param cacheTolerance Tolerance used by the [Caching] delegate for
 * position updates; defaults to 0.01.
 */
open class NextServo(initializer: () -> ServoImplEx, val cacheTolerance: Double = 0.01) {
  @JvmOverloads constructor(name: String, cacheTolerance: Double = 0.01) : this(
    { RobotController.hardwareMap[name] as ServoImplEx },
    cacheTolerance,
  )

  private val servo by LazyHardware(initializer)

  val position: Double by Caching(cacheTolerance) {
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
  var pwmRange
    get() = servo.pwmRange
    set(value) {
      servo.pwmRange = value
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

  fun enable() {
    servo.setPwmEnable()
  }

  fun disable() {
    servo.setPwmDisable()
  }
}
