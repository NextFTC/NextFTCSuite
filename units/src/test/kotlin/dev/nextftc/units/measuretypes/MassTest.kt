/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Grams
import dev.nextftc.units.Kilograms
import dev.nextftc.units.MetricTons
import dev.nextftc.units.Milligrams
import dev.nextftc.units.Ounces
import dev.nextftc.units.Pounds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class MassTest :
  FunSpec({
    context("Mass creation") {
      test("should create Mass with correct magnitude") {
        val mass = Kilograms.of(10.0)
        mass.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Mass with correct unit") {
        val mass = Grams.of(500.0)
        mass.unit shouldBe Grams
      }

      test("should create Mass with correct base unit magnitude") {
        val mass = Grams.of(2000.0)
        mass.baseUnitMagnitude shouldBe (2.0 plusOrMinus EPSILON)
      }
    }

    context("Mass.into() conversions") {
      test("should convert kilograms to grams") {
        val mass = Kilograms.of(2.0)
        mass.into(Grams) shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should convert grams to kilograms") {
        val mass = Grams.of(5000.0)
        mass.into(Kilograms) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert kilograms to milligrams") {
        val mass = Kilograms.of(0.5)
        mass.into(Milligrams) shouldBe (500000.0 plusOrMinus EPSILON)
      }

      test("should convert metric tons to kilograms") {
        val mass = MetricTons.of(2.5)
        mass.into(Kilograms) shouldBe (2500.0 plusOrMinus EPSILON)
      }

      test("should convert pounds to kilograms") {
        val mass = Pounds.of(1.0)
        mass.into(Kilograms) shouldBe (0.45359237 plusOrMinus EPSILON)
      }

      test("should convert ounces to pounds") {
        val mass = Ounces.of(32.0)
        mass.into(Pounds) shouldBe (2.0 plusOrMinus EPSILON)
      }
    }

    context("Mass.plus() addition") {
      test("should add masses with same unit") {
        val m1 = Kilograms.of(10.0)
        val m2 = Kilograms.of(5.0)
        val result = m1 + m2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should add masses with different units") {
        val m1 = Kilograms.of(1.0)
        val m2 = Grams.of(500.0)
        val result = m1 + m2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should add grams to milligrams") {
        val m1 = Grams.of(1.0)
        val m2 = Milligrams.of(500.0)
        val result = m1 + m2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Grams
      }
    }

    context("Mass.minus() subtraction") {
      test("should subtract masses with same unit") {
        val m1 = Kilograms.of(10.0)
        val m2 = Kilograms.of(3.0)
        val result = m1 - m2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should subtract masses with different units") {
        val m1 = Kilograms.of(2.0)
        val m2 = Grams.of(500.0)
        val result = m1 - m2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should handle negative results") {
        val m1 = Grams.of(100.0)
        val m2 = Grams.of(200.0)
        val result = m1 - m2

        result.magnitude shouldBe (-100.0 plusOrMinus EPSILON)
      }
    }

    context("Mass.times() scalar multiplication") {
      test("should multiply mass by positive scalar") {
        val mass = Kilograms.of(10.0)
        val result = mass * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should multiply mass by fractional scalar") {
        val mass = Grams.of(100.0)
        val result = mass * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Mass.div() scalar division") {
      test("should divide mass by positive scalar") {
        val mass = Kilograms.of(20.0)
        val result = mass / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should divide mass by fractional scalar") {
        val mass = Grams.of(10.0)
        val result = mass / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("Mass.unaryMinus() negation") {
      test("should negate positive mass") {
        val mass = Kilograms.of(10.0)
        val result = -mass

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Kilograms
      }

      test("should negate negative mass") {
        val mass = Grams.of(-500.0)
        val result = -mass

        result.magnitude shouldBe (500.0 plusOrMinus EPSILON)
      }
    }

    context("Mass edge cases") {
      test("should handle very small masses") {
        val mass = Milligrams.of(0.001)
        mass.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large masses") {
        val mass = MetricTons.of(1000000.0)
        mass.magnitude shouldBe (1000000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val mass = Kilograms.of(123.456)
        val inGrams = mass.into(Grams)
        val backToKg = Grams.of(inGrams).into(Kilograms)

        backToKg shouldBe (123.456 plusOrMinus EPSILON)
      }
    }
  })
