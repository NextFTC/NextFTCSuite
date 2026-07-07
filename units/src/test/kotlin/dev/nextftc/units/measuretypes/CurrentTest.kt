/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Amperes
import dev.nextftc.units.Kiloamperes
import dev.nextftc.units.Microamperes
import dev.nextftc.units.Milliamperes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class CurrentTest :
  FunSpec({
    context("Current creation") {
      test("should create Current with correct magnitude") {
        val current = Amperes.of(10.0)
        current.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Current with correct unit") {
        val current = Milliamperes.of(500.0)
        current.unit shouldBe Milliamperes
      }

      test("should create Current with correct base unit magnitude") {
        val current = Milliamperes.of(2000.0)
        current.baseUnitMagnitude shouldBe (2.0 plusOrMinus EPSILON)
      }
    }

    context("Current.into() conversions") {
      test("should convert amperes to milliamperes") {
        val current = Amperes.of(2.0)
        current.into(Milliamperes) shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should convert milliamperes to amperes") {
        val current = Milliamperes.of(5000.0)
        current.into(Amperes) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert amperes to microamperes") {
        val current = Amperes.of(0.5)
        current.into(Microamperes) shouldBe (500000.0 plusOrMinus EPSILON)
      }

      test("should convert kiloamperes to amperes") {
        val current = Kiloamperes.of(2.5)
        current.into(Amperes) shouldBe (2500.0 plusOrMinus EPSILON)
      }

      test("should convert microamperes to milliamperes") {
        val current = Microamperes.of(1000.0)
        current.into(Milliamperes) shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("Current.plus() addition") {
      test("should add currents with same unit") {
        val c1 = Amperes.of(10.0)
        val c2 = Amperes.of(5.0)
        val result = c1 + c2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should add currents with different units") {
        val c1 = Amperes.of(1.0)
        val c2 = Milliamperes.of(500.0)
        val result = c1 + c2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should add milliamperes to microamperes") {
        val c1 = Milliamperes.of(1.0)
        val c2 = Microamperes.of(500.0)
        val result = c1 + c2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Milliamperes
      }
    }

    context("Current.minus() subtraction") {
      test("should subtract currents with same unit") {
        val c1 = Amperes.of(10.0)
        val c2 = Amperes.of(3.0)
        val result = c1 - c2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should subtract currents with different units") {
        val c1 = Amperes.of(2.0)
        val c2 = Milliamperes.of(500.0)
        val result = c1 - c2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should handle negative results") {
        val c1 = Milliamperes.of(100.0)
        val c2 = Milliamperes.of(200.0)
        val result = c1 - c2

        result.magnitude shouldBe (-100.0 plusOrMinus EPSILON)
      }
    }

    context("Current.times() scalar multiplication") {
      test("should multiply current by positive scalar") {
        val current = Amperes.of(10.0)
        val result = current * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should multiply current by fractional scalar") {
        val current = Milliamperes.of(100.0)
        val result = current * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Current.div() scalar division") {
      test("should divide current by positive scalar") {
        val current = Amperes.of(20.0)
        val result = current / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should divide current by fractional scalar") {
        val current = Milliamperes.of(10.0)
        val result = current / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("Current.unaryMinus() negation") {
      test("should negate positive current") {
        val current = Amperes.of(10.0)
        val result = -current

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Amperes
      }

      test("should negate negative current") {
        val current = Milliamperes.of(-500.0)
        val result = -current

        result.magnitude shouldBe (500.0 plusOrMinus EPSILON)
      }
    }

    context("Current edge cases") {
      test("should handle very small currents") {
        val current = Microamperes.of(0.001)
        current.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large currents") {
        val current = Kiloamperes.of(1000.0)
        current.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val current = Amperes.of(123.456)
        val inMa = current.into(Milliamperes)
        val backToA = Milliamperes.of(inMa).into(Amperes)

        backToA shouldBe (123.456 plusOrMinus EPSILON)
      }
    }
  })
