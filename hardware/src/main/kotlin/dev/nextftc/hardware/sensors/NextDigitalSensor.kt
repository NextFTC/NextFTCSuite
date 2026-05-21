/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import com.qualcomm.robotcore.hardware.DigitalChannel
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * Lightweight wrapper around a [DigitalChannel] for reading digital sensors
 * like limit switches, magnetic switches, and beam breaks.
 *
 * Most digital sensors are "active low" — they read `false` when triggered
 * (switch pressed, magnet present, beam broken) and `true` when idle. This
 * wrapper handles that inversion via [triggeredOnLow] so [isTriggered] always
 * means what you'd expect.
 *
 * Example:
 * ```
 * val beamBreak = NextDigitalSensor("beamBreak")
 * if (beamBreak.isTriggered) { stopMotor() }
 *```
 *
 * @param initializer Lazily resolves the backing [DigitalChannel].
 * @param triggeredOnLow If true, [isTriggered] returns the inverse of the raw
 * sensor state. Defaults to true (matches most FTC digital sensors).
 *
 * @author 28shettr
 */
class NextDigitalSensor(initializer: () -> DigitalChannel, private val triggeredOnLow: Boolean = true) {
  @JvmOverloads
  constructor(name: String, activeLow: Boolean = true) : this(
    {
      RobotController.hardwareMap[name] as DigitalChannel
    },
    activeLow,
  )

  private val sensor by LazyHardware(initializer).also {
    it.applyAfterInit { channel -> channel.mode = DigitalChannel.Mode.INPUT }
  }

  /** Raw state of the digital channel*/
  val rawState: Boolean
    get() = sensor.state

  /** True if the sensor is currently triggered (accounting for [triggeredOnLow]). */
  val isTriggered: Boolean
    get() = if (triggeredOnLow) {
      !sensor.state
    } else {
      sensor.state
    }
}
