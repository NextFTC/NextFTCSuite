@file:JvmName("UnitConversionsNFTC")
package dev.nextftc.hardware.util

import dev.nextftc.control.geometry.Pose2d
import dev.nextftc.units.Amperes
import dev.nextftc.units.Celsius
import dev.nextftc.units.Centimeters
import dev.nextftc.units.Degrees
import dev.nextftc.units.Fahrenheit
import dev.nextftc.units.Inches
import dev.nextftc.units.Kelvin
import dev.nextftc.units.Meters
import dev.nextftc.units.Milliamperes
import dev.nextftc.units.Millimeters
import dev.nextftc.units.Millivolts
import dev.nextftc.units.Radians
import dev.nextftc.units.Seconds
import dev.nextftc.units.Volts
import dev.nextftc.units.unittypes.TimeUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit
import dev.nextftc.units.unittypes.AngularVelocityUnit

/**
 * Convert the official library [DistanceUnit] into the `units` library equivalent.
 */
val DistanceUnit.nftc get() =
    when (this) {
        DistanceUnit.INCH -> Inches
        DistanceUnit.MM -> Millimeters
        DistanceUnit.CM -> Centimeters
        DistanceUnit.METER -> Meters
    }

/**
 * Combines this unit with a unit of time to create a velocity unit.
 *
 * @param time the unit of time
 *
 * @return the combined velocity unit
 */
fun DistanceUnit.per(time: TimeUnit) = nftc.per(time)
/**
 * Convert the official library [AngleUnit] into the `units` library equivalent.
 */
val AngleUnit.nftc get() =
    when (this) {
        AngleUnit.DEGREES -> Degrees
        AngleUnit.RADIANS -> Radians
    }

/**
 * Combines this unit with a unit of time to create a velocity unit.
 *
 * @param time the unit of time
 *
 * @return the combined velocity unit
 */
fun AngleUnit.per(time: TimeUnit) = nftc.per(time)

/**
 * Convert the official library [UnnormalizedAngleUnit] into the `units` library equivalent.
 */
val UnnormalizedAngleUnit.nftc get() =
    when (this) {
        UnnormalizedAngleUnit.DEGREES -> Degrees
        UnnormalizedAngleUnit.RADIANS -> Radians
    }

/**
 * Combines this unit with a unit of time to create a velocity unit.
 *
 * @param time the unit of time
 *
 * @return the combined velocity unit
 */
fun UnnormalizedAngleUnit.per(time: TimeUnit) = nftc.per(time)

/**
 * Convert the official library [CurrentUnit] into the `units` library equivalent.
 */
val CurrentUnit.nftc get() =
    when (this) {
        CurrentUnit.AMPS -> Amperes
        CurrentUnit.MILLIAMPS -> Milliamperes
    }

/**
 * Convert the official library [VoltageUnit] into the `units` library equivalent.
 */
val VoltageUnit.nftc get() =
    when (this) {
        VoltageUnit.VOLTS -> Volts
        VoltageUnit.MILLIVOLTS -> Millivolts
    }

/**
 * Convert the official library [TempUnit] into the `units` library equivalent.
 */
val TempUnit.nftc get() =
    when (this) {
        TempUnit.CELSIUS -> Celsius
        TempUnit.FARENHEIT -> Fahrenheit
        TempUnit.KELVIN -> Kelvin
    }

/**
 * Get the [AngularVelocityUnit] for this [AngularVelocity].
 */
val AngularVelocity.perUnit get() = this.angleUnit.per(Seconds)

/**
 * Returns a [Pose2d] with the same values as this [Pose2D].
 * The distance unit is inches, and the angle unit is radians.
 */
val Pose2D.nftc get() =
    Pose2d(getX(DistanceUnit.INCH), getY(DistanceUnit.INCH), getHeading(AngleUnit.RADIANS))