/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Units")

package dev.nextftc.units

import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.AngularAccelerationUnit
import dev.nextftc.units.unittypes.AngularVelocityUnit
import dev.nextftc.units.unittypes.CurrentUnit
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.EnergyUnit
import dev.nextftc.units.unittypes.ForceUnit
import dev.nextftc.units.unittypes.LinearAccelerationUnit
import dev.nextftc.units.unittypes.LinearVelocityUnit
import dev.nextftc.units.unittypes.MassUnit
import dev.nextftc.units.unittypes.PowerUnit
import dev.nextftc.units.unittypes.TemperatureUnit
import dev.nextftc.units.unittypes.TimeUnit
import dev.nextftc.units.unittypes.TorqueUnit
import dev.nextftc.units.unittypes.VoltageUnit
import kotlin.time.DurationUnit

// Angle conversion constants
private const val DEGREES_PER_RADIAN = Math.PI / 180.0
private const val ROTATIONS_PER_RADIAN = 2.0 * Math.PI
private const val GRADIANS_PER_RADIAN = Math.PI / 200.0

// Time conversion constants
private const val MILLISECONDS_PER_SECOND = 1e-3
private const val MICROSECONDS_PER_SECOND = 1e-6
private const val NANOSECONDS_PER_SECOND = 1e-9
private const val MINUTES_PER_SECOND = 60.0
private const val HOURS_PER_MINUTE = 60.0
private const val DAYS_PER_HOUR = 24.0

// Distance conversion constants
private const val MILLIMETERS_PER_METER = 1e-3
private const val CENTIMETERS_PER_METER = 1e-2
private const val KILOMETERS_PER_METER = 1e3
private const val INCHES_PER_CM = 2.54
private const val FEET_PER_INCH = 12.0
private const val YARDS_PER_FOOT = 3.0
private const val MILES_PER_FOOT = 5280.0

// Current conversion constants
private const val MILLIAMPERES_PER_AMPERE = 1e-3
private const val MICROAMPERES_PER_AMPERE = 1e-6
private const val KILOAMPERES_PER_AMPERE = 1e3

// Voltage conversion constants
private const val MILLIVOLTS_PER_VOLT = 1e-3
private const val MICROVOLTS_PER_VOLT = 1e-6
private const val KILOVOLTS_PER_VOLT = 1e3

// Force conversion constants
private const val KILONEWTONS_PER_NEWTON = 1e3
private const val POUNDS_FORCE_PER_NEWTON = 4.4482216152605 // 1 lbf ≈ 4.448 N
private const val KILOGRAMS_FORCE_PER_NEWTON = 9.80665 // 1 kgf = 9.80665 N

// Torque conversion constants
private const val POUND_FEET_PER_NEWTON_METER = 1.3558179483314 // 1 lb·ft ≈ 1.356 N·m
private const val NEWTON_CENTIMETERS_PER_NEWTON_METER = 0.01 // 1 N·cm = 0.01 N·m
private const val NEWTON_MILLIMETERS_PER_NEWTON_METER = 0.001 // 1 N·mm = 0.001 N·m

// Energy conversion constants
private const val KILOJOULES_PER_JOULE = 1e3
private const val WATT_HOURS_PER_JOULE = 3600.0 // 1 Wh = 3600 J
private const val KILOWATT_HOURS_PER_JOULE = 3_600_000.0 // 1 kWh = 3,600,000 J

// Power conversion constants
private const val MILLIWATTS_PER_WATT = 1e-3
private const val KILOWATTS_PER_WATT = 1e3
private const val MEGAWATTS_PER_WATT = 1e6
private const val HORSEPOWER_PER_WATT = 745.699872 // Mechanical horsepower

// Mass conversion constants
private const val GRAMS_PER_KILOGRAM = 1e-3
private const val MILLIGRAMS_PER_KILOGRAM = 1e-6
private const val METRIC_TONS_PER_KILOGRAM = 1000.0
private const val POUNDS_PER_KILOGRAM = 0.45359237
private const val OUNCES_PER_POUND = 1.0 / 16.0

// Angle units
@JvmField
val Radians = AngleUnit(null, { it }, { it }, "radian", "rad")

@JvmField
val Degrees = AngleUnit(Radians, DEGREES_PER_RADIAN, "degree", "deg")

@JvmField
val Rotations = AngleUnit(Radians, ROTATIONS_PER_RADIAN, "rotation", "rot")

@JvmField
val Gradians = AngleUnit(Radians, GRADIANS_PER_RADIAN, "gradian", "grad")

