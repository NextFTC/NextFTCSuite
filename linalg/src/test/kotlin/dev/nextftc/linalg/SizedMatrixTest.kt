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

class SizedMatrixTest :
  FunSpec({
    context("Construction") {
      test("zero matrix has all zeros") {
        val m = Matrix.zero(N2, N3)
        m.numRows shouldBe 2
        m.numColumns shouldBe 3
        for (i in 0 until 2) {
          for (j in 0 until 3) {
            m[i, j] shouldBe 0.0
          }
        }
      }

      test("identity creates identity matrix") {
        val m = Matrix.identity(N3)
        m.numRows shouldBe 3
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 1.0
        m[2, 2] shouldBe 1.0
        m[0, 1] shouldBe 0.0
        m[1, 0] shouldBe 0.0
      }

      test("diagonal creates diagonal matrix") {
        val m = Matrix.diagonal(N3, 1.0, 2.0, 3.0)
        m.numRows shouldBe 3
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 2.0
        m[2, 2] shouldBe 3.0
        m[0, 1] shouldBe 0.0
      }

      test("diagonal throws on size mismatch") {
        shouldThrow<IllegalArgumentException> {
          Matrix.diagonal(N3, 1.0, 2.0)
        }
      }

      test("row creates row vector") {
        val m = Matrix.row(N3, 1.0, 2.0, 3.0)
        m.numRows shouldBe 1
        m.numColumns shouldBe 3
        m[0, 0] shouldBe 1.0
        m[0, 1] shouldBe 2.0
        m[0, 2] shouldBe 3.0
      }

      test("row throws on size mismatch") {
        shouldThrow<IllegalArgumentException> {
          Matrix.row(N3, 1.0, 2.0)
        }
      }

      test("column creates column vector") {
        val m = Matrix.column(N3, 1.0, 2.0, 3.0)
        m.numRows shouldBe 3
        m.numColumns shouldBe 1
        m[0, 0] shouldBe 1.0
        m[1, 0] shouldBe 2.0
        m[2, 0] shouldBe 3.0
      }

      test("column throws on size mismatch") {
        shouldThrow<IllegalArgumentException> {
          Matrix.column(N3, 1.0, 2.0)
        }
      }

      test("from creates matrix from 2D array") {
        val m = Matrix.from(
          N2,
          N2,
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

      test("from throws on row count mismatch") {
        shouldThrow<IllegalArgumentException> {
          Matrix.from(
            N3,
            N2,
            arrayOf(
              doubleArrayOf(1.0, 2.0),
              doubleArrayOf(3.0, 4.0),
            ),
          )
        }
      }

      test("from throws on column count mismatch") {
        shouldThrow<IllegalArgumentException> {
          Matrix.from(
            N2,
            N3,
            arrayOf(
              doubleArrayOf(1.0, 2.0),
              doubleArrayOf(3.0, 4.0),
            ),
          )
        }
      }

      test("2D array constructor") {
        val m = Matrix<N2, N2>(
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
          ),
        )
        m.numRows shouldBe 2
        m.numColumns shouldBe 2
        m[0, 0] shouldBe 1.0
        m[1, 1] shouldBe 4.0
      }
    }

    context("Properties") {
      test("size returns correct dimensions") {
        val m = Matrix.zero(N2, N3)
        m.size shouldBe (2 to 3)
      }

      test("transpose swaps dimensions and types") {
        val m = Matrix.from(
          N2,
          N3,
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
          ),
        )
        val t: Matrix<N3, N2> = m.transpose
        t.numRows shouldBe 3
        t.numColumns shouldBe 2
        t[0, 0] shouldBe 1.0
        t[0, 1] shouldBe 4.0
        t[1, 0] shouldBe 2.0
        t[2, 0] shouldBe 3.0
      }

      test("inverse computes correctly") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(4.0, 7.0),
            doubleArrayOf(2.0, 6.0),
          ),
        )
        val inv: Matrix<N2, N2> = m.inverse
        val product = m * inv
        product[0, 0] shouldBe (1.0 plusOrMinus 1e-9)
        product[1, 1] shouldBe (1.0 plusOrMinus 1e-9)
        product[0, 1] shouldBe (0.0 plusOrMinus 1e-9)
        product[1, 0] shouldBe (0.0 plusOrMinus 1e-9)
      }

      test("pseudoInverse has correct dimensions") {
        val m = Matrix.from(
          N3,
          N2,
          arrayOf(
            doubleArrayOf(1.0, 2.0),
            doubleArrayOf(3.0, 4.0),
            doubleArrayOf(5.0, 6.0),
          ),
        )
        val pinv: Matrix<N2, N3> = m.pseudoInverse
        pinv.numRows shouldBe 2
        pinv.numColumns shouldBe 3
      }

      test("norm computes Frobenius norm") {
        val m = Matrix.from(
          N2,
          N2,
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
        val m = Matrix.from(
          N2,
          N2,
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
        val m = Matrix.zero(N2, N2)
        m[0, 1] = 5.0
        m[0, 1] shouldBe 5.0
      }
    }

    context("Arithmetic operations") {
      test("unary minus negates all elements") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(1.0, -2.0),
            doubleArrayOf(-3.0, 4.0),
          ),
        )
        val neg: Matrix<N2, N2> = -m
        neg[0, 0] shouldBe -1.0
        neg[0, 1] shouldBe 2.0
        neg[1, 0] shouldBe 3.0
        neg[1, 1] shouldBe -4.0
      }

      test("plus adds matrices element-wise") {
        val a = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val b = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0)),
        )
        val c: Matrix<N2, N2> = a + b
        c[0, 0] shouldBe 6.0
        c[0, 1] shouldBe 8.0
        c[1, 0] shouldBe 10.0
        c[1, 1] shouldBe 12.0
      }

      test("minus subtracts matrices element-wise") {
        val a = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(5.0, 6.0), doubleArrayOf(7.0, 8.0)),
        )
        val b = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val c: Matrix<N2, N2> = a - b
        c[0, 0] shouldBe 4.0
        c[0, 1] shouldBe 4.0
        c[1, 0] shouldBe 4.0
        c[1, 1] shouldBe 4.0
      }

      test("times multiplies matrices with type-safe dimensions") {
        val a: Matrix<N2, N3> = Matrix.from(
          N2,
          N3,
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
          ),
        )
        val b: Matrix<N3, N2> = Matrix.from(
          N3,
          N2,
          arrayOf(
            doubleArrayOf(7.0, 8.0),
            doubleArrayOf(9.0, 10.0),
            doubleArrayOf(11.0, 12.0),
          ),
        )
        val c: Matrix<N2, N2> = a * b
        c.numRows shouldBe 2
        c.numColumns shouldBe 2
        c[0, 0] shouldBe (1.0 * 7.0 + 2.0 * 9.0 + 3.0 * 11.0)
        c[0, 1] shouldBe (1.0 * 8.0 + 2.0 * 10.0 + 3.0 * 12.0)
      }

      test("times scalar multiplies all elements") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val result: Matrix<N2, N2> = m * 2.0
        result[0, 0] shouldBe 2.0
        result[0, 1] shouldBe 4.0
        result[1, 0] shouldBe 6.0
        result[1, 1] shouldBe 8.0
      }

      test("times int scalar multiplies all elements") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val result: Matrix<N2, N2> = m * 2
        result[0, 0] shouldBe 2.0
        result[1, 1] shouldBe 8.0
      }

      test("scalar times matrix from left") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val result: Matrix<N2, N2> = 2.0 * m
        result[0, 0] shouldBe 2.0
        result[1, 1] shouldBe 8.0
      }

      test("int scalar times matrix from left") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val result: Matrix<N2, N2> = 2 * m
        result[0, 0] shouldBe 2.0
        result[1, 1] shouldBe 8.0
      }

      test("matrix times vector multiplies correctly") {
        val a: Matrix<N2, N3> = Matrix.from(
          N2,
          N3,
          arrayOf(
            doubleArrayOf(1.0, 2.0, 3.0),
            doubleArrayOf(4.0, 5.0, 6.0),
          ),
        )
        val v3 = vectorOf(7.0, 8.0, 9.0)
        val r = a * v3

        r.numRows shouldBe 2
        r.numColumns shouldBe 1
        r[0, 0] shouldBe (1.0 * 7.0 + 2.0 * 8.0 + 3.0 * 9.0)
        r[1, 0] shouldBe (4.0 * 7.0 + 5.0 * 8.0 + 6.0 * 9.0)
      }
    }

    context("Solve") {
      test("solve computes solution with type-safe dimensions") {
        val a: Matrix<N2, N2> = Matrix.from(
          N2,
          N2,
          arrayOf(
            doubleArrayOf(2.0, 1.0),
            doubleArrayOf(1.0, 3.0),
          ),
        )
        val b: Matrix<N2, N1> = Matrix.column(N2, 5.0, 10.0)
        val x: Matrix<N2, N1> = a.solve(b)
        // Verify: A * x ≈ b
        val result = a * x
        result[0, 0] shouldBe (5.0 plusOrMinus 1e-9)
        result[1, 0] shouldBe (10.0 plusOrMinus 1e-9)
      }
    }

    context("Type safety") {
      test("matrix multiplication produces correct result type") {
        val m2x3: Matrix<N2, N3> = Matrix.zero(N2, N3)
        val m3x4: Matrix<N3, N4> = Matrix.zero(N3, N4)
        val m2x4: Matrix<N2, N4> = m2x3 * m3x4
        m2x4.numRows shouldBe 2
        m2x4.numColumns shouldBe 4
      }

      test("transpose produces correct result type") {
        val m2x3: Matrix<N2, N3> = Matrix.zero(N2, N3)
        val m3x2: Matrix<N3, N2> = m2x3.transpose
        m3x2.numRows shouldBe 3
        m3x2.numColumns shouldBe 2
      }

      test("matrix vector multiplication produces correct result type") {
        val m2x3 = Matrix.zero(N2, N3)
        val v3 = vectorOf(0.0, 0.0, 0.0)
        val v2 = m2x3 * v3

        v2.numRows shouldBe 2
      }
    }

    context("Conversion") {
      test("toDynamicMatrix converts to dynamic matrix") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val dm = m.toDynamicMatrix()
        dm.numRows shouldBe 2
        dm.numColumns shouldBe 2
        dm[0, 0] shouldBe 1.0
        dm[1, 1] shouldBe 4.0
      }
    }

    context("Utility") {
      test("copy creates independent copy") {
        val m = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val copy = m.copy()
        copy[0, 0] = 10.0
        m[0, 0] shouldBe 1.0
        copy[0, 0] shouldBe 10.0
      }

      test("equals compares matrices") {
        val a = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val b = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0)),
        )
        val c = Matrix.from(
          N2,
          N2,
          arrayOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 5.0)),
        )
        (a == b) shouldBe true
        (a == c) shouldBe false
      }

      test("toString formats correctly") {
        val m = Matrix.identity(N2)
        m.toString().contains("SizedMatrix<2, 2>") shouldBe true
      }
    }

    context("Matrix Exponential") {
      test("exp of identity matrix equals e*I") {
        val matrix = Matrix.identity(N2)
        val result = matrix.exp()

        result[0, 0] shouldBe (Math.E plusOrMinus 1e-9)
        result[0, 1] shouldBe (0.0 plusOrMinus 1e-9)
        result[1, 0] shouldBe (0.0 plusOrMinus 1e-9)
        result[1, 1] shouldBe (Math.E plusOrMinus 1e-9)
      }

      test("exp of scaled matrix") {
        val matrix = Matrix.from(
          N2,
          N2,
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
