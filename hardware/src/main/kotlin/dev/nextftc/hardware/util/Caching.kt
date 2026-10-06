package dev.nextftc.hardware.util

import kotlin.math.abs
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Caching(private val cacheTolerance: Double, private val whenSet: (Double?) -> Unit) :
  ReadWriteProperty<Any?, Double> {

  private var cachedValue = Double.NaN

  override fun getValue(thisRef: Any?, property: KProperty<*>): Double =
    if (cachedValue.isNaN()) 0.0 else cachedValue

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: Double) {
    if (cachedValue.isNaN() || abs(cachedValue - value) > cacheTolerance) {
      // written first: the write may initialize the hardware, which resets this cache
      whenSet(value)
      cachedValue = value
    } else {
      whenSet(null)
    }
  }

  /**
   * Forgets the cached value so the next set is always written to the hardware. Call this
   * whenever the underlying hardware object is re-created.
   */
  fun reset() {
    cachedValue = Double.NaN
  }
}
