/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Degrees
import dev.nextftc.units.Feet
import dev.nextftc.units.Hours
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Meters
import dev.nextftc.units.Miles
import dev.nextftc.units.Minutes
import dev.nextftc.units.Radians
import dev.nextftc.units.Rotations
import dev.nextftc.units.Seconds
import dev.nextftc.units.unittypes.PerUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-6

class PerTest :
  FunSpec({
    context("Per creation") {
      test("should create Per with correct magnitude") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)

        velocity.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Per with correct unit") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)

        velocity.unit shouldBe milesPerHour
      }

      test("should create Per with correct base unit magnitude") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val velocity = kmPerHour.of(100.0)

        // 100 km/h ≈ 27.778 m/s
        velocity.baseUnitMagnitude shouldBe (27.778 plusOrMinus 0.001)
      }
    }

    context("Per.into() conversions") {
      test("should convert miles per hour to kilometers per hour") {
        val milesPerHour = PerUnit(Miles, Hours)
        val kmPerHour = PerUnit(Kilometers, Hours)

        val velocity = milesPerHour.of(60.0)

        // 60 mph ≈ 96.56 km/h
        velocity.into(kmPerHour) shouldBe (96.56 plusOrMinus 0.01)
      }

      test("should convert kilometers per hour to meters per second") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val metersPerSecond = PerUnit(Meters, Seconds)

        val velocity = kmPerHour.of(36.0)

        // 36 km/h = 10 m/s
        velocity.into(metersPerSecond) shouldBe (10.0 plusOrMinus 0.001)
      }

      test("should convert feet per second to meters per second") {
        val feetPerSecond = PerUnit(Feet, Seconds)
        val metersPerSecond = PerUnit(Meters, Seconds)

        val velocity = feetPerSecond.of(10.0)

        // 10 ft/s = 3.048 m/s
        velocity.into(metersPerSecond) shouldBe (3.048 plusOrMinus 0.001)
      }

      test("should convert degrees per second to radians per second") {
        val degPerSec = PerUnit(Degrees, Seconds)
        val radPerSec = PerUnit(Radians, Seconds)

        val angularVel = degPerSec.of(180.0)

        // 180 deg/s = π rad/s
        angularVel.into(radPerSec) shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should handle same unit conversion") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(25.0)

        velocity.into(metersPerSecond) shouldBe (25.0 plusOrMinus EPSILON)
      }
    }

    context("Per.plus() addition") {
      test("should add velocities with same unit") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val v1 = metersPerSecond.of(10.0)
        val v2 = metersPerSecond.of(5.0)
        val result = v1 + v2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe metersPerSecond
      }

      test("should add velocities with different units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val kmPerHour = PerUnit(Kilometers, Hours)

        val v1 = metersPerSecond.of(10.0)
        val v2 = kmPerHour.of(36.0) // 36 km/h = 10 m/s
        val result = v1 + v2

        result.magnitude shouldBe (20.0 plusOrMinus 0.001)
        result.unit shouldBe metersPerSecond
      }

      test("should add miles per hour with feet per second") {
        val milesPerHour = PerUnit(Miles, Hours)
        val feetPerSecond = PerUnit(Feet, Seconds)

        val v1 = milesPerHour.of(60.0)
        val v2 = feetPerSecond.of(88.0) // 88 ft/s ≈ 60 mph
        val result = v1 + v2

        result.magnitude shouldBe (120.0 plusOrMinus 0.5)
        result.unit shouldBe milesPerHour
      }
    }

    context("Per.minus() subtraction") {
      test("should subtract velocities with same unit") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val v1 = metersPerSecond.of(15.0)
        val v2 = metersPerSecond.of(5.0)
        val result = v1 - v2

        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
        result.unit shouldBe metersPerSecond
      }

      test("should subtract velocities with different units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val kmPerHour = PerUnit(Kilometers, Hours)

        val v1 = metersPerSecond.of(20.0)
        val v2 = kmPerHour.of(36.0) // 36 km/h = 10 m/s
        val result = v1 - v2

        result.magnitude shouldBe (10.0 plusOrMinus 0.001)
        result.unit shouldBe metersPerSecond
      }

      test("should handle negative results") {
        val milesPerHour = PerUnit(Miles, Hours)
        val v1 = milesPerHour.of(30.0)
        val v2 = milesPerHour.of(60.0)
        val result = v1 - v2

        result.magnitude shouldBe (-30.0 plusOrMinus EPSILON)
      }
    }

    context("Per.times() scalar multiplication") {
      test("should multiply velocity by positive scalar") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)
        val result = velocity * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe metersPerSecond
      }

      test("should multiply velocity by negative scalar") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)
        val result = velocity * -0.5

        result.magnitude shouldBe (-30.0 plusOrMinus EPSILON)
        result.unit shouldBe milesPerHour
      }

      test("should multiply velocity by fractional scalar") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val velocity = kmPerHour.of(100.0)
        val result = velocity * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("should multiply velocity by zero") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(25.0)
        val result = velocity * 0.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }

    context("Per.div() scalar division") {
      test("should divide velocity by positive scalar") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(20.0)
        val result = velocity / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe metersPerSecond
      }

      test("should divide velocity by fractional scalar") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)
        val result = velocity / 0.5

        result.magnitude shouldBe (120.0 plusOrMinus EPSILON)
      }

      test("should preserve unit after division") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val velocity = kmPerHour.of(100.0)
        val result = velocity / 2.0

        result.unit shouldBe kmPerHour
        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Per.unaryMinus() negation") {
      test("should negate positive velocity") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)
        val result = -velocity

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe metersPerSecond
      }

      test("should negate negative velocity") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(-60.0)
        val result = -velocity

        result.magnitude shouldBe (60.0 plusOrMinus EPSILON)
      }

      test("should negate zero velocity") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val velocity = kmPerHour.of(0.0)
        val result = -velocity

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }

    context("Per comparison") {
      test("should compare velocities with same unit") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val v1 = metersPerSecond.of(10.0)
        val v2 = metersPerSecond.of(5.0)

        (v1.compareTo(v2)) shouldBe 1
        (v2.compareTo(v1)) shouldBe -1
      }

      test("should compare velocities with different units") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val kmPerHour = PerUnit(Kilometers, Hours)

        val v1 = metersPerSecond.of(10.0)
        val v2 = kmPerHour.of(36.0) // 36 km/h = 10 m/s

        (v1.compareTo(v2)) shouldBe 0
      }
    }

    context("Per edge cases and precision") {
      test("should handle very small velocities") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(0.001)

        velocity.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large velocities") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(299792458.0) // Speed of light

        velocity.magnitude shouldBe (299792458.0 plusOrMinus 1.0)
      }

      test("should maintain precision through conversions") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val feetPerSecond = PerUnit(Feet, Seconds)

        val velocity = metersPerSecond.of(123.456)
        val inFeet = velocity.into(feetPerSecond)
        val backToMeters = feetPerSecond.of(inFeet).into(metersPerSecond)

        backToMeters shouldBe (123.456 plusOrMinus 0.001)
      }
    }

    context("Complex per operations") {
      test("should chain multiple operations") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)
        val result = (velocity + metersPerSecond.of(5.0)) * 2.0 - metersPerSecond.of(10.0)

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }

      test("should compute average velocity") {
        val milesPerHour = PerUnit(Miles, Hours)
        val v1 = milesPerHour.of(50.0)
        val v2 = milesPerHour.of(60.0)
        val v3 = milesPerHour.of(70.0)

        val sum = v1 + v2 + v3
        val avg = sum / 3.0

        avg.magnitude shouldBe (60.0 plusOrMinus EPSILON)
      }
    }

    context("Per with angular velocities") {
      test("should handle degrees per second") {
        val degPerSec = PerUnit(Degrees, Seconds)
        val angularVel = degPerSec.of(90.0)

        angularVel.magnitude shouldBe (90.0 plusOrMinus EPSILON)
      }

      test("should convert degrees per second to radians per second") {
        val degPerSec = PerUnit(Degrees, Seconds)
        val radPerSec = PerUnit(Radians, Seconds)

        val angularVel = degPerSec.of(360.0)

        // 360 deg/s = 2π rad/s
        angularVel.into(radPerSec) shouldBe (2.0 * Math.PI plusOrMinus EPSILON)
      }

      test("should handle rotations per minute") {
        val rotPerMin = PerUnit(Rotations, Minutes)
        val radPerSec = PerUnit(Radians, Seconds)

        val angularVel = rotPerMin.of(60.0)

        // 60 rot/min = 1 rot/s = 2π rad/s
        angularVel.into(radPerSec) shouldBe (2.0 * Math.PI plusOrMinus EPSILON)
      }
    }

    context("Per with zero") {
      test("should add zero velocity") {
        val metersPerSecond = PerUnit(Meters, Seconds)
        val velocity = metersPerSecond.of(10.0)
        val zero = metersPerSecond.zero()
        val result = velocity + zero

        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should subtract zero velocity") {
        val milesPerHour = PerUnit(Miles, Hours)
        val velocity = milesPerHour.of(60.0)
        val zero = milesPerHour.zero()
        val result = velocity - zero

        result.magnitude shouldBe (60.0 plusOrMinus EPSILON)
      }

      test("should multiply zero velocity") {
        val kmPerHour = PerUnit(Kilometers, Hours)
        val zero = kmPerHour.zero()
        val result = zero * 100.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }
  })
