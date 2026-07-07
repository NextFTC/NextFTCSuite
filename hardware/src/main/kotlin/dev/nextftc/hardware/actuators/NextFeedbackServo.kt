/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.ServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import dev.nextftc.hardware.AnalogFeedback
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.servoController
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.radians

/**
 * A [NextServo] paired with an analog feedback input for reading the servo's
 * actual angle. Useful for servos with a feedback wire (e.g. Axon).
 *
 * Inherits everything from [NextServo] — `position`, `pwmRange`, `enable()`,
 * `disable()` — and adds [angle] for reading the physical angle in
 * radians from the feedback input.
 *
 * Example:
 *
 *  ```
 * val arm = NextFeedbackServo("armServo", "armEncoder")
 * arm.position = 0.5
 * val angle = arm.angle
 * ```
 *
 * @param initializer A function returning the backing [ServoImplEx]. It will be
 * invoked lazily the first time the servo is accessed.
 * @param feedbackName Hardware map name of the analog input.
 * @param cacheTolerance Tolerance for the [NextServo] position caching delegate.
 */
class NextFeedbackServo(
  initializer: () -> ServoImplEx,
  feedbackName: String,
  cacheTolerance: Double = 0.01,
) : NextServo(initializer, cacheTolerance) {

  /**
   * Constructor to create a NextFeedbackServo using a servo name.
   *
   * @param servoName Hardware map name of the servo.
   * @param feedbackName Hardware map name of the analog input.
   * @param cacheTolerance Tolerance for the [NextServo] position caching delegate.
   */
  @JvmOverloads constructor(
    servoName: String,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
  ) : this(
    { RobotController.hardwareMap[servoName] as ServoImplEx },
    feedbackName,
    cacheTolerance,
  )

  /**
   * Constructor to create a NextFeedbackServo using a LynxModule and port number.
   *
   * @param module The Lynx module, see [RobotController.controlHub], [RobotController.expansionHub],
   * and [RobotController.servoHubs].
   * @param port The servo port (in the range [0, 5]).
   * @param feedbackName Hardware map name of the analog input.
   * @param cacheTolerance Tolerance for the [NextServo] position caching delegate.
   */
  @JvmOverloads constructor(
    module: LynxModule,
    port: Int,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
  ) : this(
    { ServoImplEx(module.servoController, port, ServoConfigurationType.getStandardServoType()) },
    feedbackName,
    cacheTolerance,
  )

  private val analogInput by LazyHardware {
    RobotController.hardwareMap[feedbackName] as AnalogInput
  }

  private val rawAngleRadians: Double by AnalogFeedback { analogInput.voltage }

  /**
   * Actual angle of the servo, reported from the analog feedback input.
   *
   * The value is returned as a typed [Angle].
   */
  val angle: Angle get() = rawAngleRadians.radians
}
