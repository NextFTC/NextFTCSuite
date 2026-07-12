package dev.nextftc.hardware.util

import dev.nextftc.units.Volts
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.volts
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalogFeedback @JvmOverloads constructor(
  val maxVoltage: Voltage = 3.3.volts,
  private val voltageSupplier: Supplier<Double>,
) : ReadOnlyProperty<Any?, Double> {

  override fun getValue(thisRef: Any?, property: KProperty<*>): Double =
    voltageSupplier.get() / maxVoltage.into(Volts) * 2 * PI
}
