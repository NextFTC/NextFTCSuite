/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors.colors

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

class NextColorTest :
  FunSpec({
    beforeSpec { installFakeAndroidColor() }
    afterSpec { uninstallFakeAndroidColor() }

    context("rgb() validation") {
      test("accepts values at the bounds") {
        NextColor.rgb(0f, 0f, 0f)
        NextColor.rgb(255f, 255f, 255f)
      }

      test("rejects a red value out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.rgb(300f, 0f, 0f) }
      }

      test("rejects a green value out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.rgb(0f, -1f, 0f) }
      }

      test("rejects a blue value out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.rgb(0f, 0f, 256f) }
      }
    }

    context("hsv() validation") {
      test("accepts values at the bounds") {
        NextColor.hsv(0f, 0f, 0f)
        NextColor.hsv(360f, 1f, 1f)
      }

      test("rejects a hue value out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.hsv(400f, 0.5f, 0.5f) }
      }

      test("rejects a saturation value out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.hsv(0f, 1.5f, 0.5f) }
      }

      test("rejects a value component out of range") {
        shouldThrow<IllegalArgumentException> { NextColor.hsv(0f, 0.5f, -0.1f) }
      }
    }

    context("rgb/hsv round trip") {
      test("pure red round-trips through hsv") {
        val red = NextColor.rgb(255f, 0f, 0f)
        val hsv = red.hsv
        hsv[0] shouldBe (0f plusOrMinus 0.01f)
        hsv[1] shouldBe (1f plusOrMinus 0.01f)
        hsv[2] shouldBe (1f plusOrMinus 0.01f)
      }

      test("constructing from hsv yields the expected rgb channels") {
        val green = NextColor.hsv(120f, 1f, 1f)
        green.red shouldBe (0f plusOrMinus 1f)
        green.green shouldBe (255f plusOrMinus 1f)
        green.blue shouldBe (0f plusOrMinus 1f)
      }
    }
  })
