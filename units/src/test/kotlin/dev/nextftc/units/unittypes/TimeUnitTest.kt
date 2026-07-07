/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units.unittypes

import dev.nextftc.units.Days
import dev.nextftc.units.Hours
import dev.nextftc.units.Microseconds
import dev.nextftc.units.Milliseconds
import dev.nextftc.units.Minutes
import dev.nextftc.units.Nanoseconds
import dev.nextftc.units.Seconds
import dev.nextftc.units.durationUnit
import dev.nextftc.units.timeUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.time.DurationUnit

class TimeUnitTest :
  FunSpec({
    context("TimeUnit constants") {
      test("Seconds should be the base unit") {
        Seconds.baseUnit shouldBeSameInstanceAs
          Seconds
      }

      test("all time units should have Seconds as base unit") {
        Milliseconds.baseUnit shouldBeSameInstanceAs Seconds
        Microseconds.baseUnit shouldBeSameInstanceAs Seconds
        Nanoseconds.baseUnit shouldBeSameInstanceAs Seconds
        Minutes.baseUnit shouldBeSameInstanceAs Seconds
        Hours.baseUnit shouldBeSameInstanceAs Seconds
        Days.baseUnit shouldBeSameInstanceAs Seconds
      }
    }

    context("TimeUnit.of() factory method") {
      test("should create Time measurements with correct magnitude") {
        val time = Seconds.of(10.0)
        time.magnitude shouldBe 10.0
      }

      test("should create measurements in different units") {
        val millisTime = Milliseconds.of(1000.0)
        val secondsTime = Seconds.of(1.0)

        millisTime.baseUnitMagnitude shouldBe secondsTime.baseUnitMagnitude
      }

      test("should handle fractional values") {
        val time = Minutes.of(2.5)
        time.magnitude shouldBe 2.5
        time.baseUnitMagnitude shouldBe 150.0 // 2.5 minutes = 150 seconds
      }
    }

    context("TimeUnit.ofBaseUnits() factory method") {
      test("should create measurements from base unit magnitude") {
        val time = Minutes.ofBaseUnits(120.0) // 120 seconds
        time.magnitude shouldBe 2.0 // 2 minutes
      }

      test("should work for milliseconds") {
        val time = Milliseconds.ofBaseUnits(5.0) // 5 seconds
        time.magnitude shouldBe 5000.0 // 5000 milliseconds
      }

      test("should work for microseconds") {
        val time = Microseconds.ofBaseUnits(1.0) // 1 second
        time.magnitude shouldBe 1_000_000.0 // 1 million microseconds
      }

      test("should work for nanoseconds") {
        val time = Nanoseconds.ofBaseUnits(1.0) // 1 second
        time.magnitude shouldBe 1_000_000_000.0 // 1 billion nanoseconds
      }
    }

    context("TimeUnit.durationUnit extension") {
      test("should map Seconds to DurationUnit.SECONDS") {
        Seconds.durationUnit shouldBe DurationUnit.SECONDS
      }

      test("should map Milliseconds to DurationUnit.MILLISECONDS") {
        Milliseconds.durationUnit shouldBe DurationUnit.MILLISECONDS
      }

      test("should map Microseconds to DurationUnit.MICROSECONDS") {
        Microseconds.durationUnit shouldBe DurationUnit.MICROSECONDS
      }

      test("should map Nanoseconds to DurationUnit.NANOSECONDS") {
        Nanoseconds.durationUnit shouldBe DurationUnit.NANOSECONDS
      }

      test("should map Minutes to DurationUnit.MINUTES") {
        Minutes.durationUnit shouldBe DurationUnit.MINUTES
      }

      test("should map Hours to DurationUnit.HOURS") {
        Hours.durationUnit shouldBe DurationUnit.HOURS
      }

      test("should map Days to DurationUnit.DAYS") {
        Days.durationUnit shouldBe DurationUnit.DAYS
      }
    }

    context("DurationUnit.timeUnit extension") {
      test("should map DurationUnit.SECONDS to Seconds") {
        DurationUnit.SECONDS.timeUnit shouldBeSameInstanceAs Seconds
      }

      test("should map DurationUnit.MILLISECONDS to Milliseconds") {
        DurationUnit.MILLISECONDS.timeUnit shouldBeSameInstanceAs Milliseconds
      }

      test("should map DurationUnit.MICROSECONDS to Microseconds") {
        DurationUnit.MICROSECONDS.timeUnit shouldBeSameInstanceAs Microseconds
      }

      test("should map DurationUnit.NANOSECONDS to Nanoseconds") {
        DurationUnit.NANOSECONDS.timeUnit shouldBeSameInstanceAs Nanoseconds
      }

      test("should map DurationUnit.MINUTES to Minutes") {
        DurationUnit.MINUTES.timeUnit shouldBeSameInstanceAs Minutes
      }

      test("should map DurationUnit.HOURS to Hours") {
        DurationUnit.HOURS.timeUnit shouldBeSameInstanceAs Hours
      }

      test("should map DurationUnit.DAYS to Days") {
        DurationUnit.DAYS.timeUnit shouldBeSameInstanceAs Days
      }
    }

    context("TimeUnit conversions") {
      test("should convert milliseconds to seconds correctly") {
        val time = Milliseconds.of(5000.0)
        time.baseUnitMagnitude shouldBe 5.0
      }

      test("should convert microseconds to seconds correctly") {
        val time = Microseconds.of(3_000_000.0)
        time.baseUnitMagnitude shouldBe 3.0
      }

      test("should convert nanoseconds to seconds correctly") {
        val time = Nanoseconds.of(2_000_000_000.0)
        time.baseUnitMagnitude shouldBe 2.0
      }

      test("should convert minutes to seconds correctly") {
        val time = Minutes.of(3.0)
        time.baseUnitMagnitude shouldBe 180.0
      }

      test("should convert hours to seconds correctly") {
        val time = Hours.of(2.0)
        time.baseUnitMagnitude shouldBe 7200.0
      }

      test("should convert days to seconds correctly") {
        val time = Days.of(1.0)
        time.baseUnitMagnitude shouldBe 86400.0 // 24 * 60 * 60
      }
    }

    context("TimeUnit zero and one") {
      test("zero() should return a measure with zero magnitude") {
        val zero = Seconds.zero()
        zero.magnitude shouldBe 0.0
      }

      test("one() should return a measure with magnitude of 1") {
        val one = Minutes.one()
        one.magnitude shouldBe 1.0
      }

      test("zero should be cached") {
        val zero1 = Seconds.zero()
        val zero2 = Seconds.zero()
        zero1 shouldBeSameInstanceAs zero2
      }

      test("one should be cached") {
        val one1 = Seconds.one()
        val one2 = Seconds.one()
        one1 shouldBeSameInstanceAs one2
      }
    }
  })
