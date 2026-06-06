/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import android.graphics.Color
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.sensors.colors.ColorProfile
import dev.nextftc.hardware.sensors.colors.NextColor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

/**
 * Combines a color sensor and an optional distance sensor into one class.
 * Call [update] each loop to read the hardware. Use [isColor] to check
 * against a [dev.nextftc.hardware.sensors.colors.ColorProfile].
 *
 * Example:
 * ```
 * val green = ColorProfile(
 *     space = ColorSpace.HSV,
 *     color = NextColor.HSV(160f, 0.8f, 0.7f),
 *     tolerance = NextColor.HSV(15f, 0.3f, 1f),
 * )
 *
 * override fun periodic() {
 *     sensor.update()
 *     if (sensor.isWithinDistance(4.0) && sensor.isColor(green)) { ... }
 * }
 * ```
 *
 * Use [debug] in telemetry to calibrate [dev.nextftc.hardware.sensors.colors.ColorProfile]s.
 *
 * @param colorInitializer Lazily resolves the backing [NormalizedColorSensor].
 * @param distanceInitializer Optional lazy distance sensor.
 *
 * @author 28shettr
 */
class NextColorDistanceSensor(
  colorInitializer: () -> NormalizedColorSensor,
  distanceInitializer: (() -> DistanceSensor)? = null,
) {
  @JvmOverloads
  constructor(sensorName: String, hasDistance: Boolean = false) : this(
    { RobotController.hardwareMap[sensorName] as NormalizedColorSensor },
    if (hasDistance) {
      { RobotController.hardwareMap[sensorName] as DistanceSensor }
    } else {
      null
    },
  )

  private val colorSensor by LazyHardware(colorInitializer)
  private val distanceSensor: DistanceSensor? by lazy { distanceInitializer?.invoke() }

  private var cachedDistanceCm: Double = Double.NaN

  private var cachedColor: NextColor = NextColor.rgb(0f, 0f, 0f)
  private var cachedHsv: FloatArray = FloatArray(3)

  /** Last cached reading as a [NextColor]. Black until [update] is called. */
  val color: NextColor
    get() = cachedColor

  /** Last cached hue in degrees (0..360). */
  val hue: Float get() = cachedHsv[0]

  /** Last cached saturation (0..1). */
  val saturation: Float get() = cachedHsv[1]

  /** Last cached value/brightness (0..1). */
  val value: Float get() = cachedHsv[2]

  /** Gain applied to the color sensor. Higher values amplify readings for better detection at distance or in low light. Typical range is 1..4. */
  var gain: Float
    get() = colorSensor.gain
    set(gain) {
      colorSensor.gain = gain
    }

  /** Reads the color sensor (and distance sensor, if present) and refreshes the cache. Call this once per loop, before reading any properties. */
  fun update() {
    val c = colorSensor.normalizedColors
    cachedColor = NextColor.rgb(c.red * 255, c.green * 255, c.blue * 255)

    Color.RGBToHSV(
      cachedColor.red.toInt(),
      cachedColor.green.toInt(),
      cachedColor.blue.toInt(),
      cachedHsv,
    )

    cachedDistanceCm = distanceSensor?.getDistance(DistanceUnit.CM) ?: Double.NaN
  }

  /** Returns the last cached distance converted to the requested [unit]. */
  @JvmOverloads
  fun getDistance(unit: DistanceUnit = DistanceUnit.CM): Double =
    unit.fromUnit(DistanceUnit.CM, cachedDistanceCm)

  /** True if a distance sensor senses an object within [threshold] in the given [unit]. */
  @JvmOverloads
  fun isWithinDistance(threshold: Double, unit: DistanceUnit = DistanceUnit.CM): Boolean {
    val distance = getDistance(unit)
    return !distance.isNaN() && distance <= threshold
  }

  /** True if the cached color reading matches [profile]. */
  fun isColor(profile: ColorProfile): Boolean = profile.matches(cachedColor)

  /** True if the cached color reading matches [profile] and an object is within [threshold] in the given [unit]. */
  @JvmOverloads
  fun isColorWithinDistance(
    profile: ColorProfile,
    threshold: Double,
    unit: DistanceUnit = DistanceUnit.CM,
  ): Boolean = isWithinDistance(threshold, unit) && isColor(profile)

  /** Single-line telemetry string showing current HSV and distance. Useful for calibrating [ColorProfile]s. */
  fun debug(): String {
    val r = "%.0f".format(cachedColor.red)
    val g = "%.0f".format(cachedColor.green)
    val b = "%.0f".format(cachedColor.blue)

    val h = "%.1f".format(hue)
    val s = "%.2f".format(saturation)
    val v = "%.2f".format(value)

    val d = if (cachedDistanceCm.isNaN()) {
      "n/a"
    } else {
      "%.2f".format(cachedDistanceCm)
    }

    return "RGB=($r,$g,$b) HSV=($h,$s,$v) Dist=$d"
  }
}
