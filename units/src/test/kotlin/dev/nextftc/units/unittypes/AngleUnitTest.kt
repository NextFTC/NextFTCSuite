/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Degrees
import dev.nextftc.units.Gradians
import dev.nextftc.units.Radians
import dev.nextftc.units.Rotations
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class AngleUnitTest :
  FunSpec({
    context("AngleUnit constants") {
      test("Radians should be the base unit") {
        Radians.baseUnit shouldBeSameInstanceAs
          Radians
      }

      test("all angle units should have Radians as base unit") {
        Degrees.baseUnit shouldBeSameInstanceAs Radians
        Rotations.baseUnit shouldBeSameInstanceAs Radians
        Gradians.baseUnit shouldBeSameInstanceAs Radians
      }
    }

    context("AngleUnit.of() factory method") {
      test("should create Angle measurements with correct magnitude") {
        val angle = Radians.of(Math.PI)
        angle.magnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val deg = Degrees.of(180.0)
        val rad = Radians.of(Math.PI)

        deg.baseUnitMagnitude shouldBe (rad.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val angle = Degrees.of(45.5)
        angle.magnitude shouldBe (45.5 plusOrMinus EPSILON)
      }
    }

    context("AngleUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val angle = Degrees.ofBaseUnits(Math.PI) // π radians = 180 degrees
        angle.magnitude shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("should work for rotations") {
        val angle = Rotations.ofBaseUnits(2.0 * Math.PI) // 2π radians = 1 rotation
        angle.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should work for gradians") {
        val angle = Gradians.ofBaseUnits(Math.PI / 2.0) // π/2 radians = 100 gradians
        angle.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }
    }

    context("AngleUnit conversions") {
      test("should convert degrees to radians correctly") {
        val angle = Degrees.of(180.0)
        angle.baseUnitMagnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should convert degrees to radians for common angles") {
        Degrees.of(90.0).baseUnitMagnitude shouldBe (Math.PI / 2.0 plusOrMinus EPSILON)
        Degrees.of(45.0).baseUnitMagnitude shouldBe (Math.PI / 4.0 plusOrMinus EPSILON)
        Degrees.of(360.0).baseUnitMagnitude shouldBe (2.0 * Math.PI plusOrMinus EPSILON)
      }

      test("should convert rotations to radians correctly") {
        val angle = Rotations.of(1.0)
        angle.baseUnitMagnitude shouldBe (2.0 * Math.PI plusOrMinus EPSILON)
      }

      test("should convert gradians to radians correctly") {
        val angle = Gradians.of(200.0)
        angle.baseUnitMagnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should convert radians to degrees") {
        val angle = Radians.of(Math.PI)
        angle.into(Degrees) shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("should convert degrees to rotations") {
        val angle = Degrees.of(360.0)
        angle.into(Rotations) shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("AngleUnit zero and one") {
      test("zero() should return a measure with zero magnitude") {
        val zero = Radians.zero()
        zero.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("one() should return a measure with magnitude of 1") {
        val one = Degrees.one()
        one.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }
  })
