/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Matrices")
@file:Suppress("UNUSED_PARAMETER")

package dev.nextftc.linalg

import dev.nextftc.linalg.Matrix.Companion.identity
import org.ejml.simple.SimpleMatrix

/**
 * A dimensionally type-safe matrix of doubles.
 *
 * The dimensions [R] (rows) and [C] (columns) are encoded at the type level using [Nat] types,
 * allowing the compiler to catch dimension mismatches at compile time.
 *
 * Example:
 * ```kotlin
 * val a: Matrix<N2, N3> = Matrix.zero(N2, N3)  // 2x3 matrix
 * val b: Matrix<N3, N4> = Matrix.zero(N3, N4)  // 3x4 matrix
 * val c: Matrix<N2, N4> = a * b                      // 2x4 matrix - compiles!
 * // val d = a * a  // Would not compile - N3 != N2
 * ```
 *
 * @param R The row dimension type
 * @param C The column dimension type
 */
open class Matrix<R : Nat, C : Nat> internal constructor(
  internal val simple: SimpleMatrix,
  internal val rowNat: R,
  internal val colNat: C,
) {
  @Suppress("UNCHECKED_CAST")
  constructor(data: Array<DoubleArray>) :
    this(SimpleMatrix(data), natOf(data.size) as R, natOf(data.first().size) as C)

  companion object {
    /**
     * Creates a zero matrix with dimensions specified by the [Nat] type parameters.
     */
    @JvmStatic
    fun <R : Nat, C : Nat> zero(rows: R, cols: C): Matrix<R, C> =
      Matrix(SimpleMatrix(rows.num, cols.num), rows, cols)

    /**
     * Creates a zero matrix with dimensions [rows] x [cols].
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <R : Nat, C : Nat> zero(rows: Int, cols: Int): Matrix<R, C> {
      val rNat = natOf(rows)
      val cNat = natOf(cols)
      return zero(rNat, cNat) as Matrix<R, C>
    }

    /**
     * Creates an identity matrix with dimensions [size] x [size].
     */
    @JvmStatic
    fun <N : Nat> identity(size: N): Matrix<N, N> = Matrix(SimpleMatrix.identity(size.num), size, size)

    /**
     * Creates a matrix with [data] along the diagonal.
     */
    @JvmStatic
    fun <N : Nat> diagonal(size: N, vararg data: Double): Matrix<N, N> {
      require(data.size == size.num) { "Data size must match dimension" }
      return Matrix(SimpleMatrix.diag(*data), size, size)
    }

    /**
     * Creates a row vector (1 x C matrix).
     */
    @JvmStatic
    fun <C : Nat> row(cols: C, vararg data: Double): Matrix<N1, C> {
      require(data.size == cols.num) { "Data size must match column dimension" }
      return Matrix(SimpleMatrix(1, data.size, true, data), N1, cols)
    }

    /**
     * Creates a column vector (R x 1 matrix).
     */
    @JvmStatic
    fun <R : Nat> column(rows: R, vararg data: Double): Matrix<R, N1> {
      require(data.size == rows.num) { "Data size must match row dimension" }
      return Matrix(SimpleMatrix(data.size, 1, false, data), rows, N1)
    }

    /**
     * Creates a matrix from a 2D array with specified dimensions.
     */
    @JvmStatic
    fun <R : Nat, C : Nat> from(rows: R, cols: C, data: Array<DoubleArray>): Matrix<R, C> {
      require(data.size == rows.num) { "Row count must match row dimension" }
      require(data.all { it.size == cols.num }) { "All rows must have column dimension size" }
      return Matrix(SimpleMatrix(data), rows, cols)
    }
  }

  /** The number of rows in the matrix. */
  @JvmField
  val numRows: Int = simple.numRows()

  /** Natural number representing the number of rows. */
  @JvmField
  val natRows: R = rowNat

  /** The number of columns in the matrix. */
  @JvmField
  val numColumns: Int = simple.numCols()

  /**
   * Natural number representing the number of columns.
   */
  @JvmField
  val natColumns: C = colNat

  /** The size of the matrix as (rows, columns). */
  @JvmField
  val size: Pair<Int, Int> = numRows to numColumns

  /** The transpose of this matrix, with swapped dimension types. */
  @get:JvmName("transpose")
  val transpose: Matrix<C, R>
    get() = Matrix(simple.transpose(), colNat, rowNat)

  /** Returns a copy of this matrix. */
  open fun copy(): Matrix<R, C> = Matrix(simple.copy(), rowNat, colNat)

  /** The inverse of this matrix. Only valid for square matrices. */
  @get:JvmName("inverse")
  val inverse: Matrix<R, C>
    get() = Matrix(simple.invert(), rowNat, colNat)

  /** The pseudo-inverse of this matrix. */
  @get:JvmName("pseudoInverse")
  val pseudoInverse: Matrix<C, R>
    get() = Matrix(simple.pseudoInverse(), colNat, rowNat)

  /** The Frobenius norm of this matrix. */
  @get:JvmName("norm")
  val norm: Double
    get() = simple.normF()

  /** Negates all elements of this matrix. */
  open operator fun unaryMinus(): Matrix<R, C> = Matrix(simple.negative(), rowNat, colNat)

  /** Adds another matrix with the same dimensions. */
  operator fun plus(other: Matrix<R, C>): Matrix<R, C> = Matrix(simple + other.simple, rowNat, colNat)

  /** Subtracts another matrix with the same dimensions. */
  operator fun minus(other: Matrix<R, C>): Matrix<R, C> = Matrix(simple - other.simple, rowNat, colNat)

  /**
   * Multiplies this matrix by another matrix.
   * The inner dimensions must match: (R x C) * (C x K) = (R x K)
   */
  operator fun <K : Nat> times(other: Matrix<C, K>): Matrix<R, K> =
    Matrix(simple.mult(other.simple), rowNat, other.colNat)

  /**
   * Multiplies this matrix by a vector.
   * The vector's length must be equal to [C].
   */
  operator fun times(other: Vector<C>): Vector<R> = Vector(Matrix(simple.mult(other.simple), rowNat, N1))

  /** Multiplies this matrix by a scalar. */
  open operator fun times(scalar: Double): Matrix<R, C> = Matrix(simple.scale(scalar), rowNat, colNat)

  /** Multiplies this matrix by a scalar. */
  open operator fun times(scalar: Number): Matrix<R, C> = times(scalar.toDouble())

  /** Divides this matrix by a scalar. */
  open operator fun div(scalar: Double) = times(1.0 / scalar)

  /** Divides this matrix by a scalar. */
  open operator fun div(scalar: Number) = times(1.0 / scalar.toDouble())

  /**
   * Solves for X in the equation AX = B,
   * where A is this matrix and B is [other].
   */
  fun <K : Nat> solve(other: Matrix<R, K>): Matrix<C, K> =
    Matrix(simple.solve(other.simple), colNat, other.colNat)

  /** Returns the element at the given indices. */
  operator fun get(i: Int, j: Int): Double = simple[i, j]

  /** Sets the element at the given indices. */
  operator fun set(i: Int, j: Int, value: Double) {
    simple[i, j] = value
  }

  /** Converts to a [DynamicMatrix]. */
  fun toDynamicMatrix(): DynamicMatrix = DynamicMatrix(simple)

  override fun toString(): String = buildString {
    append("SizedMatrix<$numRows, $numColumns>:\n")
    for (i in 0 until numRows) {
      append("[ ")
      for (j in 0 until numColumns) {
        append("%10.4f".format(simple[i, j]))
        if (j < numColumns - 1) append(", ")
      }
      append(" ]\n")
    }
  }.trimEnd()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return other is Matrix<*, *> && this.simple.isIdentical(other.simple, 1e-6)
  }

  override fun hashCode(): Int = simple.hashCode()
}

