/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Amperes
import dev.nextftc.units.Kiloamperes
import dev.nextftc.units.Microamperes
import dev.nextftc.units.Milliamperes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

@Suppress("ktlint:standard:property-naming")
class CurrentUnitTest :
  FunSpec({
    context("CurrentUnit constants") {
      test("Amperes should be the base unit") {
        Amperes.baseUnit shouldBeSameInstanceAs
          Amperes
      }

      test("all current units should have Amperes as base unit") {
        Milliamperes.baseUnit shouldBeSameInstanceAs Amperes
        Microamperes.baseUnit shouldBeSameInstanceAs Amperes
        Kiloamperes.baseUnit shouldBeSameInstanceAs Amperes
      }
    }

    context("CurrentUnit.of() factory method") {
      test("should create Current measurements with correct magnitude") {
        val current = Amperes.of(10.0)
        current.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val mA = Milliamperes.of(1000.0)
        val A = Amperes.of(1.0)

        mA.baseUnitMagnitude shouldBe (A.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val current = Amperes.of(2.5)
        current.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        current.baseUnitMagnitude shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("CurrentUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val current = Milliamperes.ofBaseUnits(1.0) // 1 A
        current.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should work for microamperes") {
        val current = Microamperes.ofBaseUnits(1.0) // 1 A
        current.magnitude shouldBe (1_000_000.0 plusOrMinus EPSILON)
      }

      test("should work for kiloamperes") {
        val current = Kiloamperes.ofBaseUnits(1000.0) // 1000 A
        current.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("CurrentUnit conversions") {
      test("should convert milliamperes to amperes correctly") {
        val current = Milliamperes.of(5000.0)
        current.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert microamperes to amperes correctly") {
        val current = Microamperes.of(1_000_000.0)
        current.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert kiloamperes to amperes correctly") {
        val current = Kiloamperes.of(2.0)
        current.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should maintain precision through conversions") {
        val original = Amperes.of(123.456)
        val inMilliamperes = original.into(Milliamperes)
        val backToAmperes = Milliamperes.of(inMilliamperes).into(Amperes)

        backToAmperes shouldBe (123.456 plusOrMinus EPSILON)
      }
    }

    context("CurrentUnit utility methods") {
      test("isBaseUnit should return true for Amperes") { Amperes.isBaseUnit() shouldBe true }

      test("isBaseUnit should return false for derived units") {
        Milliamperes.isBaseUnit() shouldBe false
        Microamperes.isBaseUnit() shouldBe false
        Kiloamperes.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Amperes.name() shouldBe "ampere"
        Milliamperes.name() shouldBe "milliampere"
        Microamperes.name() shouldBe "microampere"
        Kiloamperes.name() shouldBe "kiloampere"
      }

      test("symbol() should return correct unit symbol") {
        Amperes.symbol() shouldBe "A"
        Milliamperes.symbol() shouldBe "mA"
        Microamperes.symbol() shouldBe "µA"
        Kiloamperes.symbol() shouldBe "kA"
      }
    }
  })
