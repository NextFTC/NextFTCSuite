/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors

import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DigitalChannel
import com.qualcomm.robotcore.hardware.DigitalChannelImpl
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.lynx.NextLynxModule
import dev.nextftc.hardware.util.LazyHardware

/**
 * Lightweight wrapper around a [DigitalChannel] for reading digital sensors
 * like limit switches, magnetic switches, and beam breaks.
 *
 * Most digital sensors are "active low" — they read `false` when triggered
 * (switch pressed, magnet present, beam broken) and `true` when idle. This
 * wrapper handles that inversion via [inverted] so [isTriggered] always
 * means what you'd expect.
 *
 * Example:
 * ```
 * val beamBreak = NextDigitalSensor("beamBreak")
 * if (beamBreak.isTriggered) { stopMotor() }
 * ```
 *
 * @param initializer Lazily resolves the backing [DigitalChannel].
 * @param inverted If true, [isTriggered] returns the opposite of the raw
 * sensor state — i.e. triggered when the channel reads low. For example, a
 * touch sensor reads `false` while it's being pressed, so inverting makes
 * [isTriggered] read `true` when pressed, which is what you'd expect.
 * Defaults to true, matching most FTC digital sensors, which are active-low.
 *
 * @author 28shettr
 */

class NextDigitalSensor @JvmOverloads constructor(
  initializer: () -> DigitalChannel,
  private val inverted: Boolean = true,
) {
  /**
   * @param module The Lynx Module, see [RobotController.controlHub], [RobotController.expansionHub],
   * @param port The digital sensor port (in the range [0, 7])
   * @param name Hardware map name to resolve the [DigitalChannel] from.
   * @param inverted If true, [isTriggered] is the opposite of the raw state. Defaults to true.
   */
  @JvmOverloads
  constructor(
    module: NextLynxModule,
    port: Int,
    inverted: Boolean = true,
  ) : this(
    { DigitalChannelImpl(module.digitalController, port) },
    inverted,
  ) {
    require(port in 0..7) { "Expected port in range [0, 7], got $port" }
  }

  @JvmOverloads
  constructor(
    name: String,
    inverted: Boolean = true,
  ) : this(
    { RobotController.hardwareMap[name] as DigitalChannel },
    inverted,
  )

  private val sensor by LazyHardware(initializer).also {
    it.applyAfterInit { channel -> channel.mode = DigitalChannel.Mode.INPUT }
  }

  /** Raw state of the digital channel */
  val rawState: Boolean
    get() = sensor.state

  /** True if the sensor is currently triggered (accounting for [inverted]). */
  val isTriggered: Boolean
    get() = if (inverted) {
      !sensor.state
    } else {
      sensor.state
    }

  /** Returns a string of the sensor's current state for telemetry or logging. */
  fun debug(): String = "Sensor State: $isTriggered, Raw State: $rawState, Inverted: $inverted"
}
