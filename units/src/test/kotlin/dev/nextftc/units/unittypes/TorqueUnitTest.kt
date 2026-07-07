/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.NewtonCentimeters
import dev.nextftc.units.NewtonMeters
import dev.nextftc.units.NewtonMillimeters
import dev.nextftc.units.PoundFeet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class TorqueUnitTest :
  FunSpec({
    context("TorqueUnit constants") {
      test("NewtonMeters should be the base unit") {
        NewtonMeters.baseUnit shouldBeSameInstanceAs NewtonMeters
      }

      test("all torque units should have NewtonMeters as base unit") {
        PoundFeet.baseUnit shouldBeSameInstanceAs NewtonMeters
        NewtonCentimeters.baseUnit shouldBeSameInstanceAs NewtonMeters
        NewtonMillimeters.baseUnit shouldBeSameInstanceAs NewtonMeters
      }
    }

    context("TorqueUnit.of() factory method") {
      test("should create Torque measurements with correct magnitude") {
        val torque = NewtonMeters.of(10.0)
        torque.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val ncm = NewtonCentimeters.of(100.0)
        val nm = NewtonMeters.of(1.0)

        ncm.baseUnitMagnitude shouldBe (nm.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val torque = NewtonMeters.of(2.5)
        torque.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        torque.baseUnitMagnitude shouldBe (2.5 plusOrMinus EPSILON)
      }
    }

    context("TorqueUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val torque = NewtonCentimeters.ofBaseUnits(1.0) // 1 N·m
        torque.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("should work for newton-millimeters") {
        val torque = NewtonMillimeters.ofBaseUnits(1.0) // 1 N·m
        torque.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }
    }

    context("TorqueUnit conversions") {
      test("should convert newton-centimeters to newton-meters correctly") {
        val torque = NewtonCentimeters.of(500.0)
        torque.baseUnitMagnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert newton-millimeters to newton-meters correctly") {
        val torque = NewtonMillimeters.of(2000.0)
        torque.baseUnitMagnitude shouldBe (2.0 plusOrMinus EPSILON)
      }

      test("should convert pound-feet to newton-meters correctly") {
        val torque = PoundFeet.of(1.0)
        torque.baseUnitMagnitude shouldBe (1.3558179483314 plusOrMinus EPSILON)
      }
    }

    context("TorqueUnit utility methods") {
      test("isBaseUnit should return true for NewtonMeters") {
        NewtonMeters.isBaseUnit() shouldBe true
      }

      test("isBaseUnit should return false for derived units") {
        PoundFeet.isBaseUnit() shouldBe false
        NewtonCentimeters.isBaseUnit() shouldBe false
        NewtonMillimeters.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        NewtonMeters.name() shouldBe "newton-meter"
        PoundFeet.name() shouldBe "pound-foot"
      }

      test("symbol() should return correct unit symbol") {
        NewtonMeters.symbol() shouldBe "N·m"
        PoundFeet.symbol() shouldBe "lb·ft"
      }
    }
  })
