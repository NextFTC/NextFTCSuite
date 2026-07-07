/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI

class AnalogFeedbackTest :
  FunSpec({
    class Holder(voltage: Double) {
      val angle by AnalogFeedback { voltage }
    }

    test("zero voltage maps to zero angle") {
      Holder(0.0).angle shouldBe (0.0 plusOrMinus 1e-9)
    }

    test("full-scale voltage (3.3V) maps to a full turn") {
      Holder(3.3).angle shouldBe (2 * PI plusOrMinus 1e-9)
    }

    test("half-scale voltage maps to half a turn") {
      Holder(1.65).angle shouldBe (PI plusOrMinus 1e-9)
    }

    test("voltage is read fresh on every access") {
      var voltage = 0.0
      val holder = object {
        val angle by AnalogFeedback { voltage }
      }
      holder.angle shouldBe (0.0 plusOrMinus 1e-9)
      voltage = 3.3
      holder.angle shouldBe (2 * PI plusOrMinus 1e-9)
    }
  })
