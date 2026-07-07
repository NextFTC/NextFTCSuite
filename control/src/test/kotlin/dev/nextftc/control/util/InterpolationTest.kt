/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.util

import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.N2
import dev.nextftc.units.Meters
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.util.TreeMap

class InterpolationTest :
  FunSpec({
    test("lerp with min max bounds") {
      lerp(0.5, 0.0, 1.0, 10.0, 20.0) shouldBe (15.0).plusOrMinus(1e-6)
      lerp(0.0, 0.0, 1.0, 10.0, 20.0) shouldBe (10.0).plusOrMinus(1e-6)
      lerp(1.0, 0.0, 1.0, 10.0, 20.0) shouldBe (20.0).plusOrMinus(1e-6)
      lerp(2.0, 0.0, 1.0, 10.0, 20.0) shouldBe (30.0).plusOrMinus(1e-6)
      lerp(0.5, 0.0, 0.0, 10.0, 20.0) shouldBe (0.0).plusOrMinus(1e-6)
    }

    test("lerp with t") {
      lerp(10.0, 20.0, 0.5) shouldBe (15.0).plusOrMinus(1e-6)
      lerp(10.0, 20.0, 0.0) shouldBe (10.0).plusOrMinus(1e-6)
      lerp(10.0, 20.0, 1.0) shouldBe (20.0).plusOrMinus(1e-6)
    }

    test("antiLerp") {
      antiLerp(15.0, 10.0, 20.0) shouldBe (0.5).plusOrMinus(1e-6)
      antiLerp(10.0, 10.0, 20.0) shouldBe (0.0).plusOrMinus(1e-6)
      antiLerp(20.0, 10.0, 20.0) shouldBe (1.0).plusOrMinus(1e-6)
    }

    test("lerpLookup exact match") {
      val source = listOf(0.0, 1.0, 2.0)
      val target = listOf(10.0, 20.0, 30.0)
      lerpLookup(source, target, 1.0) shouldBe 20.0
    }

    test("lerpLookup interpolation") {
      val source = listOf(0.0, 1.0, 2.0)
      val target = listOf(10.0, 20.0, 30.0)
      lerpLookup(source, target, 0.5) shouldBe (15.0).plusOrMinus(1e-6)
      lerpLookup(source, target, 1.5) shouldBe (25.0).plusOrMinus(1e-6)
    }

    test("lerpLookup out of bounds") {
      val source = listOf(0.0, 1.0, 2.0)
      val target = listOf(10.0, 20.0, 30.0)
      lerpLookup(source, target, -1.0) shouldBe 10.0
      lerpLookup(source, target, 3.0) shouldBe 30.0
    }

    test("lerpMatrix") {
      val m1 = Matrix.zero(N2, N2)
      val m2 = Matrix.identity(N2)
      val m3 = lerpMatrix(0.5, m1, m2)
      m3[0, 0] shouldBe (0.5).plusOrMinus(1e-6)
      m3[1, 1] shouldBe (0.5).plusOrMinus(1e-6)
      m3[0, 1] shouldBe (0.0).plusOrMinus(1e-6)
      m3[1, 0] shouldBe (0.0).plusOrMinus(1e-6)
    }

    test("lerpMeasure") {
      val m1 = Meters.of(10.0)
      val m2 = Meters.of(20.0)
      val m3 = lerpMeasure(0.5, m1, m2)
      m3.magnitude shouldBe (15.0).plusOrMinus(1e-6)
    }

    test("splerp with points") {
      splerp(0.0, 0.0, 1.0, 2.0, 3.0) shouldBe (1.0).plusOrMinus(1e-6)
      splerp(1.0, 0.0, 1.0, 2.0, 3.0) shouldBe (2.0).plusOrMinus(1e-6)
      splerp(0.5, 0.0, 1.0, 2.0, 3.0) shouldBe (1.5).plusOrMinus(1e-6)
    }

    test("splerp with map") {
      val map = TreeMap<Double, Double>()
      map[0.0] = 0.0
      map[1.0] = 1.0
      map[2.0] = 0.5
      map[3.0] = 2.0

      splerp(map, 0.0) shouldBe 0.0
      splerp(map, 1.0) shouldBe 1.0
      splerp(map, 2.0) shouldBe 0.5
      splerp(map, 1.5) shouldBe splerp(0.5, 0.0, 1.0, 0.5, 2.0).plusOrMinus(1e-6)
    }

    test("bilerp") {
      val result = bilerp(
        x = 0.5, y = 0.5,
        x0 = 0.0, y0 = 0.0,
        x1 = 1.0, y1 = 1.0,
        q00 = 10.0, q10 = 20.0,
        q01 = 30.0, q11 = 40.0,
      )
      result shouldBe (25.0).plusOrMinus(1e-6)
    }
  })
