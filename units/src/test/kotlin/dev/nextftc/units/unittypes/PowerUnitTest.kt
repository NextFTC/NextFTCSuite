/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Horsepower
import dev.nextftc.units.Kilowatts
import dev.nextftc.units.Megawatts
import dev.nextftc.units.Milliwatts
import dev.nextftc.units.Watts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class PowerUnitTest :
  FunSpec({
    context("PowerUnit constants") {
      test("Watts should be the base unit") { Watts.baseUnit shouldBeSameInstanceAs Watts }

      test("all power units should have Watts as base unit") {
        Milliwatts.baseUnit shouldBeSameInstanceAs Watts
        Kilowatts.baseUnit shouldBeSameInstanceAs Watts
        Megawatts.baseUnit shouldBeSameInstanceAs Watts
        Horsepower.baseUnit shouldBeSameInstanceAs Watts
      }
    }

    context("PowerUnit.of() factory method") {
      test("should create Power measurements with correct magnitude") {
        val power = Watts.of(10.0)
        power.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val kw = Kilowatts.of(1.0)
        val w = Watts.of(1000.0)

        kw.baseUnitMagnitude shouldBe (w.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val power = Kilowatts.of(2.5)
        power.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        power.baseUnitMagnitude shouldBe (2500.0 plusOrMinus EPSILON)
      }
    }

    context("PowerUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val power = Kilowatts.ofBaseUnits(5000.0) // 5000 watts
        power.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should work for milliwatts") {
        val power = Milliwatts.ofBaseUnits(1.0) // 1 watt
        power.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should work for megawatts") {
        val power = Megawatts.ofBaseUnits(1_000_000.0) // 1,000,000 W = 1 MW
        power.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("PowerUnit conversions") {
      test("should convert kilowatts to watts correctly") {
        val power = Kilowatts.of(5.0)
        power.baseUnitMagnitude shouldBe (5000.0 plusOrMinus EPSILON)
      }

      test("should convert milliwatts to watts correctly") {
        val power = Milliwatts.of(5000.0)
        power.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert megawatts to watts correctly") {
        val power = Megawatts.of(1.0)
        power.baseUnitMagnitude shouldBe (1_000_000.0 plusOrMinus EPSILON)
      }

      test("should convert horsepower to watts correctly") {
        val power = Horsepower.of(1.0)
        power.baseUnitMagnitude shouldBe (745.699872 plusOrMinus EPSILON)
      }
    }

    context("PowerUnit utility methods") {
      test("isBaseUnit should return true for Watts") { Watts.isBaseUnit() shouldBe true }

      test("isBaseUnit should return false for derived units") {
        Milliwatts.isBaseUnit() shouldBe false
        Kilowatts.isBaseUnit() shouldBe false
        Megawatts.isBaseUnit() shouldBe false
        Horsepower.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Watts.name() shouldBe "watt"
        Kilowatts.name() shouldBe "kilowatt"
        Horsepower.name() shouldBe "horsepower"
      }

      test("symbol() should return correct unit symbol") {
        Watts.symbol() shouldBe "W"
        Milliwatts.symbol() shouldBe "mW"
        Kilowatts.symbol() shouldBe "kW"
        Megawatts.symbol() shouldBe "MW"
        Horsepower.symbol() shouldBe "hp"
      }
    }
  })
