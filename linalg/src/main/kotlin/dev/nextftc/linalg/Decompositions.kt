/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:Suppress("ktlint:standard:property-naming")

package dev.nextftc.linalg

import org.ejml.data.DMatrixRMaj
import org.ejml.dense.row.decomposition.chol.CholeskyDecompositionLDL_DDRM
import org.ejml.dense.row.decomposition.lu.LUDecompositionAlt_DDRM
import org.ejml.dense.row.decomposition.qr.QRDecompositionHouseholder_DDRM
import org.ejml.interfaces.decomposition.CholeskyDecomposition
import org.ejml.simple.SimpleMatrix

/** Data class for LLT (Cholesky) decomposition, with rank-1 update/downdate. */
data class LLTDecomposition internal constructor(
  private val chol: CholeskyDecomposition<DMatrixRMaj>,
  private var mat: DMatrixRMaj,
) {
  /** Lower-triangular matrix L such that mat = L*L^T */
  @get:JvmName("L")
  val L: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(chol.getT(null)))

  /** In-place rank-1 update: mat := mat + x*x^T */
  fun update(x: DoubleArray) {
    // x must be column vector of correct size
    // Use EJML's rank-1 update if available, else manual
    // Here, we re-decompose for simplicity
    for (i in 0..mat.numRows) {
      for (j in 0..mat.numCols) {
        mat[i, j] += x[i] * x[j]
      }
    }
    require(chol.decompose(mat))
  }

  /** In-place rank-1 downdate: mat := mat - x*x^T */
  fun downdate(x: DoubleArray) {
    for (i in 0..mat.numRows) {
      for (j in 0..mat.numCols) {
        mat[i, j] -= x[i] * x[j]
      }
    }
    require(chol.decompose(mat))
  }
}

/** Data class for LDLT decomposition. */
data class LDLTDecomposition internal constructor(
  private val ldlt: CholeskyDecompositionLDL_DDRM,
  private val mat: DMatrixRMaj,
) {
  @get:JvmName("L")
  val L: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(ldlt.getL(null)))

  @get:JvmName("D")
  val D: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(ldlt.getD(null)))
}

/** Data class for LU decomposition. */
data class LUDecomposition internal constructor(
  private val lu: LUDecompositionAlt_DDRM,
  private val mat: DMatrixRMaj,
) {
  @get:JvmName("L")
  val L: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(lu.getLower(null)))

  @get:JvmName("U")
  val U: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(lu.getUpper(null)))

  @get:JvmName("P")
  val P: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(lu.getRowPivot(null)))
}

/** Data class for QR decomposition. */
data class QRDecomposition internal constructor(
  private val qr: QRDecompositionHouseholder_DDRM,
  private val mat: DMatrixRMaj,
) {
  @get:JvmName("Q")
  val Q: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(qr.getQ(null, false)))

  @get:JvmName("R")
  val R: DynamicMatrix get() = DynamicMatrix(SimpleMatrix(qr.getR(null, false)))
}
