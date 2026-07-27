package dev.nextftc.hardware.sensors

import com.qualcomm.robotcore.hardware.AnalogInput
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.lynx.NextLynxModule
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.units.Volts
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.volts

/**
 * A wrapper around an [AnalogInput] that provides convenient access to raw voltage
 * readings as well as a normalized, optionally transformed value.
 *
 * The hardware device is resolved lazily on first access via [LazyHardware], so
 * constructing an instance of this class does not require the hardware map to be
 * ready yet.
 *
 * @param initializer A lambda that resolves and returns the underlying [AnalogInput].
 * @param customTransformation An optional transformation applied to the normalized
 * voltage ratio (raw voltage divided by [maxVoltage]) before it is exposed via [value].
 * Defaults to the identity function.
 * @param maxVoltage The voltage that corresponds to a normalized value of `1.0`.
 * Defaults to `3.3` volts.
 */
class NextAnalogInput @JvmOverloads constructor(
  initializer: () -> AnalogInput,
  val customTransformation: (Double) -> Double = { n: Double -> n },
  val maxVoltage: Voltage = 3.3.volts,
) {
  /**
   * Creates a [NextAnalogInput] by looking up the [AnalogInput] in the hardware map
   * by its configured name.
   *
   * @param name The name of the analog input device as configured in the hardware map.
   * @param customTransformation An optional transformation applied to the normalized
   * voltage ratio before it is exposed via [value]. Defaults to the identity function.
   * @param maxVoltage The voltage that corresponds to a normalized value of `1.0`.
   * Defaults to `3.3` volts.
   */
  @JvmOverloads constructor(
    name: String,
    customTransformation: (Double) -> Double = { n: Double -> n },
    maxVoltage: Voltage = 3.3.volts,
  ) : this(
    { RobotController.hardwareMap[name] as AnalogInput },
    customTransformation,
    maxVoltage,
  )

  /**
   * Creates a [NextAnalogInput] directly from a [NextLynxModule] and analog channel
   * number, bypassing the hardware map.
   *
   * @param module The Lynx module whose analog controller the input is attached to.
   * @param channel The analog channel index, must be in the range `[0, 3]`.
   * @param customTransformation An optional transformation applied to the normalized
   * voltage ratio before it is exposed via [value]. Defaults to the identity function.
   * @param maxVoltage The voltage that corresponds to a normalized value of `1.0`.
   * Defaults to `3.3` volts.
   * @throws IllegalArgumentException if [channel] is not in the range `[0, 3]`.
   */
  @JvmOverloads constructor(
    module: NextLynxModule,
    channel: Int,
    customTransformation: (Double) -> Double = { n: Double -> n },
    maxVoltage: Voltage = 3.3.volts,
  ) : this(
    { AnalogInput(module.analogController, channel) },
    customTransformation,
    maxVoltage,
  ) {
    require(channel in 0..3) { "Expected channel in range [0, 3], got $channel" }
  }

  /** The lazily-initialized underlying [AnalogInput] hardware device. */
  private val input by LazyHardware(initializer)

  /** The raw, untransformed voltage currently read from the analog input. */
  val rawVoltage: Voltage
    get() = input.voltage.volts

  /**
   * The normalized reading from the analog input, computed as [rawVoltage] divided
   * by [maxVoltage] and passed through [customTransformation].
   */
  val value: Double
    get() = customTransformation.invoke(rawVoltage.into(Volts) / maxVoltage.into(Volts))
}
