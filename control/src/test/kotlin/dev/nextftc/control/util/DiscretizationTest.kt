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
import dev.nextftc.linalg.Nat
import dev.nextftc.linalg.Vector
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Computes the matrix exponential e^A using a Taylor series approximation.
 * e^A = I + A + A²/2! + A³/3! + ...
 */
private fun <N : Nat> matrixExp(A: Matrix<N, N>, terms: Int = 20): Matrix<N, N> {
  var result = Matrix.identity(A.natRows)
  var term = Matrix.identity(A.natRows)

  for (k in 1..terms) {
    term = term * A * (1.0 / k)
    result += term
  }
  return result
}

/**
 * Simple RK4 integration for matrix-valued functions.
 * Integrates dx/dt = f(t, x) from t0 to t0 + dt.
 */
private fun <N : Nat> rk4Matrix(
  f: (Double, Matrix<N, N>) -> Matrix<N, N>,
  t0: Double,
  x0: Matrix<N, N>,
  dt: Double,
  steps: Int = 100,
): Matrix<N, N> {
  var t = t0
  var x = x0
  val h = dt / steps

  for (i in 0 until steps) {
    val k1 = f(t, x)
    val k2 = f(t + h / 2, x + k1 * (h / 2))
    val k3 = f(t + h / 2, x + k2 * (h / 2))
    val k4 = f(t + h, x + k3 * h)

    x += (k1 + k2 * 2.0 + k3 * 2.0 + k4) * (h / 6.0)
    t += h
  }

  return x
}

class DiscretizationTest :
  FunSpec({
    context("discretizeAB") {
      test("discretizes double integrator correctly") {
        // contA represents: dx/dt = [[0, 1], [0, 0]] * x
        // This is a double integrator: position derivative = velocity, velocity derivative = 0
        val contA = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(0.0, 0.0),
          ),
        )
        // contB represents: dx/dt += [[0], [1]] * u
        // Input affects acceleration (velocity derivative)
        val contB = Matrix.from(
          N2,
          dev.nextftc.linalg.N1,
          arrayOf(
            doubleArrayOf(0.0),
            doubleArrayOf(1.0),
          ),
        )

        val x0 = Vector.of(N2, 1.0, 1.0)
        val u = Vector.of(dev.nextftc.linalg.N1, 1.0)

        val (discA, discB) = discretizeAB(contA, contB, 1.0)

        val x1Discrete = discA * x0 + discB * u

        // We now have pos = vel = accel = 1, which should give us:
        // pos(1) = pos(0) + vel(0)*dt + 0.5*accel*dt² = 1 + 1*1 + 0.5*1*1 = 2.5
        // vel(1) = vel(0) + accel*dt = 1 + 1*1 = 2
        val x1Truth = Vector.of(
          N2,
          1.0 * x0[0] + 1.0 * x0[1] + 0.5 * u[0],
          0.0 * x0[0] + 1.0 * x0[1] + 1.0 * u[0],
        )

        x1Discrete shouldBe x1Truth
      }
    }

    //                                               T
    // Test that the discrete approximation of Q_d ≈ ∫ e^(Aτ) Q e^(Aᵀτ) dτ
    //                                               0
    context("discretizeAQ") {
      test("discretizes slow model correctly") {
        val contA = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(0.0, 0.0),
          ),
        )
        val contQ = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
          ),
        )

        val dt = 1.0

        //       T
        // Q_d = ∫ e^(Aτ) Q e^(Aᵀτ) dτ
        //       0
        val discQIntegrated = rk4Matrix(
          { t, _ ->
            val expAt = matrixExp(contA * t)
            val expAtT = matrixExp(contA.transpose * t)
            expAt * contQ * expAtT
          },
          0.0,
          Matrix.zero(N2, N2),
          dt,
        )

        val (_, discQ) = discretizeAQ(contA, contQ, dt)

        val diff = (discQIntegrated - discQ).norm
        diff shouldBeLessThan 1e-10
      }

      //                                               T
      // Test that the discrete approximation of Q_d ≈ ∫ e^(Aτ) Q e^(Aᵀτ) dτ
      //                                               0
      test("discretizes fast model correctly") {
        val contA = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(0.0, -1406.29),
          ),
        )
        val contQ = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(0.0025, 0.0),
            doubleArrayOf(0.0, 1.0),
          ),
        )

        val dt = 0.005

        //       T
        // Q_d = ∫ e^(Aτ) Q e^(Aᵀτ) dτ
        //       0
        val discQIntegrated = rk4Matrix(
          { t, _ ->
            val expAt = matrixExp(contA * t)
            val expAtT = matrixExp(contA.transpose * t)
            expAt * contQ * expAtT
          },
          0.0,
          Matrix.zero(N2, N2),
          dt,
        )

        val (_, discQ) = discretizeAQ(contA, contQ, dt)

        val diff = (discQIntegrated - discQ).norm
        diff shouldBeLessThan 1e-3
      }
    }
  })
