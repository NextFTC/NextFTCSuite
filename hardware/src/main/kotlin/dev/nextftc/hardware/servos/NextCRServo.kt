/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.servos

import com.qualcomm.robotcore.hardware.CRServoImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import dev.nextftc.hardware.Caching
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * Lightweight wrapper around a [CRServoImplEx] that provides a more user-friendly
 * interface for controlling continuous-rotation servo power and direction.
 *
 * Example:
 * val crServo = NextCRServo("intakeServo")
 * crServo.power = 0.75
 * crServo.direction = DcMotorSimple.Direction.REVERSE
 *
 * @param initializer A function returning the backing [CRServoImplEx]. It will be
 * invoked lazily the first time the servo is accessed.
 * @param cacheTolerance Tolerance used by the [Caching] delegate for
 * power updates; defaults to 0.01.
 */
open class NextCRServo(initializer: () -> CRServoImplEx, val cacheTolerance: Double = 0.01) {
    @JvmOverloads constructor(name: String, cacheTolerance: Double = 0.01) : this(
        { RobotController.hardwareMap[name] as CRServoImplEx },
        cacheTolerance,
    )

    private val servo by LazyHardware(initializer)

    /**
     * Power applied to the servo, in the range [-1.0, 1.0].
     */
    val power: Double by Caching(cacheTolerance) {
        if (it != null) {
            servo.power = it
        }
    }

    /**
     * Direction of the servo. Setting this to [DcMotorSimple.Direction.REVERSE]
     * causes positive [power] values to spin the servo the opposite way,
     * and vice versa.
     */
    var direction: DcMotorSimple.Direction
        get() = servo.direction
        set(value) {
            servo.direction = value
        }

    /**
     * Sets the servo's direction to [DcMotorSimple.Direction.REVERSE], causing
     * positive power values to spin the servo the opposite way.
     */
    fun reverse() = apply {
        direction = DcMotorSimple.Direction.REVERSE
    }

    /**
     * Enables the PWM output of the associated servo.
     */
    fun enable() {
        servo.setPwmEnable()
    }

    /**
     * Disables the PWM output of the associated servo.
     */
    fun disable() {
        servo.setPwmDisable()
    }
}