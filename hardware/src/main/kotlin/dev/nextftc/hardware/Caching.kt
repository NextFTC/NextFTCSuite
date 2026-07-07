/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

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
      cachedValue = value
      whenSet(value)
    } else {
      whenSet(null)
    }
  }
}
