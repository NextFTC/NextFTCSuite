/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors.colors

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ColorProfileTest :
  FunSpec({
    beforeSpec { installFakeAndroidColor() }
    afterSpec { uninstallFakeAndroidColor() }

    context("RGB space") {
      val profile = ColorProfile(
        space = ColorSpace.RGB,
        color = NextColor.rgb(100f, 150f, 200f),
        tolerance = NextColor.rgb(10f, 10f, 10f),
      )

      test("matches a reading within tolerance on every channel") {
        profile.matches(NextColor.rgb(105f, 145f, 205f)) shouldBe true
      }

      test("matches an exact reading") {
        profile.matches(NextColor.rgb(100f, 150f, 200f)) shouldBe true
      }

      test("rejects a reading outside tolerance on one channel") {
        profile.matches(NextColor.rgb(100f, 150f, 215f)) shouldBe false
      }

      test("rejects a reading outside tolerance on every channel") {
        profile.matches(NextColor.rgb(0f, 0f, 0f)) shouldBe false
      }
    }

    context("HSV space") {
      val profile = ColorProfile(
        space = ColorSpace.HSV,
        color = NextColor.hsv(130f, 0.7f, 0.6f),
        tolerance = NextColor.hsv(20f, 0.3f, 0.3f),
      )

      test("matches a reading within tolerance") {
        profile.matches(NextColor.hsv(140f, 0.8f, 0.7f)) shouldBe true
      }

      test("rejects a reading with hue outside tolerance") {
        profile.matches(NextColor.hsv(200f, 0.7f, 0.6f)) shouldBe false
      }

      test("hue comparison wraps around 0/360") {
        val wraparoundProfile = ColorProfile(
          space = ColorSpace.HSV,
          color = NextColor.hsv(5f, 0.5f, 0.5f),
          tolerance = NextColor.hsv(20f, 1f, 1f),
        )
        wraparoundProfile.matches(NextColor.hsv(355f, 0.5f, 0.5f)) shouldBe true
      }
    }
  })
