/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Celsius
import dev.nextftc.units.Fahrenheit
import dev.nextftc.units.Kelvin
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class TemperatureUnitTest :
  FunSpec({
    context("TemperatureUnit constants") {
      test("Celsius should be the base unit") {
        Celsius.baseUnit shouldBeSameInstanceAs
          Celsius
      }

      test("all temperature units should have Celsius as base unit") {
        Fahrenheit.baseUnit shouldBeSameInstanceAs Celsius
        Kelvin.baseUnit shouldBeSameInstanceAs Celsius
      }
    }

    context("TemperatureUnit.of() factory method") {
      test("should create Temperature measurements with correct magnitude") {
        val temp = Celsius.of(25.0)
        temp.magnitude shouldBe (25.0 plusOrMinus EPSILON)
      }

      test("should handle negative temperatures") {
        val temp = Celsius.of(-10.0)
        temp.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
      }
    }

    context("TemperatureUnit.ofBaseUnits() factory method") {
      test("should create Fahrenheit from Celsius base units") {
        val temp = Fahrenheit.ofBaseUnits(0.0) // 0°C = 32°F
        temp.magnitude shouldBe (32.0 plusOrMinus EPSILON)
      }

      test("should create Kelvin from Celsius base units") {
        val temp = Kelvin.ofBaseUnits(0.0) // 0°C = 273.15K
        temp.magnitude shouldBe (273.15 plusOrMinus EPSILON)
      }
    }

    context("TemperatureUnit conversions - Celsius to Fahrenheit") {
      test("should convert 0°C to 32°F") {
        val temp = Celsius.of(0.0)
        temp.baseUnitMagnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("should convert 100°C to 212°F") {
        val temp = Fahrenheit.of(212.0)
        temp.baseUnitMagnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("should convert -40°C to -40°F (intersection point)") {
        val celsius = Celsius.of(-40.0)
        val fahrenheit = Fahrenheit.of(-40.0)
        celsius.baseUnitMagnitude shouldBe
          (fahrenheit.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should convert 20°C to 68°F") {
        val temp = Fahrenheit.of(68.0)
        temp.baseUnitMagnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("TemperatureUnit conversions - Celsius to Kelvin") {
      test("should convert 0°C to 273.15K") {
        val temp = Kelvin.of(273.15)
        temp.baseUnitMagnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("should convert -273.15°C to 0K (absolute zero)") {
        val temp = Kelvin.of(0.0)
        temp.baseUnitMagnitude shouldBe (-273.15 plusOrMinus EPSILON)
      }

      test("should convert 100°C to 373.15K") {
        val temp = Kelvin.of(373.15)
        temp.baseUnitMagnitude shouldBe (100.0 plusOrMinus EPSILON)
      }
    }

    context("TemperatureUnit utility methods") {
      test("isBaseUnit should return true for Celsius") { Celsius.isBaseUnit() shouldBe true }

      test("isBaseUnit should return false for derived units") {
        Fahrenheit.isBaseUnit() shouldBe false
        Kelvin.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Celsius.name() shouldBe "celsius"
        Fahrenheit.name() shouldBe "fahrenheit"
        Kelvin.name() shouldBe "kelvin"
      }

      test("symbol() should return correct unit symbol") {
        Celsius.symbol() shouldBe "°C"
        Fahrenheit.symbol() shouldBe "°F"
        Kelvin.symbol() shouldBe "K"
      }
    }
  })
