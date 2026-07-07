/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Joules
import dev.nextftc.units.Kilojoules
import dev.nextftc.units.KilowattHours
import dev.nextftc.units.WattHours
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-9

class EnergyUnitTest :
  FunSpec({
    context("EnergyUnit constants") {
      test("Joules should be the base unit") { Joules.baseUnit shouldBeSameInstanceAs Joules }

      test("all energy units should have Joules as base unit") {
        Kilojoules.baseUnit shouldBeSameInstanceAs Joules
        WattHours.baseUnit shouldBeSameInstanceAs Joules
        KilowattHours.baseUnit shouldBeSameInstanceAs Joules
      }
    }

    context("EnergyUnit.of() factory method") {
      test("should create Energy measurements with correct magnitude") {
        val energy = Joules.of(10.0)
        energy.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create measurements in different units") {
        val kj = Kilojoules.of(1.0)
        val j = Joules.of(1000.0)

        kj.baseUnitMagnitude shouldBe (j.baseUnitMagnitude plusOrMinus EPSILON)
      }

      test("should handle fractional values") {
        val energy = Kilojoules.of(2.5)
        energy.magnitude shouldBe (2.5 plusOrMinus EPSILON)
        energy.baseUnitMagnitude shouldBe (2500.0 plusOrMinus EPSILON)
      }
    }

    context("EnergyUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val energy = Kilojoules.ofBaseUnits(5000.0) // 5000 joules
        energy.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should work for watt-hours") {
        val energy = WattHours.ofBaseUnits(3600.0) // 3600 joules = 1 Wh
        energy.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should work for kilowatt-hours") {
        val energy = KilowattHours.ofBaseUnits(3_600_000.0) // 3,600,000 J = 1 kWh
        energy.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("EnergyUnit conversions") {
      test("should convert kilojoules to joules correctly") {
        val energy = Kilojoules.of(5.0)
        energy.baseUnitMagnitude shouldBe (5000.0 plusOrMinus EPSILON)
      }

      test("should convert watt-hours to joules correctly") {
        val energy = WattHours.of(1.0)
        energy.baseUnitMagnitude shouldBe (3600.0 plusOrMinus EPSILON)
      }

      test("should convert kilowatt-hours to joules correctly") {
        val energy = KilowattHours.of(1.0)
        energy.baseUnitMagnitude shouldBe (3_600_000.0 plusOrMinus EPSILON)
      }

      test("should convert between watt-hours and kilowatt-hours") {
        val whEnergy = WattHours.of(1000.0)
        val kwhEnergy = KilowattHours.of(1.0)

        whEnergy.baseUnitMagnitude shouldBe
          (kwhEnergy.baseUnitMagnitude plusOrMinus EPSILON)
      }
    }

    context("EnergyUnit utility methods") {
      test("isBaseUnit should return true for Joules") { Joules.isBaseUnit() shouldBe true }

      test("isBaseUnit should return false for derived units") {
        Kilojoules.isBaseUnit() shouldBe false
        WattHours.isBaseUnit() shouldBe false
        KilowattHours.isBaseUnit() shouldBe false
      }

      test("name() should return correct unit name") {
        Joules.name() shouldBe "joule"
        Kilojoules.name() shouldBe "kilojoule"
        WattHours.name() shouldBe "watt-hour"
      }

      test("symbol() should return correct unit symbol") {
        Joules.symbol() shouldBe "J"
        Kilojoules.symbol() shouldBe "kJ"
        WattHours.symbol() shouldBe "Wh"
        KilowattHours.symbol() shouldBe "kWh"
      }
    }
  })
