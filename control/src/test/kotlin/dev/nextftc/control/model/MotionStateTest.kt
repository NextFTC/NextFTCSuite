/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.model

import dev.nextftc.units.Meters
import dev.nextftc.units.Seconds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class MotionStateTest :
  FunSpec({
    context("MotionState") {
      test("constructor with Units sets correct magnitudes") {
        val state = MotionState(Meters, 1.5, 2.5, 3.5)
        state.position.magnitude shouldBe 1.5
        state.velocity.magnitude shouldBe 2.5
        state.acceleration.magnitude shouldBe 3.5
        state.position.unit shouldBe Meters
        state.velocity.unit shouldBe Meters.per(Seconds)
        state.acceleration.unit shouldBe Meters.per(Seconds).per(Seconds)
      }

      test("copy allows updating components using doubles") {
        val state = MotionState(Meters, 1.0, 2.0, 3.0)
        val copied = state.copy(position = 4.0, acceleration = 6.0)
        copied.position.magnitude shouldBe 4.0
        copied.velocity.magnitude shouldBe 2.0
        copied.acceleration.magnitude shouldBe 6.0
      }

      test("toVector returns magnitudes") {
        val state = MotionState(Meters, 1.0, -2.0, 3.0)
        val vec = state.toVector()
        vec[0] shouldBe 1.0
        vec[1] shouldBe -2.0
        vec[2] shouldBe 3.0
      }

      test("unaryMinus negates components") {
        val state = MotionState(Meters, 1.0, -2.0, 3.0)
        val neg = -state
        neg.position.magnitude shouldBe -1.0
        neg.velocity.magnitude shouldBe 2.0
        neg.acceleration.magnitude shouldBe -3.0
      }

      test("plus adds components") {
        val state1 = MotionState(Meters, 1.0, 2.0, 3.0)
        val state2 = MotionState(Meters, 4.0, -1.0, 2.0)
        val sum = state1 + state2
        sum.position.magnitude shouldBe 5.0
        sum.velocity.magnitude shouldBe 1.0
        sum.acceleration.magnitude shouldBe 5.0
      }

      test("minus subtracts components") {
        val state1 = MotionState(Meters, 5.0, 2.0, 3.0)
        val state2 = MotionState(Meters, 4.0, -1.0, 2.0)
        val diff = state1 - state2
        diff.position.magnitude shouldBe 1.0
        diff.velocity.magnitude shouldBe 3.0
        diff.acceleration.magnitude shouldBe 1.0
      }

      test("times scalar multiplies components") {
        val state = MotionState(Meters, 1.0, -2.0, 3.0)
        val scaled = state * 2.5
        scaled.position.magnitude shouldBe (2.5 plusOrMinus 1e-6)
        scaled.velocity.magnitude shouldBe (-5.0 plusOrMinus 1e-6)
        scaled.acceleration.magnitude shouldBe (7.5 plusOrMinus 1e-6)
      }

      test("div scalar divides components") {
        val state = MotionState(Meters, 5.0, -2.0, 3.0)
        val div = state / 2.0
        div.position.magnitude shouldBe (2.5 plusOrMinus 1e-6)
        div.velocity.magnitude shouldBe (-1.0 plusOrMinus 1e-6)
        div.acceleration.magnitude shouldBe (1.5 plusOrMinus 1e-6)
      }
    }
  })
