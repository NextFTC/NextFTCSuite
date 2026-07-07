/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.filters

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class SlewRateLimiterTest :
  FunSpec({
    context("SlewRateLimiter behavior") {
      test("limits positive rate") {
        val limiter = SlewRateLimiter(1.0, -1.0, 0.0)
        val timeSource = TestTimeSource()
        var mark = timeSource.markNow()

        limiter.calculate(10.0, mark) shouldBe (0.0 plusOrMinus 0.001)

        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(10.0, mark) shouldBe (1.0 plusOrMinus 0.001)

        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(10.0, mark) shouldBe (2.0 plusOrMinus 0.001)
      }

      test("limits negative rate") {
        val limiter = SlewRateLimiter(1.0, -1.0, 0.0)
        val timeSource = TestTimeSource()
        var mark = timeSource.markNow()

        limiter.calculate(-10.0, mark) shouldBe (0.0 plusOrMinus 0.001)

        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(-10.0, mark) shouldBe (-1.0 plusOrMinus 0.001)
      }

      test("respects asymmetrical limits") {
        val limiter = SlewRateLimiter(2.0, -1.0, 0.0)
        val timeSource = TestTimeSource()
        var mark = timeSource.markNow()

        limiter.calculate(10.0, mark)
        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(10.0, mark) shouldBe (2.0 plusOrMinus 0.001)
      }

      test("reaches target without overshooting") {
        val limiter = SlewRateLimiter(1.0, -1.0, 0.0)
        val timeSource = TestTimeSource()
        var mark = timeSource.markNow()

        limiter.calculate(0.5, mark)
        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(0.5, mark) shouldBe (0.5 plusOrMinus 0.001)
      }

      test("reset jumps immediately") {
        val limiter = SlewRateLimiter(1.0, -1.0, 0.0)
        val timeSource = TestTimeSource()
        var mark = timeSource.markNow()

        limiter.calculate(10.0, mark)
        limiter.reset(5.0, mark)
        limiter.lastValue shouldBe (5.0 plusOrMinus 0.001)

        timeSource.plusAssign(1.seconds)
        mark = timeSource.markNow()
        limiter.calculate(10.0, mark) shouldBe (6.0 plusOrMinus 0.001)
      }
    }
  })
