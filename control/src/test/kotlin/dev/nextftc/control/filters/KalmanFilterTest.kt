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

package dev.nextftc.control.filters

import dev.nextftc.control.model.LinearModel
import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.N3
import dev.nextftc.linalg.N6
import dev.nextftc.linalg.Vector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.util.Random

class KalmanFilterTest :
  FunSpec({
    context("KalmanFilter") {
      val dt = 0.02

      val A = Matrix.from(
        N6,
        N6,
        arrayOf(
          doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        ),
      )

      val B = Matrix.from(
        N6,
        N3,
        arrayOf(
          doubleArrayOf(0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 0.0),
          doubleArrayOf(1.0, 0.0, 0.0),
          doubleArrayOf(0.0, 1.0, 0.0),
          doubleArrayOf(0.0, 0.0, 1.0),
        ),
      )

      val C = Matrix.from(
        N3,
        N6,
        arrayOf(
          doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0, 0.0),
          doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
        ),
      )

      val D = Matrix.zero(N3, N3)
      val plant = LinearModel(A, B, C, D, dt)

      test("Swerve stationary") {
        val stateStdDevs = Vector.of(N6, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1)
        val measurementStdDevs = Vector.of(N3, 1.0, 1.0, 1.0)

        val filter = KalmanFilter(plant, stateStdDevs, measurementStdDevs, dt)

        val u = Vector.zero(N3)
        val random = Random()

        var xHat = Vector.zero(N6)
        for (i in 0 until 100) {
          val y = Vector.of(
            N3,
            random.nextGaussian(),
            random.nextGaussian(),
            random.nextGaussian(),
          )
          filter.correct(u, y)
          xHat = filter.predict(u)
        }

        xHat[0] shouldBe (0.0 plusOrMinus 0.3)
        xHat[1] shouldBe (0.0 plusOrMinus 0.3)
      }
    }
  })
