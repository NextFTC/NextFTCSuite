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
 * A [NextServo] paired with an analog feedback input for reading the servo's
 * actual angle. Useful for servos with a feedback wire (e.g. Axon).
 *
 * Inherits everything from [NextServo] — `position`, `pwmRange`, `enable()`,
 * `disable()` — and adds [angleInRadians] or [angleInDegrees] for reading the physical angle in
 * radians or degrees respectively from the feedback input.
 *
 * Example:
 * val arm = NextFeedbackServo("armServo", "armEncoder")
 * arm.position = 0.5
 * val angle = arm.angleInRadians  // where it actually is, in RADIANS
 *
 * @param servoName Hardware map name of the servo.
 * @param feedbackName Hardware map name of the analog input.
 * @param cacheTolerance Tolerance for the [NextServo] position caching delegate.
 */
class NextFeedbackServo(
    servoName: String,
    feedbackName: String,
    cacheTolerance: Double = 0.01,
    ) : NextServo(servoName, cacheTolerance) {

    private val analogInput by LazyHardware {
        RobotController.hardwareMap[feedbackName] as AnalogInput
    }

    /** Actual angle of the servo, in RADIANS. */
    val angleInRadians: Double by AnalogFeedback { analogInput.voltage }

    /** Actual angle of the servo, in DEGREES. */
    val angleInDegrees: Double get() = Math.toDegrees(angleInRadians)

}