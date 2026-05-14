/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.servos

import com.qualcomm.robotcore.hardware.AnalogInput
import dev.nextftc.hardware.AnalogFeedback
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * A [NextCRServo] paired with an analog feedback input for reading the servo's
 * actual angle. Useful for continuous-rotation servos with feedback wires
 * (e.g. Axon CR) where you want to know how far the servo has rotated.
 *
 * Inherits everything from [NextCRServo] — `power`, `direction`, `reverse()`,
 * `enable()`, `disable()` — and adds [angleInRadians] or [angleInDegrees] for reading the physical angle in
 * radians or degrees respectively from the feedback input.
 *
 * @param servoName Hardware map name of the servo.
 * @param feedbackName Hardware map name of the analog input.
 * @param cacheTolerance Tolerance for the [NextCRServo] power caching delegate.
 */
class NextFeedbackCRServo(
    servoName: String,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
    ) : NextCRServo(servoName, cacheTolerance) {

    private val analogInput by LazyHardware {
        RobotController.hardwareMap[feedbackName] as AnalogInput
    }

    /** Actual angle of the servo, in RADIANS. */
    val angleInRadians: Double by AnalogFeedback { analogInput.voltage }

    /** Actual angle of the servo, in DEGREES. */
    val angleInDegrees: Double get() = Math.toDegrees(angleInRadians)
}