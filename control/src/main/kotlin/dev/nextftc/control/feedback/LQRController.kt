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

import dev.nextftc.control.model.LinearModel
import dev.nextftc.control.util.discretizeAB
import dev.nextftc.control.util.makeBrysonMatrix
import dev.nextftc.control.util.solveDARE
import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.N1
import dev.nextftc.linalg.Nat
import dev.nextftc.linalg.Vector

/**
 * A Linear Quadratic Regulator (LQR) for controlling a system modeled by state-space equations.
 *
 * LQR is a form of optimal control that finds the best control input to apply to a system
 * by minimizing a quadratic cost function. The cost function balances two competing goals:
 * 1.  **State Error**: How far the system is from its desired target state (penalized by the `Q` matrix).
 * 2.  **Control Effort**: How much energy or effort is used to control the system (penalized by the `R` matrix).
 *
 * The controller computes the optimal control input `u` using a simple state-feedback law: `u = -Kx`,
 * where `x` is the system's state error and `K` is the optimal gain matrix.
 **
 * Thank you to Tyler Veness and WPILib!
 *
 * @param A The state matrix.
 * @param B The input matrix.
 * @param Q The state cost matrix.
 * @param R The control cost matrix.
 * @param dt The time step for the discrete-time model (your loop time)
 *
 * @see <a href="https://en.wikipedia.org/wiki/Linear%E2%80%93quadratic_regulator">LQR on Wikipedia</a>
 * @see <a href="https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-intro.html#the-linear-quadratic-regulator">LQR in WPILib</a>
 */
class LQRController<States : Nat, Inputs : Nat, Outputs : Nat> @JvmOverloads constructor(
  A: Matrix<States, States>,
  B: Matrix<States, Inputs>,
  Q: Matrix<States, States>,
  R: Matrix<Inputs, Inputs>,
  private val dt: Double = 0.05,
) {
  private val K: Matrix<Inputs, States>

  init {
    require(dt > 0) { "Time step (dt) must be positive" }
    val (Ad, Bd) = discretizeAB(A, B, dt)
    val (_, K) = computeLQRGain(Ad, Bd, Q, R)
    this.K = K
  }

  /**
   * Constructs a controller with the given coefficient matrices.
   *
   * @param A the state matrix
   * @param B the input matrix
   * @param Qelems the maximum state error for each state dimension
   * @param Relems the maximum control effort for each control input dimension
   * @param dt the time step for the discrete-time model (your loop time)
   */
  @JvmOverloads constructor(
    A: Matrix<States, States>,
    B: Matrix<States, Inputs>,
    Qelems: Vector<States>,
    Relems: Vector<Inputs>,
    dt: Double = 0.05,
  ) : this(A, B, makeBrysonMatrix(Qelems), makeBrysonMatrix(Relems), dt)

  /**
   * Constructs a controller with the given plant model and cost matrices.
   *
   * @param plant the plant model
   * @param Qelems the maximum state error for each state dimension
   * @param Relems the maximum control effort for each control input dimension
   * @param dt the time step for the discrete-time model (your loop time)
   */
  @JvmOverloads constructor(
    plant: LinearModel<States, Inputs, Outputs>,
    Qelems: Vector<States>,
    Relems: Vector<Inputs>,
    dt: Double = 0.05,
  ) : this(plant.A, plant.B, Qelems, Relems, dt)

  /**
   * Calculates the optimal control input to correct for the given state error.
   *
   * @param error The current state error of the system, represented as a Matrix.
   * @return The calculated optimal control input as a Matrix.
   * */
  fun update(error: Matrix<States, N1>): Matrix<Inputs, N1> = -K * error
}

/**
 * Computes the optimal gain matrix K using [dev.nextftc.control.util.solveDARE].
 *
 * @return Pair of DARE solution X and K.
 */
internal fun <States : Nat, Inputs : Nat> computeLQRGain(
  Ad: Matrix<States, States>,
  Bd: Matrix<States, Inputs>,
  Q: Matrix<States, States>,
  R: Matrix<Inputs, Inputs>,
  maxIter: Int = -1,
  epsilon: Double = 1e-6,
): Pair<Matrix<States, States>, Matrix<Inputs, States>> {
  val X = solveDARE(Ad, Bd, Q, R, maxIter, epsilon)

  val btx = Bd.transpose * X
  val btxb = btx * Bd
  val K = (R + btxb).inverse * btx * Ad

  return X to K
}
