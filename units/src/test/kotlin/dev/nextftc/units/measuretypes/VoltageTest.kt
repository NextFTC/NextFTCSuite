/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Kilovolts
import dev.nextftc.units.Microvolts
import dev.nextftc.units.Millivolts
import dev.nextftc.units.Volts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

@Suppress("ktlint:standard:property-naming")
class VoltageTest :
  FunSpec({
    context("Voltage creation") {
      test("should create Voltage with correct magnitude") {
        val voltage = Volts.of(12.0)
        voltage.magnitude shouldBe 12.0 plusOrMinus EPSILON
      }

      test("should create Voltage with correct unit") {
        val voltage = Millivolts.of(3300.0)
        voltage.unit shouldBe Millivolts
      }

      test("should create Voltage with correct base unit magnitude") {
        val voltage = Kilovolts.of(2.0)
        voltage.baseUnitMagnitude shouldBe 2000.0 plusOrMinus EPSILON
      }
    }

    context("Voltage.into() conversions") {
      test("should convert volts to millivolts") {
        val voltage = Volts.of(5.0)
        voltage.into(Millivolts) shouldBe 5000.0 plusOrMinus EPSILON
      }

      test("should convert millivolts to volts") {
        val voltage = Millivolts.of(3300.0)
        voltage.into(Volts) shouldBe (3.3 plusOrMinus EPSILON)
      }

      test("should convert microvolts to millivolts") {
        val voltage = Microvolts.of(5000.0)
        voltage.into(Millivolts) shouldBe 5.0 plusOrMinus EPSILON
      }

      test("should convert kilovolts to volts") {
        val voltage = Kilovolts.of(1.5)
        voltage.into(Volts) shouldBe 1500.0 plusOrMinus EPSILON
      }

      test("should convert volts to kilovolts") {
        val voltage = Volts.of(10000.0)
        voltage.into(Kilovolts) shouldBe 10.0 plusOrMinus EPSILON
      }
    }

    context("Voltage.plus() addition") {
      test("should add voltages with same unit") {
        val v1 = Volts.of(5.0)
        val v2 = Volts.of(3.0)
        val result = v1 + v2

        result.magnitude shouldBe 8.0 plusOrMinus EPSILON
        result.unit shouldBe Volts
      }

      test("should add voltages with different units") {
        val v1 = Volts.of(5.0)
        val v2 = Millivolts.of(500.0)
        val result = v1 + v2

        result.magnitude shouldBe 5.5 plusOrMinus EPSILON
        result.unit shouldBe Volts
      }

      test("should add microvolts to millivolts") {
        val v1 = Millivolts.of(1.0)
        val v2 = Microvolts.of(1000.0)
        val result = v1 + v2

        result.magnitude shouldBe 2.0 plusOrMinus EPSILON
        result.unit shouldBe Millivolts
      }
    }

    context("Voltage.minus() subtraction") {
      test("should subtract voltages with same unit") {
        val v1 = Volts.of(10.0)
        val v2 = Volts.of(3.0)
        val result = v1 - v2

        result.magnitude shouldBe 7.0 plusOrMinus EPSILON
        result.unit shouldBe Volts
      }

      test("should subtract voltages with different units") {
        val v1 = Volts.of(5.0)
        val v2 = Millivolts.of(500.0)
        val result = v1 - v2

        result.magnitude shouldBe 4.5 plusOrMinus EPSILON
        result.unit shouldBe Volts
      }

      test("should handle negative results") {
        val v1 = Volts.of(3.0)
        val v2 = Volts.of(5.0)
        val result = v1 - v2

        result.magnitude shouldBe -2.0 plusOrMinus EPSILON
      }
    }

    context("Voltage.times() scalar multiplication") {
      test("should multiply voltage by positive scalar") {
        val voltage = Volts.of(5.0)
        val result = voltage * 2.0

        result.magnitude shouldBe 10.0 plusOrMinus EPSILON
        result.unit shouldBe Volts
      }

      test("should multiply voltage by fractional scalar") {
        val voltage = Millivolts.of(1000.0)
        val result = voltage * 0.5

        result.magnitude shouldBe 500.0 plusOrMinus EPSILON
      }
    }

    context("Voltage.div() scalar division") {
      test("should divide voltage by positive scalar") {
        val voltage = Volts.of(12.0)
        val result = voltage / 3.0

        result.magnitude shouldBe (4.0 plusOrMinus EPSILON)
        result.unit shouldBe Volts
      }

      test("should divide voltage by fractional scalar") {
        val voltage = Kilovolts.of(10.0)
        val result = voltage / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("Voltage.unaryMinus() negation") {
      test("should negate positive voltage") {
        val voltage = Volts.of(5.0)
        val result = -voltage

        result.magnitude shouldBe (-5.0 plusOrMinus EPSILON)
        result.unit shouldBe Volts
      }

      test("should negate negative voltage") {
        val voltage = Volts.of(-12.0)
        val result = -voltage

        result.magnitude shouldBe (12.0 plusOrMinus EPSILON)
      }
    }

    context("Voltage edge cases") {
      test("should handle very small voltages") {
        val voltage = Microvolts.of(1.0)
        voltage.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should handle very large voltages") {
        val voltage = Kilovolts.of(1000.0)
        voltage.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val voltage = Volts.of(3.3)
        val inMv = voltage.into(Millivolts)
        val backToV = Millivolts.of(inMv).into(Volts)

        backToV shouldBe (3.3 plusOrMinus EPSILON)
      }
    }

    context("Common voltage values") {
      test("should represent common electronic voltages") {
        val v3_3 = Volts.of(3.3)
        val v5 = Volts.of(5.0)
        val v12 = Volts.of(12.0)

        v3_3.magnitude shouldBe (3.3 plusOrMinus EPSILON)
        v5.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        v12.magnitude shouldBe (12.0 plusOrMinus EPSILON)
      }
    }
  })
