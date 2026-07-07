/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license file in the root directory of this project.
 *
 * Copyright (c) 2026 NextFTC Team
 * Portions of this file are original code or adaptations by the NextFTC Team.
 * Use of this source code is governed by an BSD-3-clause
 * license that can be found in the LICENSE.md file at the root of this repository or at
 * https://opensource.org/license/bsd-3-clause.
 */

@file:Suppress("ktlint:standard:property-naming")

package dev.nextftc.control.feedback

import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.N1
import dev.nextftc.linalg.N2
import dev.nextftc.linalg.Vector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class LQRControllerTest :
  FunSpec({
    context("LinearQuadraticRegulator") {
      test("Matrix overloads with single integrator") {
        val A = Matrix.from(N2, N2, arrayOf(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 0.0)))
        val B = Matrix.from(N2, N1, arrayOf(doubleArrayOf(0.0), doubleArrayOf(1.0)))
        val Q = Matrix.from(N2, N2, arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 1.0)))
        val R = Matrix.from(N1, N1, arrayOf(doubleArrayOf(1.0)))
        val dt = 0.02

        val lqr = LQRController<N2, N1, N1>(A, B, Q, R, dt)

        // We extract K via the update method (u = -K * error)
        val u1 = lqr.update(Vector.of(N2, 1.0, 0.0))
        val u2 = lqr.update(Vector.of(N2, 0.0, 1.0))

        (-u1[0, 0]) shouldBe (0.9828289133409422 plusOrMinus 1e-10)
        (-u2[0, 0]) shouldBe (1.7121946441864666 plusOrMinus 1e-10)
      }

      test("Matrix overloads with double integrator") {
        val Kv = 3.02
        val Ka = 0.642

        val A = Matrix.from(N2, N2, arrayOf(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, -Kv / Ka)))
        val B = Matrix.from(N2, N1, arrayOf(doubleArrayOf(0.0), doubleArrayOf(1.0 / Ka)))
        val Q = Matrix.from(N2, N2, arrayOf(doubleArrayOf(1.0, 0.0), doubleArrayOf(0.0, 0.2)))
        val R = Matrix.from(N1, N1, arrayOf(doubleArrayOf(0.25)))
        val dt = 0.02

        val lqr = LQRController<N2, N1, N1>(A, B, Q, R, dt)

        val u1 = lqr.update(Vector.of(N2, 1.0, 0.0))
        val u2 = lqr.update(Vector.of(N2, 0.0, 1.0))

        (-u1[0, 0]) shouldBe (1.984097715901652 plusOrMinus 1e-10)
        (-u2[0, 0]) shouldBe (0.5041176615634868 plusOrMinus 1e-10)
      }
    }
  })
