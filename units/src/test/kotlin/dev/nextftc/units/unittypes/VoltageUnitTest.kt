/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Kilovolts
import dev.nextftc.units.Microvolts
import dev.nextftc.units.Millivolts
import dev.nextftc.units.Volts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

@Suppress("ktlint:standard:property-naming")
class VoltageUnitTest :
  FunSpec({
    context("VoltageUnit constants") {
      test("Volts should be the base unit") {
        Volts.baseUnit shouldBeSameInstanceAs Volts
      }

      test("all voltage units should have Volts as base unit") {
        Millivolts.baseUnit shouldBeSameInstanceAs Volts
        Microvolts.baseUnit shouldBeSameInstanceAs Volts
        Kilovolts.baseUnit shouldBeSameInstanceAs Volts
      }
    }

    context("VoltageUnit.of() factory method") {
      test("should create Voltage measurements with correct magnitude") {
        val voltage = Volts.of(10.0)
        voltage.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val mV = Millivolts.of(1000.0)
        val V = Volts.of(1.0)

        mV.baseUnitMagnitude shouldBe (V.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val voltage = Volts.of(2.5)
        voltage.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        voltage.baseUnitMagnitude shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("VoltageUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val voltage = Millivolts.ofBaseUnits(1.0) // 1 V
        voltage.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should work for microvolts") {
        val voltage = Microvolts.ofBaseUnits(1.0) // 1 V
        voltage.magnitude shouldBe (1_000_000.0 plusOrMinus EPSILON)
      }

      test("should work for kilovolts") {
        val voltage = Kilovolts.ofBaseUnits(1000.0) // 1000 V
        voltage.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("VoltageUnit conversions") {
      test("should convert millivolts to volts correctly") {
        val voltage = Millivolts.of(5000.0)
        voltage.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert microvolts to volts correctly") {
        val voltage = Microvolts.of(1_000_000.0)
        voltage.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert kilovolts to volts correctly") {
        val voltage = Kilovolts.of(2.0)
        voltage.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val original = Volts.of(123.456)
        val inMillivolts = original.into(Millivolts)
        val backToVolts = Millivolts.of(inMillivolts).into(Volts)

        backToVolts shouldBe (123.456 plusOrMinus EPSILON)
      }
    }

    context("VoltageUnit utility methods") {
      test("isBaseUnit should return true for Volts") {
        Volts.isBaseUnit() shouldBe true
      }

      test("isBaseUnit should return false for derived units") {
        Millivolts.isBaseUnit() shouldBe false
        Microvolts.isBaseUnit() shouldBe false
        Kilovolts.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Volts.name() shouldBe "volt"
        Millivolts.name() shouldBe "millivolt"
        Microvolts.name() shouldBe "microvolt"
        Kilovolts.name() shouldBe "kilovolt"
      }

      test("symbol() should return correct unit symbol") {
        Volts.symbol() shouldBe "V"
        Millivolts.symbol() shouldBe "mV"
        Microvolts.symbol() shouldBe "µV"
        Kilovolts.symbol() shouldBe "kV"
      }
    }
  })
