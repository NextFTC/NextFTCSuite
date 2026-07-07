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
import io.kotest.matchers.shouldBe

class MatrixBuilderTest :
  FunSpec({
    test("buildMatrix requires correct row length") {
      shouldThrow<IllegalArgumentException> {
        buildMatrix(N2, N3) {
          // natCols == 3 but we provide only 2 elements -> should throw
          row(1.0, 2.0)
          row(3.0, 4.0, 5.0)
        }
      }
    }

    test("buildMatrix requires correct row count") {
      shouldThrow<IllegalStateException> {
        buildMatrix(N3, N2) {
          // natRows == 3 but we only provide 2 rows -> build() should throw
          row(1.0, 2.0)
          row(3.0, 4.0)
        }
      }
    }

    test("buildMatrix constructs matrix with correct values") {
      val m = buildMatrix(N2, N3) {
        row(1.0, 2.0, 3.0)
        row(4.0, 5.0, 6.0)
      }

      m.numRows shouldBe 2
      m.numColumns shouldBe 3
      m[0, 0] shouldBe 1.0
      m[0, 1] shouldBe 2.0
      m[0, 2] shouldBe 3.0
      m[1, 0] shouldBe 4.0
      m[1, 1] shouldBe 5.0
      m[1, 2] shouldBe 6.0
    }
  })
