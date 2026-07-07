/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Centimeters
import dev.nextftc.units.Feet
import dev.nextftc.units.Inches
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Meters
import dev.nextftc.units.Miles
import dev.nextftc.units.Millimeters
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class DistanceTest :
  FunSpec({
    context("Distance creation") {
      test("should create Distance with correct magnitude") {
        val distance = Meters.of(10.0)
        distance.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Distance with correct unit") {
        val distance = Kilometers.of(5.0)
        distance.unit shouldBe Kilometers
      }

      test("should create Distance with correct base unit magnitude") {
        val distance = Kilometers.of(2.0)
        distance.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }
    }

    context("Distance.into() conversions") {
      test("should convert meters to millimeters") {
        val distance = Meters.of(2.0)
        distance.into(Millimeters) shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should convert millimeters to meters") {
        val distance = Millimeters.of(3000.0)
        distance.into(Meters) shouldBe (3.0 plusOrMinus EPSILON)
      }

      test("should convert kilometers to meters") {
        val distance = Kilometers.of(1.5)
        distance.into(Meters) shouldBe (1500.0 plusOrMinus EPSILON)
      }

      test("should convert inches to centimeters") {
        val distance = Inches.of(1.0)
        distance.into(Centimeters) shouldBe (2.54 plusOrMinus EPSILON)
      }

      test("should convert feet to meters") {
        val distance = Feet.of(10.0)
        distance.into(Meters) shouldBe (3.048 plusOrMinus EPSILON)
      }

      test("should convert miles to kilometers") {
        val distance = Miles.of(5.0)
        distance.into(Kilometers) shouldBe (8.04672 plusOrMinus 1e-5)
      }
    }

    context("Distance.plus() addition") {
      test("should add distances with same unit") {
        val d1 = Meters.of(10.0)
        val d2 = Meters.of(5.0)
        val result = d1 + d2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should add distances with different units") {
        val d1 = Meters.of(100.0)
        val d2 = Kilometers.of(1.0)
        val result = d1 + d2

        result.magnitude shouldBe (1100.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should add millimeters to centimeters") {
        val d1 = Centimeters.of(10.0)
        val d2 = Millimeters.of(50.0)
        val result = d1 + d2

        result.magnitude shouldBe (15.0 plusOrMinus 1e-8)
        result.unit shouldBe Centimeters
      }
    }

    context("Distance.minus() subtraction") {
      test("should subtract distances with same unit") {
        val d1 = Meters.of(10.0)
        val d2 = Meters.of(3.0)
        val result = d1 - d2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should subtract distances with different units") {
        val d1 = Kilometers.of(2.0)
        val d2 = Meters.of(500.0)
        val result = d1 - d2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilometers
      }

      test("should handle negative results") {
        val d1 = Meters.of(5.0)
        val d2 = Meters.of(10.0)
        val result = d1 - d2

        result.magnitude shouldBe (-5.0 plusOrMinus EPSILON)
      }
    }

    context("Distance.times() scalar multiplication") {
      test("should multiply distance by positive scalar") {
        val distance = Meters.of(10.0)
        val result = distance * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should multiply distance by fractional scalar") {
        val distance = Kilometers.of(100.0)
        val result = distance * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Distance.div() scalar division") {
      test("should divide distance by positive scalar") {
        val distance = Meters.of(20.0)
        val result = distance / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should divide distance by fractional scalar") {
        val distance = Kilometers.of(10.0)
        val result = distance / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("Distance.unaryMinus() negation") {
      test("should negate positive distance") {
        val distance = Meters.of(10.0)
        val result = -distance

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Meters
      }

      test("should negate negative distance") {
        val distance = Kilometers.of(-5.0)
        val result = -distance

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }
    }

    context("Distance edge cases") {
      test("should handle very small distances") {
        val distance = Millimeters.of(0.001)
        distance.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large distances") {
        val distance = Kilometers.of(1000000.0)
        distance.magnitude shouldBe (1000000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val distance = Meters.of(123.456)
        val inMm = distance.into(Millimeters)
        val backToM = Millimeters.of(inMm).into(Meters)

        backToM shouldBe (123.456 plusOrMinus EPSILON)
      }
    }
  })