/** Scalar multiplication from the left. */
operator fun <R : Nat, C : Nat> Double.times(matrix: Matrix<R, C>): Matrix<R, C> = matrix * this

/** Scalar multiplication from the left. */
operator fun <R : Nat, C : Nat> Int.times(matrix: Matrix<R, C>): Matrix<R, C> = matrix * this

/**
 * Computes the matrix exponential of this matrix,
 * using the Padé approximant.
 *
 * Uses the formula:
 * e^A ≈ (1 + A/2 + A²/9 + A³/72 + A⁴/1008 + A⁵/30240) / (1 - A/2 + A²/9 - A³/72 + A⁴/1008 - A⁵/30240)
 *
 * @return The matrix exponential of this matrix.
 */
@Suppress("ktlint:standard:property-naming")
fun <N : Nat> Matrix<N, N>.exp(): Matrix<N, N> {
  val I = identity(natRows)
  val A2 = this * this
  val A3 = A2 * this
  val A4 = A3 * this
  val A5 = A4 * this

  val numerator =
    I + this * 0.5 + A2 * (1.0 / 9.0) + A3 * (1.0 / 72.0) + A4 * (1.0 / 1008.0) +
      A5 * (1.0 / 30240.0)
  val denominator =
    I - this * 0.5 + A2 * (1.0 / 9.0) - A3 * (1.0 / 72.0) + A4 * (1.0 / 1008.0) -
      A5 * (1.0 / 30240.0)

  return denominator.solve(numerator)
}
