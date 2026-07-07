/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Centimeters
import dev.nextftc.units.Degrees
import dev.nextftc.units.Feet
import dev.nextftc.units.Hours
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Meters
import dev.nextftc.units.Miles
import dev.nextftc.units.Millimeters
import dev.nextftc.units.Milliseconds
import dev.nextftc.units.Minutes
import dev.nextftc.units.Radians
import dev.nextftc.units.Seconds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-6

class PerUnitTest :
  FunSpec({
    context("PerUnit creation") {
      test("should create PerUnit with correct numerator and denominator") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        metersPerSecond.numerator shouldBeSameInstanceAs Meters
        metersPerSecond.denominator shouldBeSameInstanceAs Seconds
      }

      test("should create PerUnit with different units") {
        val milesPerHour = PerUnit(Miles, Hours)
        milesPerHour.numerator shouldBeSameInstanceAs Miles
        milesPerHour.denominator shouldBeSameInstanceAs Hours
      }

      test("should work with angle units") {
        val degreesPerSecond = PerUnit(Degrees, Seconds)
        degreesPerSecond.numerator shouldBeSameInstanceAs Degrees
        degreesPerSecond.denominator shouldBeSameInstanceAs Seconds
      }
    }

    context("PerUnit base unit") {
      test("should return self when already using base units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        metersPerSecond.basePerUnit shouldBeSameInstanceAs metersPerSecond
      }

      test("should return base PerUnit when using non-base units") {
        val milesPerHour = PerUnit(Miles, Hours)
        val base = milesPerHour.basePerUnit

        base.numerator shouldBeSameInstanceAs Meters
        base.denominator shouldBeSameInstanceAs Seconds
      }

      test("should return base PerUnit for kilometers per hour") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val base = kmPerHour.basePerUnit

        base.numerator shouldBeSameInstanceAs Meters
        base.denominator shouldBeSameInstanceAs Seconds
      }

      test("should return base PerUnit for degrees per millisecond") {
        val degPerMs = PerUnit(Degrees, Milliseconds)
        val base = degPerMs.basePerUnit

        base.numerator shouldBeSameInstanceAs Radians
        base.denominator shouldBeSameInstanceAs Seconds
      }
    }

    context("PerUnit.of() factory method") {
      test("should create Per measurement with correct magnitude") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)

        velocity.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Per measurement with correct unit") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)

        velocity.unit shouldBe milesPerHour
      }
    }

    context("PerUnit.ofBaseUnits() factory method") {
      test("should create Per measurement from base units") {
        val milesPerHour = PerUnit(Miles, Hours)
        // 1 meter/second ≈ 2.237 miles/hour
        val velocity = milesPerHour.ofBaseUnits(1.0)

        velocity.magnitude shouldBe (2.237 plusOrMinus 0.001)
      }

      test("should work for base units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.ofBaseUnits(10.0)

        velocity.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }
    }

    context("PerUnit conversions to base units") {
      test("should convert miles per hour to meters per second") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)

        // 60 mph ≈ 26.822 m/s
        velocity.baseUnitMagnitude shouldBe (26.822 plusOrMinus 0.001)
      }

      test("should convert kilometers per hour to meters per second") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val velocity = kmPerHour.of(100.0)

        // 100 km/h ≈ 27.778 m/s
        velocity.baseUnitMagnitude shouldBe (27.778 plusOrMinus 0.001)
      }

      test("should convert feet per second to meters per second") {
        val feetPerSecond = PerUnit(Feet, Seconds)
        val velocity = feetPerSecond.of(10.0)

        // 10 ft/s = 3.048 m/s
        velocity.baseUnitMagnitude shouldBe (3.048 plusOrMinus 0.001)
      }

      test("should convert degrees per millisecond to radians per second") {
        val degPerMs = PerUnit(Degrees, Milliseconds)
        val angularVel = degPerMs.of(1.0)

        // 1 deg/ms = 1000 deg/s = 17.453 rad/s
        angularVel.baseUnitMagnitude shouldBe (17.453 plusOrMinus 0.001)
      }

      test("should handle identity conversion for base units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(25.0)

        velocity.baseUnitMagnitude shouldBe (25.0 plusOrMinus EPSILON)
      }
    }

    context("PerUnit conversions from base units") {
      test("should convert meters per second to miles per hour") {
        val milesPerHour = PerUnit(Miles, Hours)
        val metersPerSecond = PerUnit(Meters, Seconds)

        val velocityBase = metersPerSecond.of(26.822)
        val velocityMph = milesPerHour.ofBaseUnits(velocityBase.baseUnitMagnitude)

        // 26.822 m/s ≈ 60 mph
        velocityMph.magnitude shouldBe (60.0 plusOrMinus 0.1)
      }

      test("should convert meters per second to kilometers per hour") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val metersPerSecond = PerUnit(Meters, Seconds)

        val velocityBase = metersPerSecond.of(27.778)
        val velocityKmh = kmPerHour.ofBaseUnits(velocityBase.baseUnitMagnitude)

        // 27.778 m/s ≈ 100 km/h
        velocityKmh.magnitude shouldBe (100.0 plusOrMinus 0.1)
      }

      test("should convert radians per second to degrees per second") {
        val degPerSec = PerUnit(Degrees, Seconds)
        val radPerSec = PerUnit(Radians, Seconds)

        val angularVelBase = radPerSec.of(Math.PI)
        val angularVelDeg = degPerSec.ofBaseUnits(angularVelBase.baseUnitMagnitude)

        // π rad/s = 180 deg/s
        angularVelDeg.magnitude shouldBe (180.0 plusOrMinus EPSILON)
      }
    }

    context("PerUnit into() conversions") {
      test("should convert between compatible per units") {
        val milesPerHour = PerUnit(Miles, Hours)
        val kmPerHour = PerUnit(Kilometers, Hours)

        val velocity = milesPerHour.of(60.0)

        // 60 mph ≈ 96.56 km/h
        velocity.into(kmPerHour) shouldBe (96.56 plusOrMinus 0.01)
      }

      test("should convert feet per second to meters per second") {
        val feetPerSecond = PerUnit(Feet, Seconds)
        val metersPerSecond = PerUnit(Meters, Seconds)

        val velocity = feetPerSecond.of(10.0)

        // 10 ft/s = 3.048 m/s
        velocity.into(metersPerSecond) shouldBe (3.048 plusOrMinus 0.001)
      }
    }

    context("PerUnit with mixed time units") {
      test("should handle meters per millisecond") {
        val metersPerMs = PerUnit(Meters, Milliseconds)
        val velocity = metersPerMs.of(1.0)

        // 1 m/ms = 1000 m/s
        velocity.baseUnitMagnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }

      test("should handle kilometers per minute") {
        val kmPerMin = PerUnit(Kilometers, Minutes)
        val velocity = kmPerMin.of(6.0)

        // 6 km/min = 6000 m / 60 s = 100 m/s
        velocity.baseUnitMagnitude shouldBe (100.0 plusOrMinus EPSILON)
      }
    }

    context("PerUnit with mixed distance units") {
      test("should handle centimeters per second") {
        val cmPerSecond = PerUnit(Centimeters, Seconds)
        val velocity = cmPerSecond.of(100.0)

        // 100 cm/s = 1 m/s
        velocity.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should handle millimeters per second") {
        val mmPerSecond = PerUnit(Millimeters, Seconds)
        val velocity = mmPerSecond.of(1000.0)

        // 1000 mm/s = 1 m/s
        velocity.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("PerUnit zero and one") {
      test("zero() should return a measure with zero magnitude") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val zero = metersPerSecond.zero()

        zero.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("one() should return a measure with magnitude of 1") {
        val milesPerHour = PerUnit(Miles, Hours)
        val one = milesPerHour.one()

        one.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }
  })
