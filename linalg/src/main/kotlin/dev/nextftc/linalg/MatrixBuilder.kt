/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:Suppress("ktlint:standard:max-line-length")

package dev.nextftc.linalg

class MatrixBuilder<R : Nat, C : Nat> internal constructor(val natRows: R, val natCols: C) {
  private val rows = mutableListOf<DoubleArray>()

  fun row(vararg elements: Double) = apply {
    require(elements.size == natCols.num)
    rows.add(elements)
  }

  fun build(): Matrix<R, C> {
    check(rows.size == natRows.num)
    return Matrix(rows.toTypedArray())
  }
}

/**
 * Builds a matrix with the given dimensions using a DSL-style initializer.
 *
 * Example:
 * ```
 * val matrix = buildMatrix(2, 3) {
 *     row(1.0, 2.0, 3.0)
 *     row(4.0, 5.0, 6.0)
 * }
 * ```
 *
 * @param natRows number of rows
 * @param natCols number of columns
 * @param initializer a builder lambda; each `row` call must provide exactly `natCols` elements
 * @return the constructed Matrix<R, C>
 * @throws IllegalArgumentException if a row has a different number of elements than `natCols`
 * @throws IllegalStateException if the number of rows added is not equal to `natRows`
 */
fun <R : Nat, C : Nat> buildMatrix(
  natRows: R,
  natCols: C,
  initializer: MatrixBuilder<R, C>.() -> Unit,
): Matrix<R, C> {
  val builder = MatrixBuilder<R, C>(natRows, natCols)
  builder.initializer()
  return builder.build()
}

// Fixed-size matrixOf overloads (square matrices) accepting N rows as Vector<C> and returning Matrix<N, C>

/** Creates a 1xC matrix from a single row vector. */
fun <C : Nat> matrixOf(row0: Vector<C>): Matrix<N1, C> {
  val cNat = row0.dimNat
  val data = Array(1) { DoubleArray(cNat.num) { c -> row0[c] } }
  return Matrix.from(N1, cNat, data)
}

/** Creates a 2xC matrix from two row vectors. */
fun <C : Nat> matrixOf(row0: Vector<C>, row1: Vector<C>): Matrix<N2, C> {
  val cNat = row0.dimNat
  require(row1.dimNat == cNat) { "All rows must have the same dimension" }
  val data = Array(2) { r ->
    val v = if (r == 0) row0 else row1
    DoubleArray(cNat.num) { c -> v[c] }
  }
  return Matrix.from(N2, cNat, data)
}

/** Creates a 3xC matrix from three row vectors. */
fun <C : Nat> matrixOf(row0: Vector<C>, row1: Vector<C>, row2: Vector<C>): Matrix<N3, C> {
  val cNat = row0.dimNat
  require(row1.dimNat == cNat && row2.dimNat == cNat) { "All rows must have the same dimension" }
  val data = Array(3) { r ->
    val v = when (r) {
      0 -> row0
      1 -> row1
      else -> row2
    }
    DoubleArray(cNat.num) { c -> v[c] }
  }
  return Matrix.from(N3, cNat, data)
}

/** Creates a 4xC matrix from four row vectors. */
fun <C : Nat> matrixOf(row0: Vector<C>, row1: Vector<C>, row2: Vector<C>, row3: Vector<C>): Matrix<N4, C> {
  val cNat = row0.dimNat
  require(row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat) {
    "All rows must have the same dimension"
  }
  val data = Array(4) { r ->
    val v = when (r) {
      0 -> row0
      1 -> row1
      2 -> row2
      else -> row3
    }
    DoubleArray(cNat.num) { c -> v[c] }
  }
  return Matrix.from(N4, cNat, data)
}

/** Creates a 5xC matrix from five row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
): Matrix<N5, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat,
  ) {
    "All rows must have the same dimension"
  }
  val rows = arrayOf(row0, row1, row2, row3, row4)
  val data = Array(5) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N5, cNat, data)
}

/** Creates a 6xC matrix from six row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
  row5: Vector<C>,
): Matrix<N6, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat &&
      row5.dimNat == cNat,
  ) { "All rows must have the same dimension" }
  val rows = arrayOf(row0, row1, row2, row3, row4, row5)
  val data = Array(6) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N6, cNat, data)
}

/** Creates a 7xC matrix from seven row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
  row5: Vector<C>,
  row6: Vector<C>,
): Matrix<N7, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat &&
      row5.dimNat == cNat && row6.dimNat == cNat,
  ) { "All rows must have the same dimension" }
  val rows = arrayOf(row0, row1, row2, row3, row4, row5, row6)
  val data = Array(7) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N7, cNat, data)
}

/** Creates an 8xC matrix from eight row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
  row5: Vector<C>,
  row6: Vector<C>,
  row7: Vector<C>,
): Matrix<N8, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat &&
      row5.dimNat == cNat && row6.dimNat == cNat && row7.dimNat == cNat,
  ) { "All rows must have the same dimension" }
  val rows = arrayOf(row0, row1, row2, row3, row4, row5, row6, row7)
  val data = Array(8) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N8, cNat, data)
}

/** Creates a 9xC matrix from nine row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
  row5: Vector<C>,
  row6: Vector<C>,
  row7: Vector<C>,
  row8: Vector<C>,
): Matrix<N9, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat &&
      row5.dimNat == cNat && row6.dimNat == cNat && row7.dimNat == cNat && row8.dimNat == cNat,
  ) { "All rows must have the same dimension" }
  val rows = arrayOf(row0, row1, row2, row3, row4, row5, row6, row7, row8)
  val data = Array(9) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N9, cNat, data)
}

/** Creates a 10xC matrix from ten row vectors. */
fun <C : Nat> matrixOf(
  row0: Vector<C>,
  row1: Vector<C>,
  row2: Vector<C>,
  row3: Vector<C>,
  row4: Vector<C>,
  row5: Vector<C>,
  row6: Vector<C>,
  row7: Vector<C>,
  row8: Vector<C>,
  row9: Vector<C>,
): Matrix<N10, C> {
  val cNat = row0.dimNat
  require(
    row1.dimNat == cNat && row2.dimNat == cNat && row3.dimNat == cNat && row4.dimNat == cNat &&
      row5.dimNat == cNat && row6.dimNat == cNat && row7.dimNat == cNat && row8.dimNat == cNat &&
      row9.dimNat == cNat,
  ) { "All rows must have the same dimension" }
  val rows = arrayOf(row0, row1, row2, row3, row4, row5, row6, row7, row8, row9)
  val data = Array(10) { r -> DoubleArray(cNat.num) { c -> rows[r][c] } }
  return Matrix.from(N10, cNat, data)
}
