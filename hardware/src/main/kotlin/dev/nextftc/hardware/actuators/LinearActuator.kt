package dev.nextftc.hardware.actuators

import dev.nextftc.units.Inches
import dev.nextftc.units.Rotations
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.DistanceUnit

/**
 * Wraps a `NextMotor` and a distance-per-rotation, to work on linear units
 *
 * @param motor wrapped NextMotor
 * @param distPerRotation linear distance traveled per one motor rotation
 */
class LinearActuator(val motor: NextMotor, val distPerRotation: Per<DistanceUnit, AngleUnit>) {
  fun setPosition(distance: Distance) {
    // convert distance to rotations
    val rotations = distance.into(Inches) / distPerRotation.magnitude
    val angle: Angle = Rotations.of(rotations)
    motor.setPositionSetpoint(angle)
  }

  fun setPositionInches(inches: Double) = setPosition(Inches.of(inches))

  fun setPositionTicks(ticks: Int) {
    val angle = (motor.anglePerCount * ticks) as Angle
    motor.setPositionSetpoint(angle)
  }

  fun setThrottle(throttle: Double) {
    motor.throttle = throttle
  }

  fun setThrottleContinuous(throttle: Double) {
    motor.throttle = throttle
  }
}
