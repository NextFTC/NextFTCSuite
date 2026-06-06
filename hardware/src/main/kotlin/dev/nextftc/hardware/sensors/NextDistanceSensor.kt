/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import com.qualcomm.robotcore.hardware.DistanceSensor
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
/**
 * Lightweight wrapper for a distance sensor that caches the last reading.
 * Call [update] in periodic to read the hardware.
 *
 * Use [isWithinDistance] to check
 * if an object is close enough.
 *
 * Example:
 * ```
 * override fun periodic() {
 *     sensor.update()
 *     if (sensor.isWithinDistance(6.7)) { ... }
 * }
 * ```
 *
 * @param initializer Lazily resolves the backing [DistanceSensor].
 *
 * @author 28shettr
 */
class NextDistanceSensor(initializer: () -> DistanceSensor) {
  constructor(name: String) : this(
    { RobotController.hardwareMap[name] as DistanceSensor },
  )

  private val distanceSensor by LazyHardware(initializer)

  private var cachedDistanceCm: Double = Double.NaN

  /** Reads the distance sensor and refreshes the cache. Call this once per loop, before reading any properties. */
  fun update() {
    cachedDistanceCm = distanceSensor.getDistance(DistanceUnit.CM)
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
}
