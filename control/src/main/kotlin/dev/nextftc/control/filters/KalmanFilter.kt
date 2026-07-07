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
import dev.nextftc.control.util.discretizeAQ
import dev.nextftc.control.util.discretizeR
import dev.nextftc.control.util.makeCovarianceMatrix
import dev.nextftc.control.util.solveDARE
import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.Nat
import dev.nextftc.linalg.Vector

/**
 * A Kalman filter for linear state estimation.
 *
 * The Kalman filter is an optimal state estimator for linear systems with Gaussian noise.
 * It combines predictions from a system model with noisy measurements to produce an
 * optimal estimate of the system state.
 *
 * The filter operates on a linear system of the form:
 * ```
 * x(k+1) = A * x(k) + B * u(k) + w(k)    (state equation)
 * y(k)   = C * x(k) + D * u(k) + v(k)    (measurement equation)
 * ```
 * where:
 * - `x` is the state vector
 * - `u` is the input vector
 * - `y` is the measurement vector
 * - `w` is process noise with covariance Q
 * - `v` is measurement noise with covariance R
 *
 * @param States The number of states in the system
 * @param Inputs The number of inputs to the system
 * @param Outputs The number of outputs (measurements) from the system
 * @param plant The linear system model describing the plant dynamics
 * @param stateStdDevs Standard deviations of the state model (process noise)
 * @param measurementStdDevs Standard deviations of the measurements (measurement noise)
 * @param dt The discretization timestep in seconds (default 0.05s / 50ms)
 */
class KalmanFilter<States : Nat, Inputs : Nat, Outputs : Nat> @JvmOverloads constructor(
  val plant: LinearModel<States, Inputs, Outputs>,
  val stateStdDevs: Vector<States>,
  val measurementStdDevs: Vector<Outputs>,
  dt: Double = 0.05,
) {
  private val A: Matrix<States, States>
  private val Q: Matrix<States, States>
  private val R: Matrix<Outputs, Outputs>
  private val initialP: Matrix<States, States>

  private var P: Matrix<States, States>

  private var Xhat = Vector.zero(plant.A.natRows)

  init {
    require(dt > 0) { "Time step (dt) must be positive" }

    val Qcont = makeCovarianceMatrix(stateStdDevs)
    val Rcont = makeCovarianceMatrix(measurementStdDevs)

    val AQ = discretizeAQ(plant.A, Qcont, dt)
    A = AQ.first
    Q = AQ.second

    R = discretizeR(Rcont, dt)

    initialP = solveDARE(A.transpose, plant.C.transpose, Q, R)
    P = initialP
  }

  /**
   * Resets the filter to its initial state.
   *
   * Sets the state estimate to zero and resets the error covariance matrix
   * to its initial value computed from the DARE solution.
   */
  fun reset() {
    Xhat = Vector.zero(plant.A.natRows)
    P = initialP
  }

  /**
   * Performs the prediction step of the Kalman filter.
   *
   * This propagates the state estimate forward in time using the system model:
   * ```
   * x̂(k+1|k) = A * x̂(k|k) + B * u(k)
   * P(k+1|k) = A * P(k|k) * Aᵀ + Q
   * ```
   *
   * @param inputs The current input vector u(k)
   * @return The predicted state estimate x̂(k+1|k)
   */
  fun predict(inputs: Vector<Inputs>): Vector<States> {
    Xhat = plant.derivative(Xhat, inputs)

    P = A * P * A.transpose + Q

    return Xhat
  }

  /**
   * Performs the correction (update) step of the Kalman filter.
   *
   * This incorporates a new measurement to refine the state estimate using
   * the Joseph form of the covariance update for numerical stability:
   * ```
   * S = C * P * Cᵀ + R                           (innovation covariance)
   * K = P * Cᵀ * S⁻¹                             (Kalman gain)
   * x̂(k|k) = x̂(k|k-1) + K * (y - D * u)         (state update)
   * P(k|k) = (I - K*C) * P * (I - K*C)ᵀ + K*R*Kᵀ (covariance update)
   * ```
   *
   * @param inputs The current input vector u(k)
   * @param outputs The measurement vector y(k)
   * @return The corrected state estimate x̂(k|k)
   */
  fun correct(inputs: Vector<Inputs>, outputs: Vector<Outputs>): Vector<States> {
    val S = plant.C * P * plant.C.transpose + R

    val K = S.solve(plant.C * P).transpose

    Xhat += Vector(K * (outputs - (plant.D * inputs)))

    // (I−Kₖ₊₁C)Pₖ₊₁⁻(I−Kₖ₊₁C)ᵀ + Kₖ₊₁RKₖ₊₁ᵀ
    P = (Matrix.identity(K.natRows) - K * plant.C) * P *
      (Matrix.identity(K.natRows) - K * plant.C).transpose +
      K * R * K.transpose

    return Xhat
  }
}
