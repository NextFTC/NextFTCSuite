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

class MedianFilterTest :
  FunSpec({
    context("MedianFilter validation") {
      test("rejects size 0 or less") {
        shouldThrow<IllegalArgumentException> { MedianFilter(0) }
        shouldThrow<IllegalArgumentException> { MedianFilter(-1) }
      }
    }

    context("MedianFilter behavior") {
      test("filters outliers") {
        val filter = MedianFilter(5)

        filter.calculate(1.0) shouldBe (1.0 plusOrMinus 0.001)
        filter.calculate(2.0) shouldBe (1.5 plusOrMinus 0.001)
        filter.calculate(3.0) shouldBe (2.0 plusOrMinus 0.001)
        filter.calculate(4.0) shouldBe (2.5 plusOrMinus 0.001)
        filter.calculate(5.0) shouldBe (3.0 plusOrMinus 0.001)

        // Output should be median of [2.0, 3.0, 4.0, 5.0, 100.0] = 4.0
        filter.calculate(100.0) shouldBe (4.0 plusOrMinus 0.001)
      }

      test("handles even sizes correctly") {
        val filter = MedianFilter(4)

        filter.calculate(1.0)
        filter.calculate(2.0)
        filter.calculate(3.0)
        filter.calculate(4.0)
        // Values: [1, 2, 3, 4], median = (2+3)/2 = 2.5
        filter.calculate(5.0) shouldBe (3.5 plusOrMinus 0.001)
      }

      test("reset clears filter") {
        val filter = MedianFilter(3)
        filter.calculate(10.0)
        filter.calculate(20.0)

        filter.reset()
        filter.calculate(5.0) shouldBe (5.0 plusOrMinus 0.001)
      }
    }
  })
