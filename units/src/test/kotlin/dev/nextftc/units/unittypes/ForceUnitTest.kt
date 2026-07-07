/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.KilogramsForce
import dev.nextftc.units.Kilonewtons
import dev.nextftc.units.Newtons
import dev.nextftc.units.PoundsForce
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

@Suppress("ktlint:standard:property-naming")
class ForceUnitTest :
  FunSpec({
    context("ForceUnit constants") {
      test("Newtons should be the base unit") {
        Newtons.baseUnit shouldBeSameInstanceAs
          Newtons
      }

      test("all force units should have Newtons as base unit") {
        Kilonewtons.baseUnit shouldBeSameInstanceAs Newtons
        PoundsForce.baseUnit shouldBeSameInstanceAs Newtons
        KilogramsForce.baseUnit shouldBeSameInstanceAs Newtons
      }
    }

    context("ForceUnit.of() factory method") {
      test("should create Force measurements with correct magnitude") {
        val force = Newtons.of(10.0)
        force.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val kN = Kilonewtons.of(1.0)
        val N = Newtons.of(1000.0)

        kN.baseUnitMagnitude shouldBe (N.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val force = Newtons.of(2.5)
        force.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        force.baseUnitMagnitude shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("ForceUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val force = Kilonewtons.ofBaseUnits(1000.0) // 1000 N
        force.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should work for pounds-force") {
        val force = PoundsForce.ofBaseUnits(4.4482216152605) // 1 lbf in N
        force.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should work for kilograms-force") {
        val force = KilogramsForce.ofBaseUnits(9.80665) // 1 kgf in N
        force.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("ForceUnit conversions - metric") {
      test("should convert kilonewtons to newtons correctly") {
        val force = Kilonewtons.of(5.0)
        force.baseUnitMagnitude shouldBe (5000.0 plusOrMinus EPSILON)
      }

      test("should convert newtons to kilonewtons correctly") {
        val force = Newtons.of(2500.0)
        force.into(Kilonewtons) shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("ForceUnit conversions - imperial and other") {
      test("should convert pounds-force to newtons correctly") {
        val force = PoundsForce.of(1.0)
        force.baseUnitMagnitude shouldBe (4.4482216152605 plusOrMinus EPSILON)
      }

      test("should convert kilograms-force to newtons correctly") {
        val force = KilogramsForce.of(1.0)
        force.baseUnitMagnitude shouldBe (9.80665 plusOrMinus EPSILON)
      }

      test("should convert between pounds-force and kilograms-force") {
        val lbf = PoundsForce.of(2.20462)
        val kgf = KilogramsForce.of(1.0)

        // Approximately 2.2 lbf = 1 kgf
        lbf.baseUnitMagnitude shouldBe (kgf.baseUnitMagnitude plusOrMinus 0.01)
      }

      test("should maintain precision through conversions") {
        val original = Newtons.of(123.456)
        val inKilonewtons = original.into(Kilonewtons)
        val backToNewtons = Kilonewtons.of(inKilonewtons).into(Newtons)

        backToNewtons shouldBe (123.456 plusOrMinus EPSILON)
      }
    }

    context("ForceUnit utility methods") {
      test("isBaseUnit should return true for Newtons") { Newtons.isBaseUnit() shouldBe true }

      test("isBaseUnit should return false for derived units") {
        Kilonewtons.isBaseUnit() shouldBe false
        PoundsForce.isBaseUnit() shouldBe false
        KilogramsForce.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Newtons.name() shouldBe "newton"
        Kilonewtons.name() shouldBe "kilonewton"
        PoundsForce.name() shouldBe "pound-force"
        KilogramsForce.name() shouldBe "kilogram-force"
      }

      test("symbol() should return correct unit symbol") {
        Newtons.symbol() shouldBe "N"
        Kilonewtons.symbol() shouldBe "kN"
        PoundsForce.symbol() shouldBe "lbf"
        KilogramsForce.symbol() shouldBe "kgf"
      }
    }
  })
