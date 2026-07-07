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

class SizedVectorTest :
  FunSpec({
    context("Construction") {
      test("zero vector has all zeros") {
        val v = Vector.zero(N3)
        v.dimension shouldBe 3
        v[0] shouldBe 0.0
        v[1] shouldBe 0.0
        v[2] shouldBe 0.0
      }

      test("of vararg creates vector with given values") {
        val v = Vector.of(N3, 1.0, 2.0, 3.0)
        v.dimension shouldBe 3
        v[0] shouldBe 1.0
        v[1] shouldBe 2.0
        v[2] shouldBe 3.0
      }

      test("of collection creates vector") {
        val v = Vector.of(N3, listOf(1.0, 2.0, 3.0))
        v.dimension shouldBe 3
        v[0] shouldBe 1.0
        v[1] shouldBe 2.0
        v[2] shouldBe 3.0
      }

      test("of throws on size mismatch") {
        shouldThrow<IllegalArgumentException> {
          Vector.of(N3, 1.0, 2.0)
        }
      }

      test("makeVector creates 1D vector") {
        val v = vectorOf(5.0)
        v.dimension shouldBe 1
        v[0] shouldBe 5.0
      }

      test("makeVector creates 2D vector") {
        val v = vectorOf(1.0, 2.0)
        v.dimension shouldBe 2
        v[0] shouldBe 1.0
        v[1] shouldBe 2.0
      }

      test("makeVector creates 3D vector") {
        val v = vectorOf(1.0, 2.0, 3.0)
        v.dimension shouldBe 3
        v[0] shouldBe 1.0
        v[1] shouldBe 2.0
        v[2] shouldBe 3.0
      }

      test("makeVector creates higher dimensional vectors") {
        val v4 = vectorOf(1.0, 2.0, 3.0, 4.0)
        v4.dimension shouldBe 4

        val v5 = vectorOf(1.0, 2.0, 3.0, 4.0, 5.0)
        v5.dimension shouldBe 5

        val v10 = vectorOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
        v10.dimension shouldBe 10
      }
    }

    context("Element access") {
      test("get returns correct element") {
        val v = vectorOf(1.0, 2.0, 3.0)
        v[0] shouldBe 1.0
        v[1] shouldBe 2.0
        v[2] shouldBe 3.0
      }

      test("set modifies element") {
        val v = vectorOf(1.0, 2.0, 3.0)
        v[1] = 10.0
        v[1] shouldBe 10.0
      }
    }

    context("Arithmetic operations") {
      test("unary minus negates all elements") {
        val v = vectorOf(1.0, -2.0, 3.0)
        val neg = -v
        neg[0] shouldBe -1.0
        neg[1] shouldBe 2.0
        neg[2] shouldBe -3.0
      }

      test("plus adds vectors element-wise") {
        val a = vectorOf(1.0, 2.0, 3.0)
        val b = vectorOf(4.0, 5.0, 6.0)
        val c: Vector<N3> = a + b
        c[0] shouldBe 5.0
        c[1] shouldBe 7.0
        c[2] shouldBe 9.0
      }

      test("minus subtracts vectors element-wise") {
        val a = vectorOf(4.0, 5.0, 6.0)
        val b = vectorOf(1.0, 2.0, 3.0)
        val c: Vector<N3> = a - b
        c[0] shouldBe 3.0
        c[1] shouldBe 3.0
        c[2] shouldBe 3.0
      }

      test("times scalar multiplies all elements") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val result = v * 2.0
        result[0] shouldBe 2.0
        result[1] shouldBe 4.0
        result[2] shouldBe 6.0
      }

      test("times int scalar multiplies all elements") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val result = v * 2
        result[0] shouldBe 2.0
        result[1] shouldBe 4.0
        result[2] shouldBe 6.0
      }

      test("scalar times vector from left") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val result: Vector<N3> = 2.0 * v
        result[0] shouldBe 2.0
        result[1] shouldBe 4.0
        result[2] shouldBe 6.0
      }

      test("int scalar times vector from left") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val result: Vector<N3> = 2 * v
        result[0] shouldBe 2.0
        result[1] shouldBe 4.0
        result[2] shouldBe 6.0
      }
    }

    context("Vector operations") {
      test("dot product computes correctly") {
        val a = vectorOf(1.0, 2.0, 3.0)
        val b = vectorOf(4.0, 5.0, 6.0)
        (a dot b) shouldBe (1.0 * 4.0 + 2.0 * 5.0 + 3.0 * 6.0)
      }

      test("magnitude computes correctly") {
        val v = vectorOf(3.0, 4.0)
        v.magnitude shouldBe 5.0
      }

      test("normalized returns unit vector") {
        val v = vectorOf(3.0, 4.0)
        val norm = v.normalized()
        norm.magnitude shouldBe (1.0 plusOrMinus 1e-9)
        norm[0] shouldBe (0.6 plusOrMinus 1e-9)
        norm[1] shouldBe (0.8 plusOrMinus 1e-9)
      }
    }

    context("Type safety") {
      test("operations preserve dimension type") {
        val v3: Vector<N3> = vectorOf(1.0, 2.0, 3.0)
        val result: Vector<N3> = v3 + v3
        result.dimension shouldBe 3
      }
    }

    context("Conversion") {
      test("toDynamicVector converts to dynamic vector") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val dv = v.toDynamicVector()
        dv.dimension shouldBe 3
        dv[0] shouldBe 1.0
        dv[1] shouldBe 2.0
        dv[2] shouldBe 3.0
      }
    }

    context("Utility") {
      test("copy creates independent copy") {
        val v = vectorOf(1.0, 2.0, 3.0)
        val copy = v.copy()
        copy[0] = 10.0
        v[0] shouldBe 1.0
        copy[0] shouldBe 10.0
      }

      test("toString formats correctly") {
        val v = vectorOf(1.0, 2.0)
        v.toString() shouldBe "SizedVector<2>: [    1.0000,     2.0000]"
      }
    }
  })
