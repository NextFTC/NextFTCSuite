/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.model

import dev.nextftc.linalg.matrixOf
import dev.nextftc.linalg.vectorOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class ModelTest :
  FunSpec({
    context("LinearModel") {
      test("computes derivative and output correctly") {
        // A = [[0, 1], [0, 0]]
        val a = matrixOf(
          vectorOf(0.0, 1.0),
          vectorOf(0.0, 0.0),
        )
        // B = [[0], [1]]
        val b = matrixOf(
          vectorOf(0.0),
          vectorOf(1.0),
        )
        // C = [[1, 0]]
        val c = matrixOf(
          vectorOf(1.0, 0.0),
        )
        // D = [[0]]
        val d = matrixOf(
          vectorOf(0.0),
        )

        val model = LinearModel(a, b, c, d, dt = 0.05)

        // Expected discretizeAB for A=[0 1; 0 0], B=[0; 1], dt=0.05:
        // Ad = I + A*dt + A^2*dt^2/2 + ... = [[1, 0.05], [0, 1]]
        // Bd = integral_0^dt e^{A tau} B dtau
        // e^{A tau} B = [[1, tau], [0, 1]] [[0], [1]] = [[tau], [1]]
        // integral = [[dt^2 / 2], [dt]] = [[0.00125], [0.05]]

        val state = vectorOf(2.0, 3.0)
        val input = vectorOf(4.0)

        val derivative = model.derivative(state, input)
        // Ad * state = [[1, 0.05], [0, 1]] * [2, 3] = [2.15, 3]
        // Bd * input = [[0.00125], [0.05]] * 4 = [0.005, 0.2]
        // Ad*state + Bd*input = [2.155, 3.2]
        derivative[0] shouldBe (2.155 plusOrMinus 1e-6)
        derivative[1] shouldBe (3.2 plusOrMinus 1e-6)

        val output = model.output(state, input)
        // C * state = [1, 0] * [2, 3] = [2]
        // D * input = [0] * [4] = [0]
        output[0] shouldBe (2.0 plusOrMinus 1e-6)
      }
    }
  })
