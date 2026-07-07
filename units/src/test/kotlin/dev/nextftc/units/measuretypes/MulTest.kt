/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Centimeters
import dev.nextftc.units.Degrees
import dev.nextftc.units.Hours
import dev.nextftc.units.Kilometers
import dev.nextftc.units.Kilovolts
import dev.nextftc.units.Meters
import dev.nextftc.units.Radians
import dev.nextftc.units.Seconds
import dev.nextftc.units.Volts
import dev.nextftc.units.unittypes.MulUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-6

class MulTest :
  FunSpec({
    context("Mul creation") {
      test("should create Mul with correct magnitude") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)

        measurement.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Mul with correct unit") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(100.0)

        measurement.unit shouldBe kilometerHours
      }

      test("should create Mul with correct base unit magnitude") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(1.0)

        // 1 km⋅hr = 3,600,000 m⋅s
        measurement.baseUnitMagnitude shouldBe (3_600_000.0 plusOrMinus 1.0)
      }
    }

    context("Mul.into() conversions") {
      test("should convert kilometer⋅hours to meter⋅seconds") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurement = kilometerHours.of(1.0)

        // 1 km⋅hr = 3,600,000 m⋅s
        measurement.into(meterSeconds) shouldBe (3_600_000.0 plusOrMinus 1.0)
      }

      test("should convert meter⋅seconds to kilometer⋅hours") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val kilometerHours = MulUnit(Kilometers, Hours)

        val measurement = meterSeconds.of(3_600_000.0)

        // 3,600,000 m⋅s = 1 km⋅hr
        measurement.into(kilometerHours) shouldBe (1.0 plusOrMinus 0.001)
      }

      test("should convert centimeter⋅seconds to meter⋅seconds") {
        val cmSeconds = MulUnit(Centimeters, Seconds)
        val meterSeconds = MulUnit(Meters, Seconds)

        val measurement = cmSeconds.of(100.0)

        // 100 cm⋅s = 1 m⋅s
        measurement.into(meterSeconds) shouldBe (1.0 plusOrMinus EPSILON)
      }

      test("should convert degree⋅seconds to radian⋅seconds") {
        val degreeSeconds = MulUnit(Degrees, Seconds)
        val radianSeconds = MulUnit(Radians, Seconds)

        val measurement = degreeSeconds.of(180.0)

        // 180 deg⋅s = π rad⋅s
        measurement.into(radianSeconds) shouldBe (Math.PI plusOrMinus EPSILON)
      }

      test("should handle same unit conversion") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(25.0)

        measurement.into(meterSeconds) shouldBe (25.0 plusOrMinus EPSILON)
      }
    }

    context("Mul.plus() addition") {
      test("should add measurements with same unit") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val m1 = meterSeconds.of(10.0)
        val m2 = meterSeconds.of(5.0)
        val result = m1 + m2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe meterSeconds
      }

      test("should add measurements with different units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val cmSeconds = MulUnit(Centimeters, Seconds)

        val m1 = meterSeconds.of(1.0)
        val m2 = cmSeconds.of(100.0) // 100 cm⋅s = 1 m⋅s
        val result = m1 + m2

        result.magnitude shouldBe (2.0 plusOrMinus 0.001)
        result.unit shouldBe meterSeconds
      }

      test("should add kilometer⋅hours with meter⋅seconds") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val meterSeconds = MulUnit(Meters, Seconds)

        val m1 = kilometerHours.of(1.0)
        val m2 = meterSeconds.of(3_600_000.0) // 3,600,000 m⋅s = 1 km⋅hr
        val result = m1 + m2

        result.magnitude shouldBe (2.0 plusOrMinus 0.001)
        result.unit shouldBe kilometerHours
      }
    }

    context("Mul.minus() subtraction") {
      test("should subtract measurements with same unit") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val m1 = meterSeconds.of(15.0)
        val m2 = meterSeconds.of(5.0)
        val result = m1 - m2

        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
        result.unit shouldBe meterSeconds
      }

      test("should subtract measurements with different units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val cmSeconds = MulUnit(Centimeters, Seconds)

        val m1 = meterSeconds.of(2.0)
        val m2 = cmSeconds.of(100.0) // 100 cm⋅s = 1 m⋅s
        val result = m1 - m2

        result.magnitude shouldBe (1.0 plusOrMinus 0.001)
        result.unit shouldBe meterSeconds
      }

      test("should handle negative results") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val m1 = kilometerHours.of(1.0)
        val m2 = kilometerHours.of(2.0)
        val result = m1 - m2

        result.magnitude shouldBe (-1.0 plusOrMinus EPSILON)
      }
    }

    context("Mul.times() scalar multiplication") {
      test("should multiply measurement by positive scalar") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)
        val result = measurement * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe meterSeconds
      }

      test("should multiply measurement by negative scalar") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(100.0)
        val result = measurement * -0.5

        result.magnitude shouldBe (-50.0 plusOrMinus EPSILON)
        result.unit shouldBe kilometerHours
      }

      test("should multiply measurement by fractional scalar") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(100.0)
        val result = measurement * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("should multiply measurement by zero") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(25.0)
        val result = measurement * 0.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }

    context("Mul.div() scalar division") {
      test("should divide measurement by positive scalar") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(20.0)
        val result = measurement / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe meterSeconds
      }

      test("should divide measurement by fractional scalar") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(100.0)
        val result = measurement / 0.5

        result.magnitude shouldBe (200.0 plusOrMinus EPSILON)
      }

      test("should preserve unit after division") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(100.0)
        val result = measurement / 2.0

        result.unit shouldBe meterSeconds
        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }
    }

    context("Mul.unaryMinus() negation") {
      test("should negate positive measurement") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)
        val result = -measurement

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe meterSeconds
      }

      test("should negate negative measurement") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(-100.0)
        val result = -measurement

        result.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("should negate zero measurement") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(0.0)
        val result = -measurement

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }

    context("Mul comparison") {
      test("should compare measurements with same unit") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val m1 = meterSeconds.of(10.0)
        val m2 = meterSeconds.of(5.0)

        (m1.compareTo(m2)) shouldBe 1
        (m2.compareTo(m1)) shouldBe -1
      }

      test("should compare measurements with different units") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val cmSeconds = MulUnit(Centimeters, Seconds)

        val m1 = meterSeconds.of(1.0)
        val m2 = cmSeconds.of(100.0) // 100 cm⋅s = 1 m⋅s

        (m1.compareTo(m2)) shouldBe 0
      }
    }

    context("Mul edge cases and precision") {
      test("should handle very small measurements") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(0.001)

        measurement.magnitude shouldBe (0.001 plusOrMinus EPSILON)
      }

      test("should handle very large measurements") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(1_000_000.0)

        measurement.magnitude shouldBe (1_000_000.0 plusOrMinus 1.0)
      }

      test("should maintain precision through conversions") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val cmSeconds = MulUnit(Centimeters, Seconds)

        val measurement = meterSeconds.of(123.456)
        val inCm = measurement.into(cmSeconds)
        val backToM = cmSeconds.of(inCm).into(meterSeconds)

        backToM shouldBe (123.456 plusOrMinus 0.001)
      }
    }

    context("Complex mul operations") {
      test("should chain multiple operations") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)
        val result = (measurement + meterSeconds.of(5.0)) * 2.0 - meterSeconds.of(10.0)

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }

      test("should compute average measurement") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val m1 = kilometerHours.of(100.0)
        val m2 = kilometerHours.of(200.0)
        val m3 = kilometerHours.of(300.0)

        val sum = m1 + m2 + m3
        val avg = sum / 3.0

        avg.magnitude shouldBe (200.0 plusOrMinus EPSILON)
      }
    }

    context("Mul with energy-like units") {
      test("should handle volt⋅seconds (similar to Joules)") {
        val voltSeconds = MulUnit(Volts, Seconds)
        val energy = voltSeconds.of(10.0)

        energy.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should convert kilovolt⋅hours to volt⋅seconds") {
        val kvHours = MulUnit(Kilovolts, Hours)
        val voltSeconds = MulUnit(Volts, Seconds)

        val energy = kvHours.of(1.0)

        // 1 kV⋅hr = 3,600,000 V⋅s
        energy.into(voltSeconds) shouldBe (3_600_000.0 plusOrMinus 1.0)
      }
    }

    context("Mul with zero") {
      test("should add zero measurement") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val measurement = meterSeconds.of(10.0)
        val zero = meterSeconds.zero()
        val result = measurement + zero

        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should subtract zero measurement") {
        val kilometerHours = MulUnit(Kilometers, Hours)
        val measurement = kilometerHours.of(100.0)
        val zero = kilometerHours.zero()
        val result = measurement - zero

        result.magnitude shouldBe (100.0 plusOrMinus EPSILON)
      }

      test("should multiply zero measurement") {
        val meterSeconds = MulUnit(Meters, Seconds)
        val zero = meterSeconds.zero()
        val result = zero * 100.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }
  })
