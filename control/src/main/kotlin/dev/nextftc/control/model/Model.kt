/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.model

import dev.nextftc.control.util.discretizeAB
import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.Nat
import dev.nextftc.linalg.Vector

interface Model<State : Nat, Input : Nat, Output : Nat> {
  fun derivative(state: Vector<State>, input: Vector<Input>): Vector<State>

  fun output(state: Vector<State>, input: Vector<Input>): Vector<Output>
}

@Suppress("PropertyName")
class LinearModel<State : Nat, Input : Nat, Output : Nat> @JvmOverloads constructor(
  val A: Matrix<State, State>,
  val B: Matrix<State, Input>,
  val C: Matrix<Output, State>,
  val D: Matrix<Output, Input>,
  val dt: Double = 0.05,
) : Model<State, Input, Output> {
  private val Ad: Matrix<State, State>
  private val Bd: Matrix<State, Input>

  init {
    val (Ad, Bd) = discretizeAB(A, B, dt)
    this.Ad = Ad
    this.Bd = Bd
  }

  override fun derivative(state: Vector<State>, input: Vector<Input>): Vector<State> =
    Vector(Ad * state + Bd * input)

  override fun output(state: Vector<State>, input: Vector<Input>): Vector<Output> =
    Vector(C * state + D * input)
}
