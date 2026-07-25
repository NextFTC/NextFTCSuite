package dev.nextftc.hardware.sensors

import com.qualcomm.robotcore.hardware.AnalogInput
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.lynx.NextLynxModule
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.units.Volts
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.volts

class NextAnalogInput @JvmOverloads constructor(
  initializer: () -> AnalogInput,
  val customTransformation: (Double) -> Double = { n: Double -> n },
  val maxVoltage: Voltage = 3.3.volts,
) {
  @JvmOverloads constructor(
    name: String,
    customTransformation: (Double) -> Double = { n: Double -> n },
    maxVoltage: Voltage = 3.3.volts,
  ) : this(
    { RobotController.hardwareMap[name] as AnalogInput },
    customTransformation,
    maxVoltage,
  )

  @JvmOverloads constructor(
    module: NextLynxModule,
    channel: Int,
    customTransformation: (Double) -> Double = { n: Double -> n },
    maxVoltage: Voltage = 3.3.volts,
  ) : this(
      { AnalogInput(module.analogController, channel) },
      customTransformation,
      maxVoltage
  ) {
    require(channel in 0..3) { "Expected channel in range [0, 3], got $channel" }
  }

  private val input by LazyHardware(initializer)

  val rawVoltage: Voltage
    get() = input.voltage.volts

  val value: Double
    get() = customTransformation.invoke(rawVoltage.into(Volts) / maxVoltage.into(Volts))
}
