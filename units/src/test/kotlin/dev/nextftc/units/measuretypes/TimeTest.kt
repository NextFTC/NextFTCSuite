/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.measuretypes

import dev.nextftc.units.Days
import dev.nextftc.units.Hours
import dev.nextftc.units.Microseconds
import dev.nextftc.units.Milliseconds
import dev.nextftc.units.Minutes
import dev.nextftc.units.Nanoseconds
import dev.nextftc.units.Seconds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

private const val EPSILON = 1e-6

class TimeTest :
  FunSpec({
    context("Time creation") {
      test("should create Time with correct magnitude") {
        val time = Seconds.of(10.0)
        time.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should create Time with correct unit") {
        val time = Minutes.of(5.0)
        time.unit shouldBe Minutes
      }

      test("should create Time with correct base unit magnitude") {
        val time = Minutes.of(2.0)
        time.baseUnitMagnitude shouldBe (120.0 plusOrMinus EPSILON)
      }
    }

    context("Time.into() conversions") {
      test("should convert seconds to milliseconds") {
        val time = Seconds.of(2.0)
        time.into(Milliseconds) shouldBe (2000.0 plusOrMinus EPSILON)
      }

      test("should convert milliseconds to seconds") {
        val time = Milliseconds.of(3000.0)
        time.into(Seconds) shouldBe (3.0 plusOrMinus EPSILON)
      }

      test("should convert microseconds to milliseconds") {
        val time = Microseconds.of(5000.0)
        time.into(Milliseconds) shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should convert nanoseconds to microseconds") {
        val time = Nanoseconds.of(10000.0)
        time.into(Microseconds) shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should convert minutes to seconds") {
        val time = Minutes.of(3.0)
        time.into(Seconds) shouldBe (180.0 plusOrMinus EPSILON)
      }

      test("should convert hours to minutes") {
        val time = Hours.of(2.0)
        time.into(Minutes) shouldBe (120.0 plusOrMinus EPSILON)
      }

      test("should convert days to hours") {
        val time = Days.of(1.0)
        time.magnitude shouldBe 1.0
        time.unit shouldBe Days
        time.into(Hours) shouldBe (24.0 plusOrMinus EPSILON)
      }

      test("should handle same unit conversion") {
        val time = Seconds.of(42.0)
        time.into(Seconds) shouldBe (42.0 plusOrMinus EPSILON)
      }
    }

    context("Time.plus() addition") {
      test("should add times with same unit") {
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(5.0)
        val result = time1 + time2

        result.magnitude shouldBe (15.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should add times with different units") {
        val time1 = Seconds.of(60.0)
        val time2 = Minutes.of(1.0)
        val result = time1 + time2

        result.magnitude shouldBe (120.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should add milliseconds to seconds") {
        val time1 = Seconds.of(1.0)
        val time2 = Milliseconds.of(500.0)
        val result = time1 + time2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should add microseconds to milliseconds") {
        val time1 = Milliseconds.of(1.0)
        val time2 = Microseconds.of(1000.0)
        val result = time1 + time2

        result.magnitude shouldBe (2.0 plusOrMinus EPSILON)
        result.unit shouldBe Milliseconds
      }

      test("should add nanoseconds correctly") {
        val time1 = Nanoseconds.of(1_000_000_000.0)
        val time2 = Nanoseconds.of(500_000_000.0)
        val result = time1 + time2

        result.magnitude shouldBe (1_500_000_000.0 plusOrMinus EPSILON)
        result.unit shouldBe Nanoseconds
      }

      test("should preserve unit of left operand") {
        val time1 = Minutes.of(1.0)
        val time2 = Seconds.of(30.0)
        val result = time1 + time2

        result.unit shouldBe Minutes
        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
      }
    }

    context("Time.minus() subtraction") {
      test("should subtract times with same unit") {
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(3.0)
        val result = time1 - time2

        result.magnitude shouldBe (7.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should subtract times with different units") {
        val time1 = Minutes.of(2.0)
        val time2 = Seconds.of(30.0)
        val result = time1 - time2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Minutes
      }

      test("should handle negative results") {
        val time1 = Seconds.of(5.0)
        val time2 = Seconds.of(10.0)
        val result = time1 - time2

        result.magnitude shouldBe (-5.0 plusOrMinus EPSILON)
      }

      test("should subtract milliseconds from seconds") {
        val time1 = Seconds.of(2.0)
        val time2 = Milliseconds.of(500.0)
        val result = time1 - time2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should subtract microseconds from milliseconds") {
        val time1 = Milliseconds.of(2.0)
        val time2 = Microseconds.of(500.0)
        val result = time1 - time2

        result.magnitude shouldBe (1.5 plusOrMinus EPSILON)
        result.unit shouldBe Milliseconds
      }
    }

    context("Time.times() scalar multiplication") {
      test("should multiply time by positive scalar") {
        val time = Seconds.of(10.0)
        val result = time * 3.0

        result.magnitude shouldBe (30.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should multiply time by negative scalar") {
        val time = Minutes.of(5.0)
        val result = time * -2.0

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Minutes
      }

      test("should multiply time by fractional scalar") {
        val time = Seconds.of(100.0)
        val result = time * 0.5

        result.magnitude shouldBe (50.0 plusOrMinus EPSILON)
      }

      test("should multiply time by zero") {
        val time = Hours.of(3.0)
        val result = time * 0.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }

      test("should preserve unit after multiplication") {
        val time = Milliseconds.of(250.0)
        val result = time * 4.0

        result.unit shouldBe Milliseconds
        result.magnitude shouldBe (1000.0 plusOrMinus EPSILON)
      }
    }

    context("Time.div() scalar division") {
      test("should divide time by positive scalar") {
        val time = Seconds.of(20.0)
        val result = time / 4.0

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should divide time by fractional scalar") {
        val time = Minutes.of(10.0)
        val result = time / 0.5

        result.magnitude shouldBe (20.0 plusOrMinus EPSILON)
      }

      test("should preserve unit after division") {
        val time = Hours.of(6.0)
        val result = time / 3.0

        result.unit shouldBe Hours
        result.magnitude shouldBe (2.0 plusOrMinus EPSILON)
      }

      test("should handle small divisions") {
        val time = Milliseconds.of(1.0)
        val result = time / 2.0

        result.magnitude shouldBe (0.5 plusOrMinus EPSILON)
      }
    }

    context("Time.unaryMinus() negation") {
      test("should negate positive time") {
        val time = Seconds.of(10.0)
        val result = -time

        result.magnitude shouldBe (-10.0 plusOrMinus EPSILON)
        result.unit shouldBe Seconds
      }

      test("should negate negative time") {
        val time = Minutes.of(-5.0)
        val result = -time

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should negate zero time") {
        val time = Seconds.of(0.0)
        val result = -time

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }

    context("Time comparison") {
      test("should compare equal times") {
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(10.0)

        (time1.compareTo(time2)) shouldBe 0
      }

      test("should compare times with same unit") {
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(5.0)

        (time1.compareTo(time2)) shouldBeGreaterThan 0
        (time2.compareTo(time1)) shouldBeLessThan 0
      }

      test("should compare times with different units") {
        val time1 = Minutes.of(1.0)
        val time2 = Seconds.of(30.0)

        (time1.compareTo(time2)) shouldBeGreaterThan 0
      }

      test("should compare using base units") {
        val time1 = Milliseconds.of(1500.0)
        val time2 = Seconds.of(1.0)

        (time1.compareTo(time2)) shouldBeGreaterThan 0
      }
    }

    context("Time edge cases and precision") {
      test("should handle very small time values (nanoseconds)") {
        val time = Nanoseconds.of(1.0)
        time.magnitude shouldBe 1.0
        time.baseUnitMagnitude shouldBe 1e-9
      }

      test("should handle very large time values (days)") {
        val time = Days.of(365.0)
        time.magnitude shouldBe 365.0
        time.baseUnitMagnitude shouldBe 31_536_000.0 // 365 * 24 * 60 * 60
      }

      test("should maintain precision through conversions") {
        val time = Microseconds.of(1234.5)
        val inNano = time.into(Nanoseconds)
        val backToMicro = Nanoseconds.of(inNano).into(Microseconds)

        backToMicro shouldBe 1234.5
      }
    }

    context("Complex time operations") {
      test("should chain multiple operations") {
        val time = Seconds.of(100.0)
        val result = (time + Seconds.of(50.0)) * 2.0 - Seconds.of(100.0)

        result.magnitude shouldBe 200.0
      }

      test("should handle mixed unit operations") {
        val hours = Hours.of(1.0)
        val minutes = Minutes.of(30.0)
        val seconds = Seconds.of(45.0)

        val total = hours + minutes + seconds

        total.into(Seconds) shouldBe 5445.0
        total.unit shouldBe Hours
      }

      test("should compute average time") {
        val time1 = Seconds.of(10.0)
        val time2 = Seconds.of(20.0)
        val time3 = Seconds.of(30.0)

        val sum = time1 + time2 + time3
        val avg = sum / 3.0

        avg.magnitude shouldBe 20.0
      }
    }

    context("Time with zero") {
      test("should add zero time") {
        val time = Seconds.of(10.0)
        val zero = Seconds.zero()
        val result = time + zero

        result.magnitude shouldBe (10.0 plusOrMinus EPSILON)
      }

      test("should subtract zero time") {
        val time = Minutes.of(5.0)
        val zero = Minutes.zero()
        val result = time - zero

        result.magnitude shouldBe (5.0 plusOrMinus EPSILON)
      }

      test("should multiply zero time") {
        val zero = Seconds.zero()
        val result = zero * 100.0

        result.magnitude shouldBe (0.0 plusOrMinus EPSILON)
      }
    }
  })
