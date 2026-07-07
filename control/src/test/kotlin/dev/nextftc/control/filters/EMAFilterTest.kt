/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.filters

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class EMAFilterTest :
  FunSpec({
    context("EMAFilter alpha validation") {
      test("rejects alpha below 0") {
        shouldThrow<IllegalArgumentException> { EMAFilter(-0.1) }
      }

      test("rejects alpha above 1") {
        shouldThrow<IllegalArgumentException> { EMAFilter(1.1) }
      }

      test("accepts alpha bounds") {
        EMAFilter(0.0)
        EMAFilter(1.0)
      }
    }

    context("EMAFilter behavior") {
      test("first output uses zero previous by default") {
        val filter = EMAFilter(0.5)
        val output = filter.calculate(10.0)
        output shouldBe (5.0 plusOrMinus 0.001)
        filter.previous shouldBe (5.0 plusOrMinus 0.001)
      }

      test("respects prior output when filtering next sample") {
        val filter = EMAFilter(0.5)
        val first = filter.calculate(8.0)
        val second = filter.calculate(10.0)
        // first = 0.5 * 8 + 0.5 * 0 = 4
        // second = 0.5 * 10 + 0.5 * 4 = 7
        first shouldBe (4.0 plusOrMinus 0.001)
        second shouldBe (7.0 plusOrMinus 0.001)
      }

      test("alpha 0 freezes output at previous") {
        val filter = EMAFilter(0.0)
        filter.calculate(10.0) shouldBe (0.0 plusOrMinus 0.001)
        filter.calculate(20.0) shouldBe (0.0 plusOrMinus 0.001)
      }

      test("alpha 1 outputs latest sample") {
        val filter = EMAFilter(1.0)
        filter.calculate(10.0) shouldBe (10.0 plusOrMinus 0.001)
        filter.calculate(-2.0) shouldBe (-2.0 plusOrMinus 0.001)
      }

      test("applies recurrence over multiple samples") {
        val filter = EMAFilter(0.25)
        val first = filter.calculate(8.0)
        val second = filter.calculate(4.0)
        // first = 0.25 * 8 + 0.75 * 0 = 2
        // second = 0.25 * 4 + 0.75 * 2 = 2.5
        first shouldBe (2.0 plusOrMinus 0.001)
        second shouldBe (2.5 plusOrMinus 0.001)
      }
    }
  })