// Time units
@JvmField
val Seconds = TimeUnit(null, { it }, { it }, "second", "s")

@JvmField
val Milliseconds = TimeUnit(Seconds, MILLISECONDS_PER_SECOND, "millisecond", "ms")

@JvmField
val Microseconds = TimeUnit(Seconds, MICROSECONDS_PER_SECOND, "microsecond", "us")

@JvmField
val Nanoseconds = TimeUnit(Seconds, NANOSECONDS_PER_SECOND, "nanosecond", "ns")

@JvmField
val Minutes = TimeUnit(Seconds, MINUTES_PER_SECOND, "minute", "min")

@JvmField
val Hours = TimeUnit(Minutes, HOURS_PER_MINUTE, "hour", "h")

@JvmField
val Days = TimeUnit(Hours, DAYS_PER_HOUR, "day", "d")

// Distance units
@JvmField
val Meters = DistanceUnit(null, { it }, { it }, "meter", "m")

@JvmField
val Millimeters = DistanceUnit(Meters, MILLIMETERS_PER_METER, "millimeter", "mm")

@JvmField
val Centimeters = DistanceUnit(Meters, CENTIMETERS_PER_METER, "centimeter", "cm")

@JvmField
val Kilometers = DistanceUnit(Meters, KILOMETERS_PER_METER, "kilometer", "km")

@JvmField
val Inches = DistanceUnit(Centimeters, INCHES_PER_CM, "inch", "in")

@JvmField
val Feet = DistanceUnit(Inches, FEET_PER_INCH, "foot", "ft")

@JvmField
val Yards = DistanceUnit(Feet, YARDS_PER_FOOT, "yard", "yd")

@JvmField
val Miles = DistanceUnit(Feet, MILES_PER_FOOT, "mile", "mi")

// Mass units
@JvmField
val Kilograms = MassUnit(null, { it }, { it }, "kilogram", "kg")

@JvmField
val Grams = MassUnit(Kilograms, GRAMS_PER_KILOGRAM, "gram", "g")

@JvmField
val Milligrams = MassUnit(Kilograms, MILLIGRAMS_PER_KILOGRAM, "milligram", "mg")

@JvmField
val MetricTons = MassUnit(Kilograms, METRIC_TONS_PER_KILOGRAM, "metric ton", "t")

@JvmField
val Pounds = MassUnit(Kilograms, POUNDS_PER_KILOGRAM, "pound", "lb")

@JvmField
val Ounces = MassUnit(Pounds, OUNCES_PER_POUND, "ounce", "oz")

// Current units
@JvmField
val Amperes = CurrentUnit(null, { it }, { it }, "ampere", "A")

@JvmField
val Milliamperes = CurrentUnit(Amperes, MILLIAMPERES_PER_AMPERE, "milliampere", "mA")

@JvmField
val Microamperes = CurrentUnit(Amperes, MICROAMPERES_PER_AMPERE, "microampere", "µA")

@JvmField
val Kiloamperes = CurrentUnit(Amperes, KILOAMPERES_PER_AMPERE, "kiloampere", "kA")

// Voltage units
@JvmField
val Volts = VoltageUnit(null, { it }, { it }, "volt", "V")

@JvmField
val Millivolts = VoltageUnit(Volts, MILLIVOLTS_PER_VOLT, "millivolt", "mV")

@JvmField
val Microvolts = VoltageUnit(Volts, MICROVOLTS_PER_VOLT, "microvolt", "µV")

@JvmField
val Kilovolts = VoltageUnit(Volts, KILOVOLTS_PER_VOLT, "kilovolt", "kV")

// Force units
@JvmField
val Newtons = ForceUnit(null, { it }, { it }, "newton", "N")

@JvmField
val Kilonewtons = ForceUnit(Newtons, KILONEWTONS_PER_NEWTON, "kilonewton", "kN")

@JvmField
val PoundsForce = ForceUnit(Newtons, POUNDS_FORCE_PER_NEWTON, "pound-force", "lbf")

@JvmField
val KilogramsForce = ForceUnit(Newtons, KILOGRAMS_FORCE_PER_NEWTON, "kilogram-force", "kgf")

// Torque units
@JvmField
val NewtonMeters = TorqueUnit(null, { it }, { it }, "newton-meter", "N·m")

@JvmField
val PoundFeet = TorqueUnit(NewtonMeters, POUND_FEET_PER_NEWTON_METER, "pound-foot", "lb·ft")

@JvmField
val NewtonCentimeters =
  TorqueUnit(NewtonMeters, NEWTON_CENTIMETERS_PER_NEWTON_METER, "newton-centimeter", "N·cm")

