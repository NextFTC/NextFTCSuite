/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.hardware.lynx.LynxServoController
import com.qualcomm.robotcore.hardware.AnalogInput
import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.configuration.typecontainers.ServoConfigurationType
import dev.nextftc.hardware.AnalogFeedback
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.servoController
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.radians

/**
 * A [NextCRServo] paired with an analog feedback input for reading the servo's
 * actual angle. Useful for continuous-rotation servos with feedback wires
 * (e.g. Axon CR) where you want to know how far the servo has rotated.
 *
 * Inherits everything from [NextCRServo] — `power`, `direction`, `reverse()`,
 * `enable()`, `disable()` — and adds [angle] for reading the physical angle in
 * radians from the feedback input.
 *
 * @param initializer A function returning the backing [CRServoImplEx]. It will
 * be invoked lazily the first time the servo is accessed.
 * @param feedbackName Hardware map name of the analog input.
 * @param cacheTolerance Tolerance for the [NextCRServo] power caching delegate.
 */
class NextFeedbackCRServo(
  initializer: () -> CRServoImplEx,
  feedbackName: String,
  cacheTolerance: Double = 0.01,
) : NextCRServo(initializer, cacheTolerance) {

  /**
   * Constructor to create a NextFeedbackCRServo using a servo name.
   *
   * @param servoName Hardware map name of the servo.
   * @param feedbackName Hardware map name of the analog input.
   * @param cacheTolerance Tolerance for the [NextCRServo] power caching delegate.
   */
  @JvmOverloads constructor(
    servoName: String,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
  ) : this(
    { RobotController.hardwareMap[servoName] as CRServoImplEx },
    feedbackName,
    cacheTolerance,
  )

  /**
   * Constructor to create a NextFeedbackCRServo using a LynxModule and port number.
   *
   * @param module The Lynx module, see [RobotController.controlHub], [RobotController.expansionHub],
   * and [RobotController.servoHubs].
   * @param port The servo port (in the range [0, 5]).
   * @param feedbackName Hardware map name of the analog input.
   * @param cacheTolerance Tolerance for the [NextCRServo] power caching delegate.
   */
  @JvmOverloads constructor(
    module: LynxModule,
    port: Int,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
  ) : this(
    {
      CRServoImplEx(
        module.servoController,
        port,
        ServoConfigurationType.getStandardServoType(),
      )
    },
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
   * The value is returned as a typed [Angle] in radians.
   */
  val angle: Angle get() = rawAngleRadians.radians
}
