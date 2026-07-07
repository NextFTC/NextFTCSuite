/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Grams
import dev.nextftc.units.Kilograms
import dev.nextftc.units.MetricTons
import dev.nextftc.units.Milligrams
import dev.nextftc.units.Ounces
import dev.nextftc.units.Pounds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class MassUnitTest :
  FunSpec({
    context("MassUnit constants") {
      test("Kilograms should be the base unit") {
        Kilograms.baseUnit shouldBeSameInstanceAs Kilograms
      }

      test("metric mass units should have Kilograms as base unit") {
        Grams.baseUnit shouldBeSameInstanceAs Kilograms
        Milligrams.baseUnit shouldBeSameInstanceAs Kilograms
        MetricTons.baseUnit shouldBeSameInstanceAs Kilograms
      }

      test("imperial mass units should have Kilograms as base unit") {
        Pounds.baseUnit shouldBeSameInstanceAs Kilograms
        Ounces.baseUnit shouldBeSameInstanceAs Kilograms
      }
    }

    context("MassUnit.of() factory method") {
      test("should create Mass measurements with correct magnitude") {
        val mass = Kilograms.of(10.0)
        mass.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val g = Grams.of(1000.0)
        val kg = Kilograms.of(1.0)

        g.baseUnitMagnitude shouldBe (kg.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val mass = Kilograms.of(2.5)
        mass.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        mass.baseUnitMagnitude shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("MassUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val mass = Grams.ofBaseUnits(1.0) // 1 kg
        mass.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should work for milligrams") {
        val mass = Milligrams.ofBaseUnits(1.0) // 1 kg
        mass.magnitude shouldBe (1_000_000.0 plusOrMinus EPSILON)
      }

      test("should work for metric tons") {
        val mass = MetricTons.ofBaseUnits(1000.0) // 1000 kg
        mass.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("MassUnit conversions - metric") {
      test("should convert grams to kilograms correctly") {
        val mass = Grams.of(5000.0)
        mass.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert milligrams to kilograms correctly") {
        val mass = Milligrams.of(1_000_000.0)
        mass.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert metric tons to kilograms correctly") {
        val mass = MetricTons.of(2.0)
        mass.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }
    }

    context("MassUnit conversions - imperial") {
      test("should convert pounds to kilograms correctly") {
        val mass = Pounds.of(1.0)
        mass.baseUnitMagnitude shouldBe (0.45359237 plusOrMinus EPSILON)
      }

      test("should convert ounces to kilograms correctly") {
        val mass = Ounces.of(16.0) // 1 pound
        mass.baseUnitMagnitude shouldBe (0.45359237 plusOrMinus EPSILON)
      }

      test("should convert 2.2 pounds to approximately 1 kilogram") {
        val mass = Pounds.of(2.20462262185)
        mass.baseUnitMagnitude shouldBe (1.0 plusOrMinus 1e-6)
      }
    }

    context("MassUnit utility methods") {
      test("isBaseUnit should return true for Kilograms") {
        Kilograms.isBaseUnit() shouldBe
          true
      }

      test("isBaseUnit should return false for derived units") {
        Grams.isBaseUnit() shouldBe false
        Milligrams.isBaseUnit() shouldBe false
        MetricTons.isBaseUnit() shouldBe false
        Pounds.isBaseUnit() shouldBe false
        Ounces.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Kilograms.name() shouldBe "kilogram"
        Grams.name() shouldBe "gram"
        Pounds.name() shouldBe "pound"
      }

      test("symbol() should return correct unit symbol") {
        Kilograms.symbol() shouldBe "kg"
        Grams.symbol() shouldBe "g"
        Milligrams.symbol() shouldBe "mg"
        MetricTons.symbol() shouldBe "t"
        Pounds.symbol() shouldBe "lb"
        Ounces.symbol() shouldBe "oz"
      }
    }
  })
