/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class InterpolatingMapTest :
  FunSpec({
    test("linear interpolation exact match") {
      val map = InterpolatingMap.linear()
      map[0.0] = 0.0
      map[1.0] = 1.0
      map[2.0] = 4.0

      map[0.0] shouldBe 0.0
      map[1.0] shouldBe 1.0
      map[2.0] shouldBe 4.0
    }

    test("linear interpolation intermediate values") {
      val map = InterpolatingMap.linear()
      map[0.0] = 0.0
      map[1.0] = 1.0
      map[2.0] = 4.0

      map[0.5] shouldBe (0.5).plusOrMinus(1e-6)
      map[1.5] shouldBe (2.5).plusOrMinus(1e-6)
    }

    test("linear interpolation with lists") {
      val map = InterpolatingMap.linear(listOf(0.0, 1.0, 2.0), listOf(0.0, 1.0, 4.0))
      map[0.5] shouldBe (0.5).plusOrMinus(1e-6)
      map[1.5] shouldBe (2.5).plusOrMinus(1e-6)
    }

    test("spline interpolation exact match") {
      val map = InterpolatingMap.spline()
      map[0.0] = 0.0
      map[1.0] = 1.0
      map[2.0] = 0.5
      map[3.0] = 2.0

      map[0.0] shouldBe 0.0
      map[1.0] shouldBe 1.0
      map[2.0] shouldBe 0.5
      map[3.0] shouldBe 2.0
    }

    test("spline interpolation with lists") {
      val map = InterpolatingMap.spline(listOf(0.0, 1.0, 2.0, 3.0), listOf(0.0, 1.0, 0.5, 2.0))
      map[1.5] shouldBe splerp(map, 1.5).plusOrMinus(1e-6)
    }
  })
