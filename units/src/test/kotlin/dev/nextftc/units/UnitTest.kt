/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.units

import dev.nextftc.units.unittypes.DistanceUnit
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class UnitTest :
  FunSpec({
    test("equal units have equal hash codes") {
      // Two independently constructed units with the same class, name, symbol, and conversion
      // behavior must be equals()-true, and therefore must also share a hashCode(), even though
      // they are backed by distinct converter lambda closures.
      val meters1 = DistanceUnit(null, { it }, { it }, "Meter", "m")
      val meters2 = DistanceUnit(null, { it }, { it }, "Meter", "m")

      meters1 shouldBe meters2
      meters1.hashCode() shouldBe meters2.hashCode()
    }
  })
