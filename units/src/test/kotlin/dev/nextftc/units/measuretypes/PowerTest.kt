/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Horsepower
import dev.nextftc.units.Kilowatts
import dev.nextftc.units.Megawatts
import dev.nextftc.units.Milliwatts
import dev.nextftc.units.Watts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class PowerTest :
  FunSpec({
    context("Power creation") {
      test("should create Power with correct magnitude") {
        val power = Watts.of(10.0)
        power.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Power with correct unit") {
        val power = Kilowatts.of(5.0)
        power.unit shouldBe Kilowatts
      }

      test("should create Power with correct base unit magnitude") {
        val power = Kilowatts.of(2.0)
        power.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }
    }

    context("Power.into() conversions") {
      test("should convert watts to kilowatts") {
        val power = Watts.of(5000.0)
        power.into(Kilowatts) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert kilowatts to watts") {
        val power = Kilowatts.of(3.0)
        power.into(Watts) shouldBe (3000.0 plusOrMinus EPSILON)
      }

      test("should convert watts to milliwatts") {
        val power = Watts.of(1.0)
        power.into(Milliwatts) shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should convert megawatts to kilowatts") {
        val power = Megawatts.of(1.0)
        power.into(Kilowatts) shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should convert horsepower to watts") {
        val power = Horsepower.of(1.0)
        power.into(Watts) shouldBe (745.699872 plusOrMinus EPSILON)
      }

      test("should convert watts to horsepower") {
        val power = Watts.of(745.699872)
        power.into(Horsepower) shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("Power.plus() addition") {
      test("should add powers with same unit") {
        val p1 = Watts.of(100.0)
        val p2 = Watts.of(50.0)
        val result = p1 + p2

        result.magnitude shouldBe (150.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }

      test("should add powers with different units") {
        val p1 = Watts.of(1000.0)
        val p2 = Kilowatts.of(1.0)
        val result = p1 + p2

        result.magnitude shouldBe (2000.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }

      test("should add milliwatts to watts") {
        val p1 = Watts.of(1.0)
        val p2 = Milliwatts.of(500.0)
        val result = p1 + p2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }
    }

    context("Power.minus() subtraction") {
      test("should subtract powers with same unit") {
        val p1 = Watts.of(100.0)
        val p2 = Watts.of(30.0)
        val result = p1 - p2

        result.magnitude shouldBe (70.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }

      test("should subtract powers with different units") {
        val p1 = Kilowatts.of(2.0)
        val p2 = Watts.of(500.0)
        val result = p1 - p2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilowatts
      }
    }

    context("Power.times() multiplication") {
      test("should multiply power by scalar") {
        val power = Watts.of(50.0)
        val result = power * 3.0

        result.magnitude shouldBe (150.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }

      test("should handle negative multipliers") {
        val power = Watts.of(100.0)
        val result = power * -2.0

        result.magnitude shouldBe (-200.0 plusOrMinus EPSILON)
      }
    }

    context("Power.div() division") {
      test("should divide power by scalar") {
        val power = Watts.of(200.0)
        val result = power / 4.0

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }
    }

    context("Power.unaryMinus() negation") {
      test("should negate power") {
        val power = Watts.of(50.0)
        val result = -power

        result.magnitude shouldBe (-50.0 plusOrMinus EPSILON)
        result.unit shouldBe Watts
      }
    }

    context("Power comparisons") {
      test("should compare powers correctly") {
        val p1 = Watts.of(100.0)
        val p2 = Watts.of(200.0)

        (p1 < p2) shouldBe true
        (p2 > p1) shouldBe true
      }

      test("should compare powers with different units") {
        val p1 = Kilowatts.of(1.0)
        val p2 = Watts.of(500.0)

        (p1 > p2) shouldBe true
      }
    }
  })
