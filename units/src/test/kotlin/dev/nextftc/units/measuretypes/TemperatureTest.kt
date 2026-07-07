/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Celsius
import dev.nextftc.units.Fahrenheit
import dev.nextftc.units.Kelvin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class TemperatureTest :
  FunSpec({
    context("Temperature creation") {
      test("should create Temperature with correct magnitude") {
        val temp = Celsius.of(25.0)
        temp.magnitude shouldBe (25.0 plusOrMinus EPSILON)
      }

      test("should create Temperature with correct unit") {
        val temp = Fahrenheit.of(72.0)
        temp.unit shouldBe Fahrenheit
      }

      test("should create Temperature with correct base unit magnitude") {
        val temp = Kelvin.of(300.0)
        temp.baseUnitMagnitude shouldBe (26.85 plusOrMinus EPSILON)
      }
    }

    context("Temperature.into() conversions") {
      test("should convert Celsius to Fahrenheit") {
        val temp = Celsius.of(100.0)
        temp.into(Fahrenheit) shouldBe (212.0 plusOrMinus EPSILON)
      }

      test("should convert Fahrenheit to Celsius") {
        val temp = Fahrenheit.of(32.0)
        temp.into(Celsius) shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("should convert Celsius to Kelvin") {
        val temp = Celsius.of(0.0)
        temp.into(Kelvin) shouldBe (273.15 plusOrMinus EPSILON)
      }

      test("should convert Kelvin to Celsius") {
        val temp = Kelvin.of(373.15)
        temp.into(Celsius) shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("should convert Fahrenheit to Kelvin") {
        val temp = Fahrenheit.of(32.0) // 0°C
        temp.into(Kelvin) shouldBe (273.15 plusOrMinus EPSILON)
      }

      test("should handle room temperature conversion") {
        val temp = Celsius.of(20.0)
        temp.into(Fahrenheit) shouldBe (68.0 plusOrMinus EPSILON)
      }
    }

    context("Temperature.plus() addition") {
      test("should add temperatures with same unit") {
        val t1 = Celsius.of(20.0)
        val t2 = Celsius.of(5.0)
        val result = t1 + t2

        result.magnitude shouldBe (25.0 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }

      test("should add temperatures with different units") {
        val t1 = Celsius.of(10.0)
        val t2 = Kelvin.of(10.0) // -263.15°C
        val result = t1 + t2

        result.magnitude shouldBe (-253.15 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }
    }

    context("Temperature.minus() subtraction") {
      test("should subtract temperatures with same unit") {
        val t1 = Celsius.of(30.0)
        val t2 = Celsius.of(10.0)
        val result = t1 - t2

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }

      test("should subtract temperatures with different units") {
        val t1 = Kelvin.of(300.0) // 26.85°C
        val t2 = Celsius.of(10.0)
        val result = t1 - t2

        result.magnitude shouldBe (290.0 plusOrMinus EPSILON)
        result.unit shouldBe Kelvin
      }
    }

    context("Temperature.times() multiplication") {
      test("should multiply temperature by scalar") {
        val temp = Celsius.of(10.0)
        val result = temp * 2.0

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }
    }

    context("Temperature.div() division") {
      test("should divide temperature by scalar") {
        val temp = Celsius.of(100.0)
        val result = temp / 4.0

        result.magnitude shouldBe (25.0 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }
    }

    context("Temperature.unaryMinus() negation") {
      test("should negate temperature") {
        val temp = Celsius.of(25.0)
        val result = -temp

        result.magnitude shouldBe (-25.0 plusOrMinus EPSILON)
        result.unit shouldBe Celsius
      }
    }

    context("Temperature comparisons") {
      test("should compare temperatures correctly") {
        val t1 = Celsius.of(20.0)
        val t2 = Celsius.of(30.0)

        (t1 < t2) shouldBe true
        (t2 > t1) shouldBe true
      }

      test("should compare temperatures with different units") {
        val t1 = Celsius.of(0.0)
        val t2 = Fahrenheit.of(32.0) // 0°C

        (t1.compareTo(t2)) shouldBe 0
      }

      test("boiling water comparison across units") {
        val boilingC = Celsius.of(100.0)
        val boilingF = Fahrenheit.of(212.0)
        val boilingK = Kelvin.of(373.15)

        (boilingC.baseUnitMagnitude) shouldBe
          (boilingF.baseUnitMagnitude plusOrMinus EPSILON)
        (boilingC.baseUnitMagnitude) shouldBe
          (boilingK.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }
  })
