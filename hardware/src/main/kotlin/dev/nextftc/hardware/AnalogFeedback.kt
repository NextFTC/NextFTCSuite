/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

import com.qualcomm.robotcore.hardware.AnalogInput
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class AnalogFeedback(private val voltageSupplier: Supplier<Double>) : ReadOnlyProperty<Any?, Double> {

  override fun getValue(thisRef: Any?, property: KProperty<*>): Double =
    voltageSupplier.get() / 3.3 * 2 * PI
}
