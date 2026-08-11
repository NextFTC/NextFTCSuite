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
import dev.nextftc.linalg.Nat
import dev.nextftc.linalg.Vector
import kotlin.math.pow

internal fun <N : Nat> makeBrysonMatrix(tolerances: Vector<N>): Matrix<N, N> {
  val matrix = Matrix.zero(tolerances.natRows, tolerances.natRows)

  for (i in 0 until tolerances.numRows) {
    if (tolerances[i].isInfinite()) {
      matrix[i, i] = 0.0
    } else {
      matrix[i, i] = 1.0 / (tolerances[i].pow(2))
    }
  }

  return matrix
}

internal fun <N : Nat> makeCovarianceMatrix(tolerances: Vector<N>): Matrix<N, N> {
  val matrix = Matrix.zero(tolerances.natRows, tolerances.natRows)

  for (i in 0 until tolerances.numRows) {
    matrix[i, i] = tolerances[i].pow(2)
  }

  return matrix
}

/**
 * Solves the Discrete-Time Algebraic Riccati Equation (DARE) using iterative method.
 * P = A'PA - (A'PB)(R + B'PB)⁻¹(B'PA) + Q
 *
 * @param maxIter the maximum number of iterations to attempt before giving up. Must be positive;
 *   a system that hasn't converged within this many iterations is treated as non-convergent
 *   rather than iterated on indefinitely.
 * @throws IllegalStateException if the iteration doesn't converge within [maxIter] iterations
 *
 * @author Tyler Veness (C++ implementation)
 * @author Zach Harel (Kotlin implementation)
 */
internal fun <States : Nat, Inputs : Nat> solveDARE(
  Ad: Matrix<States, States>,
  Bd: Matrix<States, Inputs>,
  Q: Matrix<States, States>,
  R: Matrix<Inputs, Inputs>,
  maxIter: Int = 1000,
  epsilon: Double = 1e-6,
): Matrix<States, States> {
  require(maxIter > 0) { "maxIter must be positive, got $maxIter" }

  // Initialize matrices
  var A_K = Ad.copy()

  // Calculate G_k = B * R^-1 * B^T using Cholesky decomposition
  // Equivalent to B * R.llt().solve(B.transpose())
  var G_K = Bd * R.solve(Bd.transpose)

  var H_K1 = Q.copy()
  var H_K: Matrix<States, States>

  var i = 0
  var converged: Boolean

  do {
    H_K = H_K1.copy()

    val W = Matrix.Companion.identity(H_K.natRows) + G_K * H_K

    val v1 = W.solve(A_K)
    val v2 = W.solve(G_K.transpose).transpose

    G_K = (G_K + A_K * v2 * A_K.transpose)

    H_K1 += v1.transpose * H_K * A_K

    A_K *= v1

    converged = (H_K1 - H_K).norm <= epsilon * H_K1.norm
    i++
  } while (!converged && i < maxIter)

  check(converged) { "DARE solver did not converge within $maxIter iterations" }

  return H_K1
}
