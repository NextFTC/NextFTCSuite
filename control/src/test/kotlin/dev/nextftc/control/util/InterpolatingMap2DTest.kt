/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class InterpolatingMap2DTest :
  FunSpec({
    context("InterpolatingMap2D construction") {
      test("creates empty bilinear map") {
        val map = InterpolatingMap2D.bilinear()
        shouldThrow<IllegalArgumentException> { map[0.0, 0.0] }
      }

      test("initializes with data") {
        val xKeys = listOf(0.0, 1.0)
        val yKeys = listOf(0.0, 1.0)
        val values = listOf(
          listOf(0.0, 1.0),
          listOf(2.0, 3.0),
        )
        val map = InterpolatingMap2D.bilinear(xKeys, yKeys, values)
        map[0.0, 0.0] shouldBe (0.0 plusOrMinus 0.001)
        map[1.0, 1.0] shouldBe (3.0 plusOrMinus 0.001)
      }

      test("rejects mismatched dimensions") {
        val xKeys = listOf(0.0, 1.0)
        val yKeys = listOf(0.0, 1.0, 2.0)
        val values = listOf(
          listOf(0.0, 1.0),
          listOf(2.0, 3.0),
        )
        shouldThrow<IllegalArgumentException> {
          InterpolatingMap2D.bilinear(xKeys, yKeys, values)
        }
      }

      test("rejects inconsistent row sizes") {
        val xKeys = listOf(0.0, 1.0)
        val yKeys = listOf(0.0, 1.0)
        val values = listOf(
          listOf(0.0, 1.0),
          listOf(2.0),
        )
        shouldThrow<IllegalArgumentException> {
          InterpolatingMap2D.bilinear(xKeys, yKeys, values)
        }
      }
    }

    context("InterpolatingMap2D exact matches") {
      test("returns exact value at grid point") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 10.0
        map[1.0, 1.0] = 20.0
        map[0.0, 0.0] shouldBe (10.0 plusOrMinus 0.001)
        map[1.0, 1.0] shouldBe (20.0 plusOrMinus 0.001)
      }

      test("returns exact value when both coordinates match") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 1.0
        map[1.0, 0.0] = 2.0
        map[0.0, 1.0] = 3.0
        map[1.0, 1.0] = 4.0
        map[0.0, 0.0] shouldBe (1.0 plusOrMinus 0.001)
        map[1.0, 0.0] shouldBe (2.0 plusOrMinus 0.001)
        map[0.0, 1.0] shouldBe (3.0 plusOrMinus 0.001)
        map[1.0, 1.0] shouldBe (4.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D 1D interpolation (along x-axis)") {
      test("interpolates between values on x-axis") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 0.0
        map[1.0, 0.0] = 2.0
        map[2.0, 0.0] = 4.0

        // Linear interpolation at midpoint
        map[0.5, 0.0] shouldBe (1.0 plusOrMinus 0.001)
        map[1.5, 0.0] shouldBe (3.0 plusOrMinus 0.001)
      }

      test("interpolates between values on y-axis") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 0.0
        map[0.0, 1.0] = 2.0
        map[0.0, 2.0] = 4.0

        // Linear interpolation at midpoint
        map[0.0, 0.5] shouldBe (1.0 plusOrMinus 0.001)
        map[0.0, 1.5] shouldBe (3.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D bilinear interpolation") {
      test("bilinear interpolation at center") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 0.0
        map[1.0, 0.0] = 1.0
        map[0.0, 1.0] = 2.0
        map[1.0, 1.0] = 3.0

        // At center, all corners contribute equally
        map[0.5, 0.5] shouldBe (1.5 plusOrMinus 0.001)
      }

      test("bilinear interpolation at quarter point") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 0.0
        map[1.0, 0.0] = 4.0
        map[0.0, 1.0] = 4.0
        map[1.0, 1.0] = 8.0

        // t_x = 0.25, t_y = 0.25
        // bottom = 0 * 0.75 + 4 * 0.25 = 1
        // top = 4 * 0.75 + 8 * 0.25 = 5
        // result = 1 * 0.75 + 5 * 0.25 = 2.0
        map[0.25, 0.25] shouldBe (2.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D custom interpolation") {
      test("nearest-neighbor using custom interpolation") {
        val map = InterpolatingMap2D({ q00, q10, q01, q11, tx, ty ->
          // Nearest-neighbor interpolation
          if (tx < 0.5) {
            if (ty < 0.5) q00 else q01
          } else {
            if (ty < 0.5) q10 else q11
          }
        })
        map[0.0, 0.0] = 1.0
        map[1.0, 0.0] = 2.0
        map[0.0, 1.0] = 3.0
        map[1.0, 1.0] = 4.0

        // t_x < 0.5, t_y < 0.5 -> q00
        map[0.25, 0.25] shouldBe (1.0 plusOrMinus 0.001)

        // t_x >= 0.5, t_y < 0.5 -> q10
        map[0.75, 0.25] shouldBe (2.0 plusOrMinus 0.001)

        // t_x < 0.5, t_y >= 0.5 -> q01
        map[0.25, 0.75] shouldBe (3.0 plusOrMinus 0.001)

        // t_x >= 0.5, t_y >= 0.5 -> q11
        map[0.75, 0.75] shouldBe (4.0 plusOrMinus 0.001)
      }

      test("supports custom interpolation function") {
        // Custom: average of all four corners
        val map = InterpolatingMap2D({ q00, q10, q01, q11, _, _ ->
          (q00 + q10 + q01 + q11) / 4.0
        })
        map[0.0, 0.0] = 0.0
        map[1.0, 0.0] = 4.0
        map[0.0, 1.0] = 4.0
        map[1.0, 1.0] = 8.0

        // Average of corners: (0 + 4 + 4 + 8) / 4 = 4
        map[0.5, 0.5] shouldBe (4.0 plusOrMinus 0.001)
      }

      test("custom interpolation with lookup table") {
        val xKeys = listOf(0.0, 1.0)
        val yKeys = listOf(0.0, 1.0)
        val values = listOf(
          listOf(1.0, 2.0),
          listOf(3.0, 4.0),
        )

        // Custom: max of corners
        val map = InterpolatingMap2D(
          { q00, q10, q01, q11, _, _ ->
            maxOf(q00, q10, q01, q11)
          },
          xKeys,
          yKeys,
          values,
        )

        map[0.5, 0.5] shouldBe (4.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D with lookup table") {
      test("interpolates 3x3 grid") {
        val xKeys = listOf(0.0, 1.0, 2.0)
        val yKeys = listOf(0.0, 1.0, 2.0)
        val values = listOf(
          listOf(0.0, 1.0, 2.0),
          listOf(1.0, 2.0, 3.0),
          listOf(2.0, 3.0, 4.0),
        )
        val map = InterpolatingMap2D.bilinear(xKeys, yKeys, values)

        // Check corner values
        map[0.0, 0.0] shouldBe (0.0 plusOrMinus 0.001)
        map[2.0, 2.0] shouldBe (4.0 plusOrMinus 0.001)

        // Check interpolated values
        map[0.5, 0.5] shouldBe (1.0 plusOrMinus 0.001)
        map[1.5, 1.5] shouldBe (3.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D bounds checking") {
      test("throws when querying outside grid bounds") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 0.0
        map[1.0, 1.0] = 1.0

        shouldThrow<NoSuchElementException> { map[-1.0, 0.0] }
        shouldThrow<NoSuchElementException> { map[0.0, -1.0] }
        shouldThrow<NoSuchElementException> { map[2.0, 0.0] }
        shouldThrow<NoSuchElementException> { map[0.0, 2.0] }
      }

      test("throws when grid is empty") {
        val map = InterpolatingMap2D.bilinear()
        shouldThrow<IllegalArgumentException> { map[0.0, 0.0] }
      }
    }

    context("InterpolatingMap2D set and get") {
      test("set updates value") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 1.0
        map[1.0, 1.0] = 2.0

        map[0.0, 0.0] shouldBe (1.0 plusOrMinus 0.001)

        map[0.0, 0.0] = 5.0
        map[0.0, 0.0] shouldBe (5.0 plusOrMinus 0.001)
      }

      test("set creates new row if needed") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 1.0
        map[0.0, 1.0] = 2.0

        map[0.0, 0.0] shouldBe (1.0 plusOrMinus 0.001)
        map[0.0, 1.0] shouldBe (2.0 plusOrMinus 0.001)
      }
    }

    context("InterpolatingMap2D interpolation accuracy") {
      test("bilinear: linear relationship is preserved") {
        val map = InterpolatingMap2D.bilinear()
        // Create a linear surface: z = x + y
        map[0.0, 0.0] = 0.0
        map[1.0, 0.0] = 1.0
        map[0.0, 1.0] = 1.0
        map[1.0, 1.0] = 2.0

        // Sample various points
        for (x in 0..10) {
          for (y in 0..10) {
            val px = x / 10.0
            val py = y / 10.0
            map[px, py] shouldBe (px + py plusOrMinus 0.001)
          }
        }
      }

      test("bilinear: on constant field") {
        val map = InterpolatingMap2D.bilinear()
        map[0.0, 0.0] = 5.0
        map[1.0, 0.0] = 5.0
        map[0.0, 1.0] = 5.0
        map[1.0, 1.0] = 5.0

        map[0.5, 0.5] shouldBe (5.0 plusOrMinus 0.001)
        map[0.25, 0.75] shouldBe (5.0 plusOrMinus 0.001)
      }
    }
  })
