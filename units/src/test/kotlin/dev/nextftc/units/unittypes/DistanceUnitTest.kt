/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Centimeters
import dev.nextftc.units.Feet
import dev.nextftc.units.Inches
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Meters
import dev.nextftc.units.Miles
import dev.nextftc.units.Millimeters
import dev.nextftc.units.Yards
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class DistanceUnitTest :
  FunSpec({
    context("DistanceUnit constants") {
      test("Meters should be the base unit") { Meters.baseUnit shouldBeSameInstanceAs Meters }

      test("all distance units should have Meters as base unit") {
        Millimeters.baseUnit shouldBeSameInstanceAs Meters
        Centimeters.baseUnit shouldBeSameInstanceAs Meters
        Kilometers.baseUnit shouldBeSameInstanceAs Meters
        Inches.baseUnit shouldBeSameInstanceAs Meters
        Feet.baseUnit shouldBeSameInstanceAs Meters
        Yards.baseUnit shouldBeSameInstanceAs Meters
        Miles.baseUnit shouldBeSameInstanceAs Meters
      }
    }

    context("DistanceUnit.of() factory method") {
      test("should create Distance measurements with correct magnitude") {
        val distance = Meters.of(10.0)
        distance.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val mm = Millimeters.of(1000.0)
        val m = Meters.of(1.0)

        mm.baseUnitMagnitude shouldBe (m.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val distance = Kilometers.of(2.5)
        distance.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        distance.baseUnitMagnitude shouldBe (2500.0 plusOrMinus EPSILON)
      }
    }

    context("DistanceUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val distance = Kilometers.ofBaseUnits(5000.0) // 5000 meters
        distance.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should work for millimeters") {
        val distance = Millimeters.ofBaseUnits(1.0) // 1 meter
        distance.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should work for centimeters") {
        val distance = Centimeters.ofBaseUnits(2.0) // 2 meters
        distance.magnitude shouldBe (200.0 plusOrMinus EPSILON)
      }

      test("should work for inches") {
        val distance = Inches.ofBaseUnits(0.0254) // 0.0254 meters = 1 inch
        distance.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("DistanceUnit conversions") {
      test("should convert millimeters to meters correctly") {
        val distance = Millimeters.of(5000.0)
        distance.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert centimeters to meters correctly") {
        val distance = Centimeters.of(300.0)
        distance.baseUnitMagnitude shouldBe (3.0 plusOrMinus EPSILON)
      }

      test("should convert kilometers to meters correctly") {
        val distance = Kilometers.of(2.0)
        distance.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should convert inches to meters correctly") {
        val distance = Inches.of(1.0)
        distance.baseUnitMagnitude shouldBe (0.0254 plusOrMinus EPSILON)
      }

      test("should convert feet to meters correctly") {
        val distance = Feet.of(1.0)
        distance.baseUnitMagnitude shouldBe (0.3048 plusOrMinus 1e-10)
      }

      test("should convert yards to meters correctly") {
        val distance = Yards.of(1.0)
        distance.baseUnitMagnitude shouldBe (0.9144 plusOrMinus 1e-10)
      }

      test("should convert miles to meters correctly") {
        val distance = Miles.of(1.0)
        distance.baseUnitMagnitude shouldBe (1609.344 plusOrMinus 1e-10)
      }

      test("should convert meters to feet") {
        val distance = Meters.of(1.0)
        distance.into(Feet) shouldBe (3.280839895 plusOrMinus 1e-8)
      }

      test("should convert kilometers to miles") {
        val distance = Kilometers.of(1.0)
        distance.into(Miles) shouldBe (0.621371192 plusOrMinus 1e-8)
      }
    }

    context("DistanceUnit zero and one") {
      test("zero() should return a measure with zero magnitude") {
        val zero = Meters.zero()
        zero.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("one() should return a measure with magnitude of 1") {
        val one = Kilometers.one()
        one.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }
  })
