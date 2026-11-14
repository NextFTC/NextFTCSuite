@file:JvmName("UnitConversionsFTC")

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
import dev.nextftc.units.Volts
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.CurrentUnit
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.TemperatureUnit
import dev.nextftc.units.unittypes.VoltageUnit
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.DEGREES
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit.AMPS
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit.MILLIAMPS
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.CM
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.INCH
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.METER
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit.MM
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit.CELSIUS
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit.FARENHEIT
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit.KELVIN
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit.VOLTS

/**
 * Convert the `units` library [DistanceUnit] into the official library equivalent, or null if there is not a corresponding unit.
 */
val DistanceUnit.ftc get() =
  when (this) {
    Inches -> INCH
    Millimeters -> MM
    Centimeters -> CM
    Meters -> METER
    else -> null
  }

/**
 * Convert the `units` library [AngleUnit] into the official library equivalent, or null if there is not a corresponding unit.
 */
val AngleUnit.ftc get() =
  when (this) {
    Degrees -> DEGREES
    Radians -> RADIANS
    else -> null
  }

/**
 * Convert the `units` library [CurrentUnit] into the official library equivalent, or null if there is not a corresponding unit.
 */
val CurrentUnit.ftc get() =
  when (this) {
    Amperes -> AMPS
    Milliamperes -> MILLIAMPS
    else -> null
  }

/**
 * Convert the `units` library [VoltageUnit] into the official library equivalent, or null if there is not a corresponding unit.
 */
val VoltageUnit.ftc get() =
  when (this) {
    Volts -> VOLTS
    Millivolts -> Millivolts
    else -> null
  }

/**
 * Convert the `units` library [TemperatureUnit] into the official library equivalent, or null if there is not a corresponding unit.
 */
val TemperatureUnit.ftc get() =
  when (this) {
    Celsius -> CELSIUS
    Fahrenheit -> FARENHEIT
    Kelvin -> KELVIN
    else -> null
  }

/**
 * Convert this [Pose2d] into the official library equivalent.
 */
val Pose2d.ftc get(): Pose2D {
  val unit = position.x.unit.ftc ?: INCH
  return Pose2D(
    unit,
    position.x.into(unit.nftc),
    position.y.into(unit.nftc),
    RADIANS,
    heading.log(),
  )
}
