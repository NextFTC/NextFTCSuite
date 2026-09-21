package dev.nextftc.hardware.util

import kotlin.math.abs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Caching(private val cacheTolerance: Double, private val whenSet: (Double?) -> Unit) :
  ReadWriteProperty<Any?, Double> {

  private var cachedValue = Double.NaN

  override fun getValue(thisRef: Any?, property: KProperty<*>): Double =
    if (cachedValue.isNaN()) 0.0 else cachedValue

  fun invalidate() { cachedValue = Double.NaN }

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
    if (cachedValue.isNaN() || abs(cachedValue - value) > cacheTolerance) {
      cachedValue = value
      whenSet(value)
    } else {
      whenSet(null)
    }
  }
}
