/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Joules
import dev.nextftc.units.Kilojoules
import dev.nextftc.units.KilowattHours
import dev.nextftc.units.Seconds
import dev.nextftc.units.WattHours
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-9

class EnergyTest :
  FunSpec({
    context("Energy creation") {
      test("should create Energy with correct magnitude") {
        val energy = Joules.of(10.0)
        energy.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Energy with correct unit") {
        val energy = Kilojoules.of(5.0)
        energy.unit shouldBe Kilojoules
      }

      test("should create Energy with correct base unit magnitude") {
        val energy = Kilojoules.of(2.0)
        energy.baseUnitMagnitude shouldBe (2000.0 plusOrMinus EPSILON)
      }
    }

    context("Energy.into() conversions") {
      test("should convert joules to kilojoules") {
        val energy = Joules.of(5000.0)
        energy.into(Kilojoules) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert kilojoules to joules") {
        val energy = Kilojoules.of(3.0)
        energy.into(Joules) shouldBe (3000.0 plusOrMinus EPSILON)
      }

      test("should convert joules to watt-hours") {
        val energy = Joules.of(3600.0)
        energy.into(WattHours) shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert watt-hours to kilowatt-hours") {
        val energy = WattHours.of(1000.0)
        energy.into(KilowattHours) shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert kilowatt-hours to joules") {
        val energy = KilowattHours.of(1.0)
        energy.into(Joules) shouldBe (3_600_000.0 plusOrMinus EPSILON)
      }
    }

    context("Energy.plus() addition") {
      test("should add energies with same unit") {
        val e1 = Joules.of(100.0)
        val e2 = Joules.of(50.0)
        val result = e1 + e2

        result.magnitude shouldBe (150.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }

      test("should add energies with different units") {
        val e1 = Joules.of(1000.0)
        val e2 = Kilojoules.of(1.0)
        val result = e1 + e2

        result.magnitude shouldBe (2000.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }
    }

    context("Energy.minus() subtraction") {
      test("should subtract energies with same unit") {
        val e1 = Joules.of(100.0)
        val e2 = Joules.of(30.0)
        val result = e1 - e2

        result.magnitude shouldBe (70.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }

      test("should subtract energies with different units") {
        val e1 = Kilojoules.of(2.0)
        val e2 = Joules.of(500.0)
        val result = e1 - e2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Kilojoules
      }
    }

    context("Energy.times() multiplication") {
      test("should multiply energy by scalar") {
        val energy = Joules.of(50.0)
        val result = energy * 3.0

        result.magnitude shouldBe (150.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }

      test("should handle negative multipliers") {
        val energy = Joules.of(100.0)
        val result = energy * -2.0

        result.magnitude shouldBe (-200.0 plusOrMinus EPSILON)
      }
    }

    context("Energy.div() division") {
      test("should divide energy by scalar") {
        val energy = Joules.of(200.0)
        val result = energy / 4.0

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }

      test("should divide energy by time to get power rate") {
        val energy = Joules.of(100.0)
        val time = Seconds.of(10.0)
        val result = energy / time

        // 100 J / 10 s = 10 J/s
        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }
    }

    context("Energy.unaryMinus() negation") {
      test("should negate energy") {
        val energy = Joules.of(50.0)
        val result = -energy

        result.magnitude shouldBe (-50.0 plusOrMinus EPSILON)
        result.unit shouldBe Joules
      }
    }
  })
