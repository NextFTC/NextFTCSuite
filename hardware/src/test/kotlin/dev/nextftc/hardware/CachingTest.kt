/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CachingTest :
  FunSpec({
    class Holder {
      val written = mutableListOf<Double?>()
      var value by Caching(0.1) { written += it }
    }

    test("reads default to zero before any write") {
      val holder = Holder()
      holder.value shouldBe 0.0
    }

    test("first write is always applied") {
      val holder = Holder()
      holder.value = 5.0
      holder.written shouldBe listOf(5.0)
      holder.value shouldBe 5.0
    }

    test("write within tolerance of cached value is suppressed") {
      val holder = Holder()
      holder.value = 5.0
      holder.value = 5.05
      holder.written shouldBe listOf(5.0, null)
      holder.value shouldBe 5.0
    }

    test("write outside tolerance is applied and updates the cache") {
      val holder = Holder()
      holder.value = 5.0
      holder.value = 5.2
      holder.written shouldBe listOf(5.0, 5.2)
      holder.value shouldBe 5.2
    }

    test("write exactly at the tolerance boundary is suppressed") {
      val holder = Holder()
      holder.value = 5.0
      holder.value = 5.1
      holder.written shouldBe listOf(5.0, null)
    }
  })
