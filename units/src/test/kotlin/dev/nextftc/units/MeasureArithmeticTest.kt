/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units

import dev.nextftc.units.measuretypes.Angle
import dev.nextftc.units.measuretypes.AngularVelocity
import dev.nextftc.units.measuretypes.Distance
import dev.nextftc.units.measuretypes.LinearAcceleration
import dev.nextftc.units.measuretypes.LinearVelocity
import dev.nextftc.units.measuretypes.Mul
import dev.nextftc.units.measuretypes.Per
import dev.nextftc.units.unittypes.AngleUnit
import dev.nextftc.units.unittypes.DistanceUnit
import dev.nextftc.units.unittypes.MulUnit
import dev.nextftc.units.unittypes.PerUnit
import dev.nextftc.units.unittypes.TimeUnit
import dev.nextftc.units.unittypes.VoltageUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val EPSILON = 1e-9

class MeasureArithmeticTest :
  FunSpec({
    context("Measure multiplication creates MulUnit") {
      test("Distance * Distance should create Mul (area)") {
        val d1 = Meters.of(10.0)
        val d2 = Meters.of(5.0)
        val result = d1 * d2

        result.shouldBeInstanceOf<Mul<DistanceUnit, DistanceUnit>>()
        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
        result.unit.first shouldBe Meters
        result.unit.second shouldBe Meters
      }

      test("Distance * Time should create Mul") {
        val distance = Meters.of(100.0)
        val time = Seconds.of(10.0)
        val result = distance * time

        result.shouldBeInstanceOf<Mul<DistanceUnit, TimeUnit>>()
        result.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
        result.unit.first shouldBe Meters
        result.unit.second shouldBe Seconds
      }

      test("Voltage * Time should create Mul (charge-like)") {
        val voltage = Volts.of(12.0)
        val time = Seconds.of(5.0)
        val result = voltage * time

        result.shouldBeInstanceOf<Mul<VoltageUnit, TimeUnit>>()
        result.magnitude shouldBe (60.0 plusOrMinus EPSILON)
      }

      test("Angle * Distance should create Mul") {
        val angle = Degrees.of(90.0)
        val distance = Meters.of(2.0)
        val result = angle * distance

        result.shouldBeInstanceOf<Mul<AngleUnit, DistanceUnit>>()
        result.magnitude shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("Multiplication with different units preserves units") {
        val km = Kilometers.of(5.0)
        val hr = Hours.of(2.0)
        val result = km * hr

        result.unit.first shouldBe Kilometers
        result.unit.second shouldBe Hours
        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }
    }

    context("Measure division creates PerUnit") {
      test("Distance / Time should create Per (velocity)") {
        val distance = Meters.of(100.0)
        val time = Seconds.of(10.0)
        val result = distance / time

        result.shouldBeInstanceOf<Per<DistanceUnit, TimeUnit>>()
        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
        result.unit.numerator shouldBe Meters
        result.unit.denominator shouldBe Seconds
      }

      test("Angle / Time should create Per (angular velocity)") {
        val angle = Degrees.of(180.0)
        val time = Seconds.of(2.0)
        val result = angle / time

        result.shouldBeInstanceOf<Per<AngleUnit, TimeUnit>>()
        result.magnitude shouldBe (90.0 plusOrMinus EPSILON)
      }

      test("Distance / Distance should create Per (dimensionless ratio)") {
        val d1 = Meters.of(50.0)
        val d2 = Meters.of(10.0)
        val result = d1 / d2

        result.shouldBeInstanceOf<Per<DistanceUnit, DistanceUnit>>()
        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("Voltage / Distance should create Per (electric field)") {
        val voltage = Volts.of(100.0)
        val distance = Meters.of(5.0)
        val result = voltage / distance

        result.shouldBeInstanceOf<Per<VoltageUnit, DistanceUnit>>()
        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }

      test("Division with different units preserves units") {
        val miles = Miles.of(60.0)
        val hours = Hours.of(2.0)
        val result = miles / hours

        result.unit.numerator shouldBe Miles
        result.unit.denominator shouldBe Hours
        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
      }
    }

    context("Specific typed operations override generic ones") {
      test("Distance / Time returns LinearVelocity, not generic Per") {
        val distance = Meters.of(50.0)
        val time = Seconds.of(5.0)
        val result = distance / time

        // Should be LinearVelocity due to specific override
        result.shouldBeInstanceOf<LinearVelocity>()
        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("Angle / Time returns AngularVelocity, not generic Per") {
        val angle = Radians.of(Math.PI)
        val time = Seconds.of(2.0)
        val result = angle / time

        result.shouldBeInstanceOf<AngularVelocity>()
        result.magnitude shouldBe (Math.PI / 2.0 plusOrMinus EPSILON)
      }

      test("LinearVelocity * Time returns Distance, not generic Mul") {
        val velocity = MetersPerSecond.of(25.0)
        val time = Seconds.of(4.0)
        val result = velocity * time

        result.shouldBeInstanceOf<Distance>()
        result.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("AngularVelocity * Time returns Angle, not generic Mul") {
        val velocity = DegreesPerSecond.of(45.0)
        val time = Seconds.of(4.0)
        val result = velocity * time

        result.shouldBeInstanceOf<Angle>()
        result.magnitude shouldBe (180.0 plusOrMinus EPSILON)
      }
    }

    context("Unit conversions work correctly in operations") {
      test("Mul converts to base units correctly") {
        val km = Kilometers.of(2.0)
        val hr = Hours.of(3.0)
        val result = km * hr

        // 2 km * 3 hr = 2000 m * 10800 s = 21,600,000 m⋅s
        result.baseUnitMagnitude shouldBe (21_600_000.0 plusOrMinus 1.0)
      }

      test("Per converts to base units correctly") {
        val miles = Miles.of(60.0)
        val hours = Hours.of(1.0)
        val result = miles / hours

        // 60 miles/hour ≈ 26.822 m/s
        result.baseUnitMagnitude shouldBe (26.822 plusOrMinus 0.001)
      }

      test("Converting Mul result to different MulUnit works") {
        val m = Meters.of(1000.0)
        val s = Seconds.of(3600.0)
        val result = m * s

        val kmHrUnit = MulUnit(Kilometers, Hours)
        result.into(kmHrUnit) shouldBe (1.0 plusOrMinus 0.001)
      }

      test("Converting Per result to different PerUnit works") {
        val meters = Meters.of(100.0)
        val seconds = Seconds.of(10.0)
        val result = meters / seconds

        val kmhUnit = PerUnit(Kilometers, Hours)
        result.into(kmhUnit) shouldBe (36.0 plusOrMinus 0.01)
      }
    }

    context("Chained operations work correctly") {
      test("(Distance * Distance) / Distance should work") {
        val d1 = Meters.of(20.0)
        val d2 = Meters.of(5.0)
        val d3 = Meters.of(2.0)

        val area = d1 * d2 // Mul
        val result = area / d3 // Per<Mul, Distance>

        result.shouldBeInstanceOf<Per<*, *>>()
        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("(Distance / Time) / Time should work (acceleration-like)") {
        val distance = Meters.of(100.0)
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(2.0)

        val velocity = distance / time1 // LinearVelocity (Per)
        val result = velocity / time2 // Per<Per, Time> or LinearAcceleration

        // Should be LinearAcceleration due to specific override
        result.shouldBeInstanceOf<LinearAcceleration>()
        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("Multiple multiplications create nested Mul types") {
        val d1 = Meters.of(2.0)
        val d2 = Meters.of(3.0)
        val d3 = Meters.of(4.0)

        val result1 = d1 * d2 // Mul<Distance, Distance>
        val result2 = result1 * d3 // Mul<Mul<Distance, Distance>, Distance>

        result2.shouldBeInstanceOf<Mul<*, *>>()
        result2.magnitude shouldBe (24.0 plusOrMinus EPSILON)
      }
    }

    context("Arithmetic with compound units") {
      test("Adding two Mul measurements works") {
        val m1 = Meters.of(10.0)
        val s1 = Seconds.of(5.0)
        val result1 = m1 * s1 // 50 m⋅s

        val m2 = Meters.of(8.0)
        val s2 = Seconds.of(5.0)
        val result2 = m2 * s2 // 40 m⋅s

        val sum = result1 + result2
        sum.magnitude shouldBe (90.0 plusOrMinus EPSILON)
      }

      test("Adding two Per measurements works") {
        val d1 = Meters.of(100.0)
        val t1 = Seconds.of(10.0)
        val v1 = d1 / t1 // 10 m/s

        val d2 = Meters.of(50.0)
        val t2 = Seconds.of(10.0)
        val v2 = d2 / t2 // 5 m/s

        val sum = v1 + v2
        sum.magnitude shouldBe (15.0 plusOrMinus EPSILON)
      }

      test("Scalar multiplication on Mul works") {
        val d = Meters.of(5.0)
        val t = Seconds.of(4.0)
        val result = d * t

        val scaled = result * 2.0
        scaled.magnitude shouldBe (40.0 plusOrMinus EPSILON)
      }

      test("Scalar division on Per works") {
        val d = Meters.of(100.0)
        val t = Seconds.of(10.0)
        val result = d / t

        val divided = result / 2.0
        divided.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }
    }

    context("Edge cases and special values") {
      test("Multiplying by zero creates zero Mul") {
        val d = Meters.of(0.0)
        val t = Seconds.of(5.0)
        val result = d * t

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("Dividing zero creates zero Per") {
        val d = Meters.of(0.0)
        val t = Seconds.of(5.0)
        val result = d / t

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("Operations with same unit types work") {
        val t1 = Seconds.of(10.0)
        val t2 = Seconds.of(5.0)

        val mul = t1 * t2 // 50 s⋅s
        mul.magnitude shouldBe (50.0 plusOrMinus EPSILON)

        val per = t1 / t2 // 2 s/s (dimensionless)
        per.magnitude shouldBe (2.0 plusOrMinus EPSILON)
      }
    }
  })
