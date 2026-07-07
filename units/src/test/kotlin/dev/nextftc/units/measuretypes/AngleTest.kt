/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Degrees
import dev.nextftc.units.Gradians
import dev.nextftc.units.Radians
import dev.nextftc.units.Rotations
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class AngleTest :
  FunSpec({
    context("Angle creation") {
      test("should create Angle with correct magnitude") {
        val angle = Radians.of(Math.PI)
        angle.magnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should create Angle with correct unit") {
        val angle = Degrees.of(90.0)
        angle.unit shouldBe Degrees
      }

      test("should create Angle with correct base unit magnitude") {
        val angle = Degrees.of(180.0)
        angle.baseUnitMagnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }
    }

    context("Angle.into() conversions") {
      test("should convert radians to degrees") {
        val angle = Radians.of(Math.PI)
        angle.into(Degrees) shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("should convert degrees to radians") {
        val angle = Degrees.of(90.0)
        angle.into(Radians) shouldBe (Math.PI / 2.0 plusOrMinus EPSILON)
      }

      test("should convert degrees to rotations") {
        val angle = Degrees.of(720.0)
        angle.into(Rotations) shouldBe (2.0 plusOrMinus EPSILON)
      }

      test("should convert rotations to degrees") {
        val angle = Rotations.of(0.5)
        angle.into(Degrees) shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("should convert gradians to degrees") {
        val angle = Gradians.of(100.0)
        angle.into(Degrees) shouldBe (90.0 plusOrMinus EPSILON)
      }
    }

    context("Angle.plus() addition") {
      test("should add angles with same unit") {
        val a1 = Degrees.of(90.0)
        val a2 = Degrees.of(45.0)
        val result = a1 + a2

        result.magnitude shouldBe (135.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should add angles with different units") {
        val a1 = Degrees.of(90.0)
        val a2 = Radians.of(Math.PI / 2.0)
        val result = a1 + a2

        result.magnitude shouldBe (180.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should add rotations correctly") {
        val a1 = Rotations.of(0.25)
        val a2 = Rotations.of(0.75)
        val result = a1 + a2

        result.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("Angle.minus() subtraction") {
      test("should subtract angles with same unit") {
        val a1 = Degrees.of(180.0)
        val a2 = Degrees.of(45.0)
        val result = a1 - a2

        result.magnitude shouldBe (135.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should subtract angles with different units") {
        val a1 = Degrees.of(180.0)
        val a2 = Radians.of(Math.PI / 2.0)
        val result = a1 - a2

        result.magnitude shouldBe (90.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should handle negative results") {
        val a1 = Degrees.of(45.0)
        val a2 = Degrees.of(90.0)
        val result = a1 - a2

        result.magnitude shouldBe (-45.0 plusOrMinus EPSILON)
      }
    }

    context("Angle.times() scalar multiplication") {
      test("should multiply angle by positive scalar") {
        val angle = Degrees.of(45.0)
        val result = angle * 2.0

        result.magnitude shouldBe (90.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should multiply angle by fractional scalar") {
        val angle = Radians.of(Math.PI)
        val result = angle * 0.5

        result.magnitude shouldBe (Math.PI / 2.0 plusOrMinus EPSILON)
      }
    }

    context("Angle.div() scalar division") {
      test("should divide angle by positive scalar") {
        val angle = Degrees.of(180.0)
        val result = angle / 2.0

        result.magnitude shouldBe (90.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should divide angle by fractional scalar") {
        val angle = Radians.of(Math.PI)
        val result = angle / 0.5

        result.magnitude shouldBe (2.0 * Math.PI plusOrMinus EPSILON)
      }
    }

    context("Angle.unaryMinus() negation") {
      test("should negate positive angle") {
        val angle = Degrees.of(90.0)
        val result = -angle

        result.magnitude shouldBe (-90.0 plusOrMinus EPSILON)
        result.unit shouldBe Degrees
      }

      test("should negate negative angle") {
        val angle = Radians.of(-Math.PI)
        val result = -angle

        result.magnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }
    }

    context("Angle edge cases") {
      test("should handle very small angles") {
        val angle = Degrees.of(0.001)
        angle.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle angles greater than 360 degrees") {
        val angle = Degrees.of(720.0)
        angle.into(Rotations) shouldBe (2.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val angle = Degrees.of(123.456)
        val inRad = angle.into(Radians)
        val backToDeg = Radians.of(inRad).into(Degrees)

        backToDeg shouldBe (123.456 plusOrMinus EPSILON)
      }
    }
  })
