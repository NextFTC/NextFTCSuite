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
import com.qualcomm.robotcore.hardware.NormalizedRGBA
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit


/**
 * Combines a color sensor and an optional distance sensor into one class.
 * Call [update] each loop to read the hardware. Use [isColor] to check
 * against a [ColorProfile].
 *
 * Example:
 * ```
 * val sensor = NextColorDistanceSensor("colorSensor", hasDistance = true)
 * val green  = ColorProfile(hueMin = 150f, hueMax = 185f, saturationMin = 0.2f)
 *
 * override fun periodic() {
 *     sensor.update()
 *     if (sensor.isWithinDistance(4.0) && sensor.isColor(green)) { ... }
 * }
 * ```
 *
 * Use [debug] in telemetry to calibrate [ColorProfile]s.
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
        } else null,
    )

    private val colorSensor by LazyHardware(colorInitializer)
    private val distanceSensor: DistanceSensor? by lazy { distanceInitializer?.invoke() }

    private var cachedColors: NormalizedRGBA? = null
    private var cachedDistanceCm: Double = Double.NaN
    private val cachedHsv: FloatArray = FloatArray(3)


    /** Last cached normalized RGBA reading, or null if [update] has not been called. */
    val rgba: NormalizedRGBA?
        get() = cachedColors

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
        val colors = colorSensor.normalizedColors
        cachedColors = colors

        val r = (colors.red * 255).toInt()
        val g = (colors.green * 255).toInt()
        val b = (colors.blue * 255).toInt()

        Color.RGBToHSV(r, g, b, cachedHsv)

        cachedDistanceCm = distanceSensor?.getDistance(DistanceUnit.CM) ?: Double.NaN
    }

    /** Returns the last cached distance converted to the requested [unit]. */
    fun distance(unit: DistanceUnit = DistanceUnit.CM): Double {
        return when (unit) {
            DistanceUnit.CM -> cachedDistanceCm
            DistanceUnit.MM -> cachedDistanceCm * 10
            DistanceUnit.INCH -> cachedDistanceCm / 2.54
            DistanceUnit.METER -> cachedDistanceCm / 100
        }
    }

    /** True if a distance sensor is attached and an object is within [threshold] centimeters. */
    fun isWithinDistance(threshold: Double, unit: DistanceUnit = DistanceUnit.CM): Boolean {
        val distance = distance(unit)
        return !distance.isNaN() && distance <= threshold
    }

    /** True if the cached HSV reading falls inside [range]. */
    fun isColor(range: ColorProfile): Boolean {
        return range.contains(cachedHsv)
    }

    /** Single-line telemetry string showing current HSV and distance. Useful for calibrating [ColorProfile]s. */
    fun debug(): String {
        val rgba = cachedColors

        val r = "%.2f".format(rgba?.red ?: 0f)
        val g = "%.2f".format(rgba?.green ?: 0f)
        val b = "%.2f".format(rgba?.blue ?: 0f)

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