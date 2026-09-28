package dev.nextftc.hardware.actuators

import dev.nextftc.units.Inches
import dev.nextftc.units.Rotations
import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.DistanceUnit
import kotlin.math.roundToInt

/**
 * Wraps a `NextMotor` and a distance-per-rotation, to work on linear units
 *
 * @param motor wrapped NextMotor
 * @param distPerRotation linear distance traveled per one motor rotation
 */
class LinearActuator(val motor: NextMotor, val distPerRotation: Per<DistanceUnit, AngleUnit>) {
  private val distancePerRotation: Distance
    get() = distPerRotation.unit.numerator.of(distPerRotation.magnitude)

  var position: Distance
    get() {
      val rotations = motor.encoderPosition.into(distPerRotation.unit.denominator)
      return distancePerRotation * rotations
    }
    set(value) {
      val rotations = value.into(distancePerRotation.unit) / distancePerRotation.magnitude
      val angle = distPerRotation.unit.denominator.of(rotations)
      motor.setPositionSetpoint(angle)
    }

  var positionInches: Double
    get() = position.into(Inches)
    set(value) {
      position = Inches.of(value)
    }

  var positionTicks: Int
    get() = ((motor.encoderPosition / motor.anglePerCount).magnitude).roundToInt()
    set(value) {
      val angle = (motor.anglePerCount * value.toDouble()) as Angle
      motor.setPositionSetpoint(angle)
    }

  var throttle: Double
    get() = motor.throttle
    set(value) {
      motor.throttle = value
    }

  var throttleContinuous: Double
    get() = throttle
    set(value) {
      throttle = value
    }
}