@JvmField
val NewtonMillimeters =
  TorqueUnit(NewtonMeters, NEWTON_MILLIMETERS_PER_NEWTON_METER, "newton-millimeter", "N·mm")

// Energy units
@JvmField
val Joules = EnergyUnit(null, { it }, { it }, "joule", "J")

@JvmField
val Kilojoules = EnergyUnit(Joules, KILOJOULES_PER_JOULE, "kilojoule", "kJ")

@JvmField
val WattHours = EnergyUnit(Joules, WATT_HOURS_PER_JOULE, "watt-hour", "Wh")

@JvmField
val KilowattHours = EnergyUnit(Joules, KILOWATT_HOURS_PER_JOULE, "kilowatt-hour", "kWh")

// Power units
@JvmField
val Watts = PowerUnit(null, { it }, { it }, "watt", "W")

@JvmField
val Milliwatts = PowerUnit(Watts, MILLIWATTS_PER_WATT, "milliwatt", "mW")

@JvmField
val Kilowatts = PowerUnit(Watts, KILOWATTS_PER_WATT, "kilowatt", "kW")

@JvmField
val Megawatts = PowerUnit(Watts, MEGAWATTS_PER_WATT, "megawatt", "MW")

@JvmField
val Horsepower = PowerUnit(Watts, HORSEPOWER_PER_WATT, "horsepower", "hp")

// Temperature units
@JvmField
val Celsius = TemperatureUnit(null, { it }, { it }, "celsius", "°C")

@JvmField
val Fahrenheit =
  TemperatureUnit(
    Celsius,
    { fahrenheit -> (fahrenheit - 32.0) * 5.0 / 9.0 },
    { celsius -> celsius * 9.0 / 5.0 + 32.0 },
    "fahrenheit",
    "°F",
  )

@JvmField
val Kelvin =
  TemperatureUnit(
    Celsius,
    { kelvin -> kelvin - 273.15 },
    { celsius -> celsius + 273.15 },
    "kelvin",
    "K",
  )

// Linear velocity units
@JvmField
val MetersPerSecond = LinearVelocityUnit(Meters, Seconds)

@JvmField
val KilometersPerHour = LinearVelocityUnit(Kilometers, Hours)

@JvmField
val MilesPerHour = LinearVelocityUnit(Miles, Hours)

@JvmField
val FeetPerSecond = LinearVelocityUnit(Feet, Seconds)

@JvmField
val InchesPerSecond = LinearVelocityUnit(Inches, Seconds)

// Linear acceleration units
@JvmField
val MetersPerSecondSquared = LinearAccelerationUnit(MetersPerSecond, Seconds)

@JvmField
val FeetPerSecondSquared = LinearAccelerationUnit(FeetPerSecond, Seconds)

@JvmField
val InchesPerSecondSquared = LinearAccelerationUnit(InchesPerSecond, Seconds)

// Angular velocity units
@JvmField
val RadiansPerSecond = AngularVelocityUnit(Radians, Seconds)

@JvmField
val DegreesPerSecond = AngularVelocityUnit(Degrees, Seconds)

@JvmField
val RotationsPerMinute = AngularVelocityUnit(Rotations, Minutes)

@JvmField
val RotationsPerSecond = AngularVelocityUnit(Rotations, Seconds)

// Angular acceleration units
@JvmField
val RadiansPerSecondSquared = AngularAccelerationUnit(RadiansPerSecond, Seconds)

@JvmField
val DegreesPerSecondSquared = AngularAccelerationUnit(DegreesPerSecond, Seconds)

@JvmField
val RotationsPerSecondSquared = AngularAccelerationUnit(RotationsPerSecond, Seconds)

// Double extension properties
inline val Double.radians get() = Radians.of(this)
inline val Double.degrees get() = Degrees.of(this)
inline val Double.rotations get() = Rotations.of(this)
inline val Double.gradians get() = Gradians.of(this)

inline val Double.seconds get() = Seconds.of(this)
inline val Double.milliseconds get() = Milliseconds.of(this)
inline val Double.microseconds get() = Microseconds.of(this)
inline val Double.nanoseconds get() = Nanoseconds.of(this)
inline val Double.minutes get() = Minutes.of(this)
inline val Double.hours get() = Hours.of(this)
inline val Double.days get() = Days.of(this)

inline val Double.meters get() = Meters.of(this)
inline val Double.millimeters get() = Millimeters.of(this)
inline val Double.centimeters get() = Centimeters.of(this)
inline val Double.kilometers get() = Kilometers.of(this)
inline val Double.inches get() = Inches.of(this)
inline val Double.feet get() = Feet.of(this)
inline val Double.yards get() = Yards.of(this)
inline val Double.miles get() = Miles.of(this)

