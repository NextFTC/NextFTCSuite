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
import dev.nextftc.units.Hours
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Kilovolts
import dev.nextftc.units.Meters
import dev.nextftc.units.Miles
import dev.nextftc.units.Millimeters
import dev.nextftc.units.Milliseconds
import dev.nextftc.units.Minutes
import dev.nextftc.units.Radians
import dev.nextftc.units.Seconds
import dev.nextftc.units.Volts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val EPSILON = 1e-6

class MulUnitTest :
  FunSpec({
    context("MulUnit creation") {
      test("should create MulUnit with correct first and second units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        meterSeconds.first shouldBeSameInstanceAs Meters
        meterSeconds.second shouldBeSameInstanceAs Seconds
      }

      test("should create MulUnit with different units") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        kilometerHours.first shouldBeSameInstanceAs Kilometers
        kilometerHours.second shouldBeSameInstanceAs Hours
      }

      test("should work with distance and angle units") {
        val meterDegrees = MulUnit(Meters, Degrees)
        meterDegrees.first shouldBeSameInstanceAs Meters
        meterDegrees.second shouldBeSameInstanceAs Degrees
      }

      test("should work with voltage and time units") {
        val voltSeconds = MulUnit(Volts, Seconds)
        voltSeconds.first shouldBeSameInstanceAs Volts
        voltSeconds.second shouldBeSameInstanceAs Seconds
      }
    }

    context("MulUnit base unit") {
      test("should return self when already using base units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        meterSeconds.baseMulUnit shouldBeSameInstanceAs meterSeconds
      }

      test("should return base MulUnit when using non-base units") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val base = kilometerHours.baseMulUnit

        base.first shouldBeSameInstanceAs Meters
        base.second shouldBeSameInstanceAs Seconds
      }

      test("should return base MulUnit for miles and hours") {
        val mileHours = MulUnit(Miles, Hours)
        val base = mileHours.baseMulUnit

        base.first shouldBeSameInstanceAs Meters
        base.second shouldBeSameInstanceAs Seconds
      }

      test("should return base MulUnit for degrees and milliseconds") {
        val degreeMs = MulUnit(Degrees, Milliseconds)
        val base = degreeMs.baseMulUnit

        base.first shouldBeSameInstanceAs Radians
        base.second shouldBeSameInstanceAs Seconds
      }
    }

    context("MulUnit.of() factory method") {
      test("should create Mul measurement with correct magnitude") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)

        measurement.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Mul measurement with correct unit") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(100.0)

        measurement.unit shouldBe kilometerHours
      }
    }

    context("MulUnit.ofBaseUnits() factory method") {
      test("should create Mul measurement from base units") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        // 3,600,000 meter⋅seconds converted to kilometer⋅hours
        // 1 km⋅hr = 1000 m × 3600 s = 3,600,000 m⋅s
        // Therefore: 3,600,000 m⋅s = 1 km⋅hr
        val measurement = kilometerHours.ofBaseUnits(3_600_000.0)

        measurement.magnitude shouldBe (1.0 plusOrMinus 0.001)
      }

      test("should work for base units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.ofBaseUnits(10.0)

        measurement.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }
    }

    context("MulUnit conversions to base units") {
      test("should convert kilometer⋅hours to meter⋅seconds") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(1.0)

        // 1 km⋅hr = 1000 m⋅3600 s = 3,600,000 m⋅s
        measurement.baseUnitMagnitude shouldBe (3_600_000.0 plusOrMinus 1.0)
      }

      test("should convert mile⋅hours to meter⋅seconds") {
        val mileHours = MulUnit(Miles, Hours)
        val measurement = mileHours.of(1.0)

        // 1 mi⋅hr = 1609.344 m⋅3600 s = 5,793,638.4 m⋅s
        measurement.baseUnitMagnitude shouldBe (5_793_638.4 plusOrMinus 1.0)
      }

      test("should convert centimeter⋅milliseconds to meter⋅seconds") {
        val cmMs = MulUnit(Centimeters, Milliseconds)
        val measurement = cmMs.of(1.0)

        // 1 cm⋅ms = 0.01 m⋅0.001 s = 0.00001 m⋅s
        measurement.baseUnitMagnitude shouldBe (0.00001 plusOrMinus EPSILON)
      }

      test("should convert degree⋅seconds to radian⋅seconds") {
        val degreeSeconds = MulUnit(Degrees, Seconds)
        val measurement = degreeSeconds.of(180.0)

        // 180 deg⋅s = π rad⋅s
        measurement.baseUnitMagnitude shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should handle identity conversion for base units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(25.0)

        measurement.baseUnitMagnitude shouldBe (25.0 plusOrMinus EPSILON)
      }
    }

    context("MulUnit conversions from base units") {
      test("should convert meter⋅seconds to kilometer⋅hours") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurementBase = meterSeconds.of(3_600_000.0)
        val measurementKmHr = kilometerHours.ofBaseUnits(measurementBase.baseUnitMagnitude)

        // 3,600,000 m⋅s = 1 km⋅hr
        measurementKmHr.magnitude shouldBe (1.0 plusOrMinus 0.001)
      }

      test("should convert meter⋅seconds to mile⋅hours") {
        val mileHours = MulUnit(Miles, Hours)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurementBase = meterSeconds.of(5_793_638.4)
        val measurementMiHr = mileHours.ofBaseUnits(measurementBase.baseUnitMagnitude)

        // 5,793,638.4 m⋅s ≈ 1 mi⋅hr
        measurementMiHr.magnitude shouldBe (1.0 plusOrMinus 0.001)
      }

      test("should convert radian⋅seconds to degree⋅seconds") {
        val degreeSeconds = MulUnit(Degrees, Seconds)
        val radianSeconds = MulUnit(Radians, Seconds)

        val measurementBase = radianSeconds.of(Math.PI)
        val measurementDeg = degreeSeconds.ofBaseUnits(measurementBase.baseUnitMagnitude)

        // π rad⋅s = 180 deg⋅s
        measurementDeg.magnitude shouldBe (180.0 plusOrMinus 0.001)
      }
    }

    context("MulUnit into() conversions") {
      test("should convert between compatible mul units") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurement = kilometerHours.of(1.0)

        // 1 km⋅hr = 3,600,000 m⋅s
        measurement.into(meterSeconds) shouldBe (3_600_000.0 plusOrMinus 1.0)
      }

      test("should convert centimeter⋅seconds to meter⋅seconds") {
        val cmSeconds = MulUnit(Centimeters, Seconds)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurement = cmSeconds.of(100.0)

        // 100 cm⋅s = 1 m⋅s
        measurement.into(meterSeconds) shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("MulUnit with mixed time units") {
      test("should handle meters⋅milliseconds") {
        val meterMs = MulUnit(Meters, Milliseconds)
        val measurement = meterMs.of(1.0)

        // 1 m⋅ms = 0.001 m⋅s
        measurement.baseUnitMagnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle kilometers⋅minutes") {
        val kmMin = MulUnit(Kilometers, Minutes)
        val measurement = kmMin.of(1.0)

        // 1 km⋅min = 1000 m⋅60 s = 60,000 m⋅s
        measurement.baseUnitMagnitude shouldBe (60_000.0 plusOrMinus EPSILON)
      }
    }

    context("MulUnit with mixed distance units") {
      test("should handle centimeters⋅seconds") {
        val cmSeconds = MulUnit(Centimeters, Seconds)
        val measurement = cmSeconds.of(100.0)

        // 100 cm⋅s = 1 m⋅s
        measurement.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should handle millimeters⋅seconds") {
        val mmSeconds = MulUnit(Millimeters, Seconds)
        val measurement = mmSeconds.of(1000.0)

        // 1000 mm⋅s = 1 m⋅s
        measurement.baseUnitMagnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }

    context("MulUnit with voltage and time") {
      test("should handle volt⋅seconds") {
        val voltSeconds = MulUnit(Volts, Seconds)
        val measurement = voltSeconds.of(10.0)

        measurement.baseUnitMagnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should handle kilovolt⋅hours") {
        val kvHours = MulUnit(Kilovolts, Hours)
        val measurement = kvHours.of(1.0)

        // 1 kV⋅hr = 1000 V⋅3600 s = 3,600,000 V⋅s
        measurement.baseUnitMagnitude shouldBe (3_600_000.0 plusOrMinus 1.0)
      }
    }

    context("MulUnit zero and one") {
      test("zero() should return a measure with zero magnitude") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val zero = meterSeconds.zero()

        zero.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("one() should return a measure with magnitude of 1") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val one = kilometerHours.one()

        one.magnitude shouldBe (1.0 plusOrMinus EPSILON)
      }
    }
  })
