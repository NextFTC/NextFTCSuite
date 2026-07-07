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

package dev.nextftc.control.util

import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.N1
import dev.nextftc.linalg.N2
import dev.nextftc.linalg.N3
import dev.nextftc.linalg.Vector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt

class StateSpaceUtilTest :
  FunSpec({
    test("makeBrysonMatrix (costMatrix)") {
      val tolerances = Vector.of(N3, 1.0, 2.0, 3.0)
      val bryson = makeBrysonMatrix(tolerances)
      bryson.numRows shouldBe 3
      bryson.numColumns shouldBe 3
      bryson[0, 0] shouldBe (1.0).plusOrMinus(1e-3)
      bryson[0, 1] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[0, 2] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[1, 0] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[1, 1] shouldBe (1.0 / 4.0).plusOrMinus(1e-3)
      bryson[1, 2] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[2, 0] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[2, 1] shouldBe (0.0).plusOrMinus(1e-3)
      bryson[2, 2] shouldBe (1.0 / 9.0).plusOrMinus(1e-3)
    }

    test("makeBrysonMatrix with infinity") {
      val tolerances = Vector.of(N2, 2.0, Double.POSITIVE_INFINITY)
      val bryson = makeBrysonMatrix(tolerances)
      bryson.numRows shouldBe 2
      bryson.numColumns shouldBe 2
      bryson[0, 0] shouldBe (1.0 / 4.0).plusOrMinus(1e-6)
      bryson[1, 1] shouldBe (0.0).plusOrMinus(1e-6)
    }

    test("makeCovarianceMatrix") {
      val tolerances = Vector.of(N3, 1.0, 2.0, 3.0)
      val cov = makeCovarianceMatrix(tolerances)
      cov.numRows shouldBe 3
      cov.numColumns shouldBe 3
      cov[0, 0] shouldBe (1.0).plusOrMinus(1e-3)
      cov[0, 1] shouldBe (0.0).plusOrMinus(1e-3)
      cov[0, 2] shouldBe (0.0).plusOrMinus(1e-3)
      cov[1, 0] shouldBe (0.0).plusOrMinus(1e-3)
      cov[1, 1] shouldBe (4.0).plusOrMinus(1e-3)
      cov[1, 2] shouldBe (0.0).plusOrMinus(1e-3)
      cov[2, 0] shouldBe (0.0).plusOrMinus(1e-3)
      cov[2, 1] shouldBe (0.0).plusOrMinus(1e-3)
      cov[2, 2] shouldBe (9.0).plusOrMinus(1e-3)
    }

    test("solveDARE") {
      val Ad = Matrix.from(N1, N1, arrayOf(doubleArrayOf(1.0)))
      val Bd = Matrix.from(N1, N1, arrayOf(doubleArrayOf(1.0)))
      val Q = Matrix.from(N1, N1, arrayOf(doubleArrayOf(1.0)))
      val R = Matrix.from(N1, N1, arrayOf(doubleArrayOf(1.0)))

      val P = solveDARE(Ad, Bd, Q, R)
      P[0, 0] shouldBe ((1.0 + sqrt(5.0)) / 2.0).plusOrMinus(1e-6)
    }
  })
