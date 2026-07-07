/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.KilogramsForce
import dev.nextftc.units.Kilonewtons
import dev.nextftc.units.Newtons
import dev.nextftc.units.PoundsForce
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class ForceTest :
  FunSpec({
    context("Force creation") {
      test("should create Force with correct magnitude") {
        val force = Newtons.of(10.0)
        force.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Force with correct unit") {
        val force = Kilonewtons.of(5.0)
        force.unit shouldBe Kilonewtons
      }

      test("should create Force with correct base unit magnitude") {
        val force = Kilonewtons.of(2.0)
        force.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }
    }

    context("Force.into() conversions") {
      test("should convert newtons to kilonewtons") {
        val force = Newtons.of(5000.0)
        force.into(Kilonewtons) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert kilonewtons to newtons") {
        val force = Kilonewtons.of(3.0)
        force.into(Newtons) shouldBe (3000.0 plusOrMinus EPSILON)
      }

      test("should convert pounds-force to newtons") {
        val force = PoundsForce.of(1.0)
        force.into(Newtons) shouldBe (4.4482216152605 plusOrMinus EPSILON)
      }

      test("should convert kilograms-force to newtons") {
        val force = KilogramsForce.of(1.0)
        force.into(Newtons) shouldBe (9.80665 plusOrMinus EPSILON)
      }

      test("should convert newtons to pounds-force") {
        val force = Newtons.of(44.482216152605)
        force.into(PoundsForce) shouldBe (10.0 plusOrMinus 1e-6)
      }
    }

    context("Force.plus() addition") {
      test("should add forces with same unit") {
        val f1 = Newtons.of(10.0)
        val f2 = Newtons.of(5.0)
        val result = f1 + f2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }

      test("should add forces with different units") {
        val f1 = Kilonewtons.of(1.0)
        val f2 = Newtons.of(500.0)
        val result = f1 + f2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilonewtons
      }

      test("should add pounds-force to newtons") {
        val f1 = Newtons.of(10.0)
        val f2 = PoundsForce.of(1.0) // ~4.448 N
        val result = f1 + f2

        result.magnitude shouldBe (14.4482216152605 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }
    }

    context("Force.minus() subtraction") {
      test("should subtract forces with same unit") {
        val f1 = Newtons.of(10.0)
        val f2 = Newtons.of(3.0)
        val result = f1 - f2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }

      test("should subtract forces with different units") {
        val f1 = Kilonewtons.of(2.0)
        val f2 = Newtons.of(500.0)
        val result = f1 - f2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilonewtons
      }

      test("should handle negative results") {
        val f1 = Newtons.of(5.0)
        val f2 = Newtons.of(10.0)
        val result = f1 - f2

        result.magnitude shouldBe (-5.0 plusOrMinus EPSILON)
      }
    }

    context("Force.times() scalar multiplication") {
      test("should multiply force by positive scalar") {
        val force = Newtons.of(10.0)
        val result = force * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }

      test("should multiply force by fractional scalar") {
        val force = Kilonewtons.of(100.0)
        val result = force * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Force.div() scalar division") {
      test("should divide force by positive scalar") {
        val force = Newtons.of(20.0)
        val result = force / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }

      test("should divide force by fractional scalar") {
        val force = Kilonewtons.of(10.0)
        val result = force / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }
    }

    context("Force.unaryMinus() negation") {
      test("should negate positive force") {
        val force = Newtons.of(10.0)
        val result = -force

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Newtons
      }

      test("should negate negative force") {
        val force = Kilonewtons.of(-5.0)
        val result = -force

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }
    }

    context("Force edge cases") {
      test("should handle very small forces") {
        val force = Newtons.of(0.001)
        force.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large forces") {
        val force = Kilonewtons.of(1000000.0)
        force.magnitude shouldBe (1000000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val force = Newtons.of(123.456)
        val inKn = force.into(Kilonewtons)
        val backToN = Kilonewtons.of(inKn).into(Newtons)

        backToN shouldBe (123.456 plusOrMinus EPSILON)
      }
    }
  })
