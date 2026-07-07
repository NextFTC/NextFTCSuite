/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.NewtonCentimeters
import dev.nextftc.units.NewtonMeters
import dev.nextftc.units.NewtonMillimeters
import dev.nextftc.units.PoundFeet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class TorqueTest :
  FunSpec({
    context("Torque creation") {
      test("should create Torque with correct magnitude") {
        val torque = NewtonMeters.of(10.0)
        torque.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Torque with correct unit") {
        val torque = PoundFeet.of(5.0)
        torque.unit shouldBe PoundFeet
      }

      test("should create Torque with correct base unit magnitude") {
        val torque = NewtonCentimeters.of(200.0)
        torque.baseUnitMagnitude shouldBe (2.0 plusOrMinus EPSILON)
      }
    }

    context("Torque.into() conversions") {
      test("should convert newton-meters to newton-centimeters") {
        val torque = NewtonMeters.of(2.0)
        torque.into(NewtonCentimeters) shouldBe (200.0 plusOrMinus EPSILON)
      }

      test("should convert newton-centimeters to newton-meters") {
        val torque = NewtonCentimeters.of(300.0)
        torque.into(NewtonMeters) shouldBe (3.0 plusOrMinus EPSILON)
      }

      test("should convert newton-meters to newton-millimeters") {
        val torque = NewtonMeters.of(1.5)
        torque.into(NewtonMillimeters) shouldBe (1500.0 plusOrMinus EPSILON)
      }

      test("should convert newton-meters to pound-feet") {
        val torque = NewtonMeters.of(1.3558179483314)
        torque.into(PoundFeet) shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("Torque.plus() addition") {
      test("should add torques with same unit") {
        val t1 = NewtonMeters.of(10.0)
        val t2 = NewtonMeters.of(5.0)
        val result = t1 + t2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }

      test("should add torques with different units") {
        val t1 = NewtonMeters.of(1.0)
        val t2 = NewtonCentimeters.of(100.0)
        val result = t1 + t2

        result.magnitude shouldBe (2.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }
    }

    context("Torque.minus() subtraction") {
      test("should subtract torques with same unit") {
        val t1 = NewtonMeters.of(10.0)
        val t2 = NewtonMeters.of(3.0)
        val result = t1 - t2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }

      test("should subtract torques with different units") {
        val t1 = NewtonMeters.of(2.0)
        val t2 = NewtonCentimeters.of(50.0)
        val result = t1 - t2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }
    }

    context("Torque.times() multiplication") {
      test("should multiply torque by scalar") {
        val torque = NewtonMeters.of(5.0)
        val result = torque * 3.0

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }

      test("should handle negative multipliers") {
        val torque = NewtonMeters.of(10.0)
        val result = torque * -2.0

        result.magnitude shouldBe (-20.0 plusOrMinus EPSILON)
      }
    }

    context("Torque.div() division") {
      test("should divide torque by scalar") {
        val torque = NewtonMeters.of(20.0)
        val result = torque / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }
    }

    context("Torque.unaryMinus() negation") {
      test("should negate torque") {
        val torque = NewtonMeters.of(5.0)
        val result = -torque

        result.magnitude shouldBe (-5.0 plusOrMinus EPSILON)
        result.unit shouldBe NewtonMeters
      }
    }
  })
