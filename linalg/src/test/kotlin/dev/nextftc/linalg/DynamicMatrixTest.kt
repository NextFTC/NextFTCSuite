/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.linalg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class DynamicMatrixTest :
  FunSpec({
    context("Construction") {
      test("zero matrix has all zeros") {
        val m = DynamicMatrix.zero(2, 3)
        m.numRows shouldBe 2
        m.numColumns shouldBe 3
        for (i in 0 until 2) {
          for (j in 0 until 3) {
            m[i, j] shouldBe 0.0
          }
        }
      }

      test("zero with single size creates square matrix") {
        val m = DynamicMatrix.zero(3)
        m.numRows shouldBe 3
        m.numColumns shouldBe 3
      }

      test("identity creates identity matrix") {
        val m = DynamicMatrix.identity(3)
        m.numRows shouldBe 3
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 1.0
        m[2, 2] shouldBe 1.0
        m[0, 1] shouldBe 0.0
        m[1, 0] shouldBe 0.0
      }

      test("diagonal creates diagonal matrix") {
        val m = DynamicMatrix.diagonal(1.0, 2.0, 3.0)
        m.numRows shouldBe 3
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 2.0
        m[2, 2] shouldBe 3.0
        m[0, 1] shouldBe 0.0
      }

      test("diagonal from collection") {
        val m = DynamicMatrix.diagonal(listOf(1.0, 2.0))
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 2.0
      }

      test("row creates row vector") {
        val m = DynamicMatrix.row(1.0, 2.0, 3.0)
        m.numRows shouldBe 1
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[0, 1] shouldBe 2.0
        m[0, 2] shouldBe 3.0
      }

      test("row from collection") {
        val m = DynamicMatrix.row(listOf(1.0, 2.0))
        m.numColumns shouldBe 2
      }

      test("column creates column vector") {
        val m = DynamicMatrix.column(1.0, 2.0, 3.0)
        m.numRows shouldBe 3
        m.numColumns shouldBe 1
        m[0, 0] shouldBe 1.0
        m[1, 0] shouldBe 2.0
        m[2, 0] shouldBe 3.0
      }

      test("column from collection") {
        val m = DynamicMatrix.column(listOf(1.0, 2.0))
        m.numRows shouldBe 2
      }

      test("2D array constructor") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        m.numRows shouldBe 2
        m.numColumns shouldBe 2
        m[0, 0] shouldBe 1.0
        m[0, 1] shouldBe 2.0
        m[1, 0] shouldBe 3.0
        m[1, 1] shouldBe 4.0
      }

      test("list of lists constructor") {
        val m = DynamicMatrix(
          listOf(
            listOf(1.0, 2.0),
            listOf(3.0, 4.0),
          ),
        )
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 4.0
      }
    }

    context("Properties") {
      test("size returns correct dimensions") {
        val m = DynamicMatrix.zero(2, 3)
        m.size shouldBe (2 to 3)
      }

      test("transpose swaps dimensions") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
          ),
        )
        val t = m.transpose
        t.numRows shouldBe 3
        t.numColumns shouldBe 2
        t[0, 0] shouldBe 1.0
        t[0, 1] shouldBe 4.0
        t[1, 0] shouldBe 2.0
        t[2, 0] shouldBe 3.0
      }

      test("inverse computes correctly") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(4.0, 7.0),
            doubleArrayOf(2.0, 6.0),
          ),
        )
        val inv = m.inverse
        val product = m * inv
        product[0, 0] shouldBe (1.0 plusOrMinus 1e-9)
        product[1, 1] shouldBe (1.0 plusOrMinus 1e-9)
        product[0, 1] shouldBe (0.0 plusOrMinus 1e-9)
        product[1, 0] shouldBe (0.0 plusOrMinus 1e-9)
      }

      test("norm computes Frobenius norm") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        // Frobenius norm = sqrt(1 + 4 + 9 + 16) = sqrt(30)
        m.norm shouldBe (kotlin.math.sqrt(30.0) plusOrMinus 1e-9)
      }
    }

    context("Element access") {
      test("get returns correct element") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        m[0, 0] shouldBe 1.0
        m[0, 1] shouldBe 2.0
        m[1, 0] shouldBe 3.0
        m[1, 1] shouldBe 4.0
      }

      test("set modifies element") {
        val m = DynamicMatrix.zero(2, 2)
        m[0, 1] = 5.0
        m[0, 1] shouldBe 5.0
      }

      test("row extracts row") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
          ),
        )
        val r = m.row(1)
        r.numRows shouldBe 1
        r.numColumns shouldBe 3
        r[0, 0] shouldBe 4.0
        r[0, 1] shouldBe 5.0
        r[0, 2] shouldBe 6.0
      }

      test("column extracts column") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
            doubleArrayOf(5.0, 6.0),
          ),
        )
        val c = m.column(0)
        c.numRows shouldBe 3
        c.numColumns shouldBe 1
        c[0, 0] shouldBe 1.0
        c[1, 0] shouldBe 3.0
        c[2, 0] shouldBe 5.0
      }

      test("diagonals extracts diagonal") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
            doubleArrayOf(7.0, 8.0, 9.0),
          ),
        )
        val d = m.diagonals()
        d[0, 0] shouldBe 1.0
        d[1, 0] shouldBe 5.0
        d[2, 0] shouldBe 9.0
      }

      test("slice extracts submatrix") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
            doubleArrayOf(7.0, 8.0, 9.0),
          ),
        )
        val s = m.slice(0, 2, 1, 3)
        s.numRows shouldBe 2
        s.numColumns shouldBe 2
        s[0, 0] shouldBe 2.0
        s[0, 1] shouldBe 3.0
        s[1, 0] shouldBe 5.0
        s[1, 1] shouldBe 6.0
      }
    }

    context("Arithmetic operations") {
      test("unary minus negates all elements") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, -2.0),
            doubleArrayOf(-3.0, 4.0),
          ),
        )
        val neg = -m
        neg[0, 0] shouldBe -1.0
        neg[0, 1] shouldBe 2.0
        neg[1, 0] shouldBe 3.0
        neg[1, 1] shouldBe -4.0
      }

      test("plus adds matrices element-wise") {
        val a = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val b = DynamicMatrix(arrayOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0)))
        val c = a + b
        c[0, 0] shouldBe 6.0
        c[0, 1] shouldBe 8.0
        c[1, 0] shouldBe 10.0
        c[1, 1] shouldBe 12.0
      }

      test("minus subtracts matrices element-wise") {
        val a = DynamicMatrix(arrayOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0)))
        val b = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val c = a - b
        c[0, 0] shouldBe 4.0
        c[0, 1] shouldBe 4.0
        c[1, 0] shouldBe 4.0
        c[1, 1] shouldBe 4.0
      }

      test("times multiplies matrices") {
        val a = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        val b = DynamicMatrix(
          arrayOf(
            doubleArrayOf(5.0, 6.0),
            doubleArrayOf(7.0, 8.0),
          ),
        )
        val c = a * b
        c[0, 0] shouldBe (1.0 * 5.0 + 2.0 * 7.0)
        c[0, 1] shouldBe (1.0 * 6.0 + 2.0 * 8.0)
        c[1, 0] shouldBe (3.0 * 5.0 + 4.0 * 7.0)
        c[1, 1] shouldBe (3.0 * 6.0 + 4.0 * 8.0)
      }

      test("times scalar multiplies all elements") {
        val m = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val result = m * 2.0
        result[0, 0] shouldBe 2.0
        result[0, 1] shouldBe 4.0
        result[1, 0] shouldBe 6.0
        result[1, 1] shouldBe 8.0
      }

      test("times int scalar multiplies all elements") {
        val m = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val result = m * 2
        result[0, 0] shouldBe 2.0
        result[1, 1] shouldBe 8.0
      }
    }

    context("Solve") {
      test("solve computes solution to Ax = B") {
        val a = DynamicMatrix(
          arrayOf(
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(1.0, 3.0),
          ),
        )
        val b = DynamicMatrix.column(5.0, 10.0)
        val x = a.solve(b)
        // Verify: A * x ≈ b
        val result = a * x
        result[0, 0] shouldBe (5.0 plusOrMinus 1e-9)
        result[1, 0] shouldBe (10.0 plusOrMinus 1e-9)
      }
    }

    context("Decompositions") {
      test("LLT decomposition") {
        // Symmetric positive-definite matrix
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(4.0, 2.0),
            doubleArrayOf(2.0, 5.0),
          ),
        )
        val llt = m.llt()
        val l = llt.L
        // L * L^T should equal original matrix
        val reconstructed = l * l.transpose
        reconstructed[0, 0] shouldBe (4.0 plusOrMinus 1e-9)
        reconstructed[0, 1] shouldBe (2.0 plusOrMinus 1e-9)
        reconstructed[1, 0] shouldBe (2.0 plusOrMinus 1e-9)
        reconstructed[1, 1] shouldBe (5.0 plusOrMinus 1e-9)
      }

      test("LU decomposition") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(1.0, 3.0),
          ),
        )
        val lu = m.lu()
        // Just verify it doesn't throw
        val l = lu.L
        val u = lu.U
        l.numRows shouldBe 2
        u.numColumns shouldBe 2
      }

      test("QR decomposition") {
        val m = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
            doubleArrayOf(5.0, 6.0),
          ),
        )
        val qr = m.qr()
        val q = qr.Q
        val r = qr.R
        // Q should be orthogonal: Q^T * Q = I
        val qtq = q.transpose * q
        qtq[0, 0] shouldBe (1.0 plusOrMinus 1e-9)
        qtq[1, 1] shouldBe (1.0 plusOrMinus 1e-9)
      }
    }

    context("Utility") {
      test("copy creates independent copy") {
        val m = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val copy = m.copy()
        copy[0, 0] = 10.0
        m[0, 0] shouldBe 1.0
        copy[0, 0] shouldBe 10.0
      }

      test("equals compares matrices") {
        val a = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val b = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val c = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 5.0)))
        (a == b) shouldBe true
        (a == c) shouldBe false
      }

      test("toSizedMatrix converts to sized matrix") {
        val m = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        val sized = m.toSizedMatrix(N2, N2)
        sized[0, 0] shouldBe 1.0
        sized[1, 1] shouldBe 4.0
      }

      test("toSizedMatrix throws on dimension mismatch") {
        val m = DynamicMatrix(arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)))
        shouldThrow<IllegalArgumentException> {
          m.toSizedMatrix(N3, N2)
        }
      }
    }

    context("Matrix Exponential") {
      test("exp of identity matrix equals e*I") {
        val matrix = DynamicMatrix.identity(2)
        val result = matrix.exp()

        result[0, 0] shouldBe (Math.E plusOrMinus 1e-9)
        result[0, 1] shouldBe (0.0 plusOrMinus 1e-9)
        result[1, 0] shouldBe (0.0 plusOrMinus 1e-9)
        result[1, 1] shouldBe (Math.E plusOrMinus 1e-9)
      }

      test("exp of scaled matrix") {
        val matrix = DynamicMatrix(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        val result = (matrix * 0.01).exp()

        result[0, 0] shouldBe (1.01035625 plusOrMinus 1e-8)
        result[0, 1] shouldBe (0.02050912 plusOrMinus 1e-8)
        result[1, 0] shouldBe (0.03076368 plusOrMinus 1e-8)
        result[1, 1] shouldBe (1.04111993 plusOrMinus 1e-8)
      }
    }
  })
