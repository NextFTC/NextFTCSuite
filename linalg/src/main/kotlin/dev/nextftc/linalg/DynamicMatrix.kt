/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.linalg

import org.ejml.dense.row.decomposition.chol.CholeskyDecompositionInner_DDRM
import org.ejml.dense.row.decomposition.chol.CholeskyDecompositionLDL_DDRM
import org.ejml.dense.row.decomposition.lu.LUDecompositionAlt_DDRM
import org.ejml.dense.row.decomposition.qr.QRDecompositionHouseholder_DDRM
import org.ejml.simple.SimpleMatrix

/**
 * Represents a matrix of doubles with dynamic (runtime-checked) dimensions.
 * Internally represented as a SimpleMatrix from EJML.
 */
open class DynamicMatrix(internal val simple: SimpleMatrix) {
  /**
   * Constructor to create a [DynamicMatrix] from a 2D array.
   */
  constructor(data: Array<DoubleArray>) : this(SimpleMatrix(data))

  /**
   * Constructor to create a [DynamicMatrix] from a list of lists.
   */
  constructor(
    data: Collection<Collection<Double>>,
  ) : this(data.map { it.toDoubleArray() }.toTypedArray())

  companion object {
    /**
     * Creates a zero matrix with the given dimensions.
     */
    @JvmStatic
    fun zero(rows: Int, cols: Int) = DynamicMatrix(SimpleMatrix(rows, cols))

    /**
     * Creates a zero matrix with dimensions [size] by [size].
     */
    @JvmStatic
    fun zero(size: Int) = zero(size, size)

    /**
     * Creates an identity matrix with dimensions [size] by [size].
     */
    @JvmStatic
    fun identity(size: Int) = DynamicMatrix(SimpleMatrix.identity(size))

    /**
     * Creates a matrix with [data] along the diagonal
     * and all other elements set to 0.
     */
    @JvmStatic
    fun diagonal(vararg data: Double) = DynamicMatrix(SimpleMatrix.diag(*data))

    @JvmStatic
    fun diagonal(data: Collection<Double>) = diagonal(*data.toDoubleArray())

    /**
     * Creates a 1 by n matrix with [data] as its elements,
     * where n is [data].size.
     */
    @JvmStatic
    fun row(vararg data: Double) = DynamicMatrix(SimpleMatrix(1, data.size, true, data))

    /**
     * Creates a 1 by n matrix with [data] as its elements,
     * where n is [data].size.
     */
    @JvmStatic
    fun row(data: Collection<Double>) = row(*data.toDoubleArray())

    /**
     * Creates an n by 1 matrix with [data] as its elements,
     * where n is [data].size.
     */
    @JvmStatic
    fun column(vararg data: Double) = DynamicMatrix(SimpleMatrix(data.size, 1, false, data))

    /**
     * Creates an n by 1 matrix with [data] as its elements,
     * where n is [data].size.
     */
    @JvmStatic
    fun column(data: Collection<Double>) = column(*data.toDoubleArray())
  }

  /**
   * The number of columns in the matrix.
   */
  val numColumns = simple.numCols()

  /**
   * The number of rows in the matrix.
   */
  val numRows = simple.numRows()

  /**
   * The size of the matrix.
   *
   * @return a pair of integers representing the number of rows and columns respectively.
   */
  val size = numRows to numColumns

  /**
   * The transpose of this matrix.
   */
  @get:JvmName("transpose")
  val transpose: DynamicMatrix
    get() = DynamicMatrix(simple.transpose())

  /**
   * Returns a copy of this matrix.
   */
  open fun copy() = DynamicMatrix(simple.copy())

  /**
   * The inverse of this matrix.
   * The matrix must be square and invertible.
   * If the matrix is not square, use [pseudoInverse] instead.
   */
  @get:JvmName("inverse")
  val inverse: DynamicMatrix
    get() = DynamicMatrix(simple.invert())

  /**
   * The pseudo-inverse of this matrix.
   * The matrix must be square and invertible.
   */
  @get:JvmName("pseudoInverse")
  val pseudoInverse: DynamicMatrix
    get() = DynamicMatrix(simple.pseudoInverse())

  /**
   * Returns the Frobenius norm of this matrix.
   * The Frobenius norm is the square root of the sum of squares of all elements.
   */
  @get:JvmName("norm")
  val norm: Double
    get() = simple.normF()

  /**
   * Returns the matrix with all elements negated.
   * This is equivalent to multiplying the matrix by -1.
   */
  open operator fun unaryMinus() = DynamicMatrix(-simple)

  /**
   * Adds another matrix to this matrix.
   * The matrices must have the same dimensions.
   */
  operator fun plus(other: DynamicMatrix) = DynamicMatrix(this.simple + other.simple)