inline val Double.kilograms get() = Kilograms.of(this)
inline val Double.grams get() = Grams.of(this)
inline val Double.milligrams get() = Milligrams.of(this)
inline val Double.metricTons get() = MetricTons.of(this)
inline val Double.pounds get() = Pounds.of(this)
inline val Double.ounces get() = Ounces.of(this)

inline val Double.amperes get() = Amperes.of(this)
inline val Double.milliamperes get() = Milliamperes.of(this)
inline val Double.microamperes get() = Microamperes.of(this)
inline val Double.kiloamperes get() = Kiloamperes.of(this)

inline val Double.volts get() = Volts.of(this)
inline val Double.millivolts get() = Millivolts.of(this)
inline val Double.microvolts get() = Microvolts.of(this)
inline val Double.kilovolts get() = Kilovolts.of(this)

inline val Double.newtons get() = Newtons.of(this)
inline val Double.kilonewtons get() = Kilonewtons.of(this)
inline val Double.poundsForce get() = PoundsForce.of(this)
inline val Double.kilogramsForce get() = KilogramsForce.of(this)

inline val Double.newtonMeters get() = NewtonMeters.of(this)
inline val Double.poundFeet get() = PoundFeet.of(this)
inline val Double.newtonCentimeters get() = NewtonCentimeters.of(this)
inline val Double.newtonMillimeters get() = NewtonMillimeters.of(this)

inline val Double.joules get() = Joules.of(this)
inline val Double.kilojoules get() = Kilojoules.of(this)
inline val Double.wattHours get() = WattHours.of(this)
inline val Double.kilowattHours get() = KilowattHours.of(this)

inline val Double.watts get() = Watts.of(this)
inline val Double.milliwatts get() = Milliwatts.of(this)
inline val Double.kilowatts get() = Kilowatts.of(this)
inline val Double.megawatts get() = Megawatts.of(this)
inline val Double.horsepower get() = Horsepower.of(this)

inline val Double.celsius get() = Celsius.of(this)
inline val Double.fahrenheit get() = Fahrenheit.of(this)
inline val Double.kelvin get() = Kelvin.of(this)

inline val Double.metersPerSecond get() = MetersPerSecond.of(this)
inline val Double.kilometersPerHour get() = KilometersPerHour.of(this)
inline val Double.milesPerHour get() = MilesPerHour.of(this)
inline val Double.feetPerSecond get() = FeetPerSecond.of(this)
inline val Double.inchesPerSecond get() = InchesPerSecond.of(this)

inline val Double.metersPerSecondSquared get() = MetersPerSecondSquared.of(this)
inline val Double.feetPerSecondSquared get() = FeetPerSecondSquared.of(this)
inline val Double.inchesPerSecondSquared get() = InchesPerSecondSquared.of(this)

inline val Double.radiansPerSecond get() = RadiansPerSecond.of(this)
inline val Double.degreesPerSecond get() = DegreesPerSecond.of(this)
inline val Double.rotationsPerMinute get() = RotationsPerMinute.of(this)
inline val Double.rotationsPerSecond get() = RotationsPerSecond.of(this)
inline val Double.rpm get() = RotationsPerMinute.of(this)

inline val Double.radiansPerSecondSquared get() = RadiansPerSecondSquared.of(this)
inline val Double.degreesPerSecondSquared get() = DegreesPerSecondSquared.of(this)
inline val Double.rotationsPerSecondSquared get() = RotationsPerSecondSquared.of(this)

val DurationUnit.timeUnit
  get() =
    when (this) {
      DurationUnit.NANOSECONDS -> Nanoseconds
      DurationUnit.MICROSECONDS -> Microseconds
      DurationUnit.MILLISECONDS -> Milliseconds
      DurationUnit.SECONDS -> Seconds
      DurationUnit.MINUTES -> Minutes
      DurationUnit.HOURS -> Hours
      DurationUnit.DAYS -> Days
    }

val TimeUnit.durationUnit
  get() =
    when (this) {
      Seconds -> DurationUnit.SECONDS
      Milliseconds -> DurationUnit.MILLISECONDS
      Microseconds -> DurationUnit.MICROSECONDS
      Nanoseconds -> DurationUnit.NANOSECONDS
      Minutes -> DurationUnit.MINUTES
      Hours -> DurationUnit.HOURS
      Days -> DurationUnit.DAYS
      else -> null
    }
