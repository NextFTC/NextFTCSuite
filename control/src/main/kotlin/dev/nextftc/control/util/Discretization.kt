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

import dev.nextftc.linalg.DynamicMatrix
import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.Nat

/**
 * Discretizes a continuous-time system (A, B) to a discrete-time system (Ad, Bd).
 * Uses the matrix exponential (approximated with a Taylor series).
 * Ad = e^(A*dt)
 * Bd = (∫ e^(Aτ) dτ from 0 to dt) * B ≈ (I*dt + A*dt²/2! + ...) * B
 */
internal fun <States : Nat, Inputs : Nat> discretizeAB(
  A: Matrix<States, States>,
  B: Matrix<States, Inputs>,
  dt: Double,
  taylorTerms: Int = 10,
): Pair<Matrix<States, States>, Matrix<States, Inputs>> {
  val n = A.numRows
  var Ad = Matrix.Companion.identity(A.natRows)
  var Bd_integral = Matrix.Companion.identity(B.natRows).times(dt) // Start with I*dt

  var APowerDt = A.times(dt)
  var dtPower = dt
  var factorial = 1.0

  // Taylor series for e^(A*dt) and its integral
  for (i in 1..taylorTerms) {
    // Ad term: (A*dt)^i / i!
    Ad = Ad.plus(APowerDt.times(1.0 / factorial))

    // Bd integral term: A^(i-1) * dt^(i+1) / (i+1)!
    dtPower *= dt
    factorial *= (i + 1)
    Bd_integral = Bd_integral.plus(APowerDt.times(dt / (i + 1)))

    // Prepare for next iteration
    APowerDt = APowerDt.times(A.times(dt))
    factorial *= (i + 1)
  }

  val Bd = Bd_integral.times(B)
  return Pair(Ad, Bd)
}

/**
 * Discretizes the given continuous A and Q matrices.
 *
 * @param States Nat representing the number of states.
 * @param A Continuous system matrix.
 * @param Q Continuous process noise covariance matrix.
 * @param dt Discretization timestep in seconds.
 * @return a pair representing the discrete system matrix and process noise covariance matrix.
*/
internal fun <States : Nat> discretizeAQ(
  A: Matrix<States, States>,
  Q: Matrix<States, States>,
  dt: Double,
): Pair<Matrix<States, States>, Matrix<States, States>> {
  val states = A.numRows

  // Make continuous Q symmetric if it isn't already
  val Qsym = (Q + Q.transpose) * 0.5

  // M = [−A  Q ]
  //     [ 0  Aᵀ]
  val M = DynamicMatrix.zero(2 * states, 2 * states)

  // Assign block (0, 0): -A
  for (i in 0 until states) {
    for (j in 0 until states) {
      M[i, j] = -A[i, j]
    }
  }

  // Assign block (0, states): Q
  for (i in 0 until states) {
    for (j in 0 until states) {
      M[i, j + states] = Qsym[i, j]
    }
  }

  // Block (states, 0) is already zero

  // Assign block (states, states): Aᵀ
  for (i in 0 until states) {
    for (j in 0 until states) {
      M[i + states, j + states] = A[j, i] // Transpose
    }
  }

  // ϕ = e^(M*dt)
  val phi = (M * dt).exp()

  // ϕ₁₂ = A_d⁻¹Q_d (block at row 0, col states)
  val phi12 = phi.slice(0, states, states, 2 * states)

  // ϕ₂₂ = A_dᵀ (block at row states, col states)
  val phi22 = phi.slice(states, 2 * states, states, 2 * states)

  // A_d = ϕ₂₂ᵀ
  val discA = phi22.transpose

  // Q_d = A_d * ϕ₁₂
  var discQ = discA * phi12

  // Make discrete Q symmetric if it isn't already
  discQ = (discQ + discQ.transpose) * 0.5

  // Convert back to sized matrices
  @Suppress("UNCHECKED_CAST")
  return Pair(
    discA.toSizedMatrix(A.natRows, A.natColumns),
    discQ.toSizedMatrix(Q.natRows, Q.natColumns),
  )
}

internal fun <Outputs : Nat> discretizeR(
  R: Matrix<Outputs, Outputs>,
  dt: Double,
): Matrix<Outputs, Outputs> = R / dt