  /**
   * Subtracts another matrix from this matrix.
   * The matrices must have the same dimensions.
   */
  operator fun minus(other: DynamicMatrix) = DynamicMatrix(this.simple - other.simple)

  /**
   * Multiplies this matrix by another matrix.
   * The number of columns in this matrix must match the number of rows in the other matrix.
   */
  operator fun times(other: DynamicMatrix) = DynamicMatrix(this.simple * other.simple)

  /**
   * Multiplies this matrix by a scalar.
   */
  open operator fun times(scalar: Double) = DynamicMatrix(simple * scalar)

  /**
   * Multiplies this matrix by a scalar.
   */
  open operator fun times(scalar: Number) = DynamicMatrix(simple * scalar.toDouble())

  /** Divides this matrix by a scalar. */
  open operator fun div(scalar: Double) = times(1.0 / scalar)

  /** Divides this matrix by a scalar. */
  open operator fun div(scalar: Number) = times(1.0 / scalar.toDouble())

  /**
   * @usesMathJax
   *
   * Solves for X in the equation \(AX = B)\,
   * where A is this matrix and B is other.
   */
  fun solve(other: DynamicMatrix): DynamicMatrix = DynamicMatrix(this.simple.solve(other.simple))

  /**
   * Returns the element at the given indices.
   */
  operator fun get(i: Int, j: Int) = simple[i, j]

  /**
   * Sets the element at the given indices to the given [value].
   */
  operator fun set(i: Int, j: Int, value: Double) {
    simple[i, j] = value
  }

  /**
   * Returns the [n]th row of the matrix.
   */
  fun row(n: Int) = DynamicMatrix(simple.rows(n, n + 1))

  /**
   * Returns the [n]th column of the matrix.
   */
  fun column(n: Int) = DynamicMatrix(simple.cols(n, n + 1))

  /**
   * Returns the diagonal elements of this matrix.
   */
  fun diagonals() = DynamicMatrix(simple.diag())

  /**
   * Returns a submatrix of this matrix.
   * @param startRow First row to include in the submatrix, inclusive.
   * @param endRow Last row to include in the submatrix, exclusive.
   * @param startCol First column to include in the submatrix, inclusive.
   * @param endCol Last column to include in the submatrix, exclusive.
   */
  fun slice(startRow: Int, endRow: Int, startCol: Int, endCol: Int) = DynamicMatrix(
    simple.extractMatrix(startRow, endRow, startCol, endCol),
  )

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
  fun exp(): DynamicMatrix {
    require(numRows == numColumns) { "Matrix must be square" }

    val I = identity(numRows)
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

  /**
   * Returns the LLT (Cholesky) decomposition of this matrix.
   * Only works for symmetric, positive-definite matrices.
   * Provides in-place rank-1 update/downdate methods.
   */
  fun llt(): LLTDecomposition {
    val chol = CholeskyDecompositionInner_DDRM(true)
    val mat = simple.ddrm.copy()
    require(chol.decompose(mat)) { "Matrix is not symmetric positive-definite" }
    return LLTDecomposition(chol, mat)
  }

  /**
   * Returns the LDLT decomposition of this matrix.
   * Only works for symmetric matrices.
   */
  fun ldlt(): LDLTDecomposition {
    val ldlt = CholeskyDecompositionLDL_DDRM()
    val mat = simple.ddrm.copy()
    require(ldlt.decompose(mat)) { "Matrix is not positive definite" }
    return LDLTDecomposition(ldlt, mat)
  }

  /**
   * Returns the LU decomposition of this matrix.
   */
  fun lu(): LUDecomposition {
    val lu = LUDecompositionAlt_DDRM()
    val mat = simple.ddrm.copy()
    require(lu.decompose(mat)) { "Matrix is singular or not square" }
    return LUDecomposition(lu, mat)
  }

  /**
   * Returns the QR decomposition of this matrix.
   */
  fun qr(): QRDecomposition {
    val qr = QRDecompositionHouseholder_DDRM()
    val mat = simple.ddrm.copy()
    require(qr.decompose(mat)) { "QR decomposition failed" }
    return QRDecomposition(qr, mat)
  }

  override fun toString(): String = buildString {
    append("Matrix(${numRows}x$numColumns):\n")
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
    return other is DynamicMatrix && this.simple.isIdentical(other.simple, 1e-6)
  }

  override fun hashCode(): Int = simple.hashCode()

  /**
   * Returns a [Matrix] with the same dimensions as this matrix.
   * The dimensions are checked at runtime.
   */
  fun <R : Nat, C : Nat> toSizedMatrix(rows: R, cols: C): Matrix<R, C> {
    require(numRows == rows.num) { "Matrix has $numRows rows, expected ${rows.num}" }
    require(numColumns == cols.num) { "Matrix has $numColumns columns, expected ${cols.num}" }
    return Matrix(simple, rows, cols)
  }
}
