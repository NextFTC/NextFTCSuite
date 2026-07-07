/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.InchesPerSecond
import dev.nextftc.units.RadiansPerSecond
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.radiansPerSecond
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI

class ChassisVelocitiesTest :
  FunSpec({
    val epsilon = 1e-6

    test("math operations") {
      val v1 = ChassisVelocities(Vector2d(1.0.inchesPerSecond, 2.0.inchesPerSecond), 1.0.radiansPerSecond)
      val v2 = ChassisVelocities(Vector2d(3.0.inchesPerSecond, 4.0.inchesPerSecond), 2.0.radiansPerSecond)

      val sum = v1 + v2
      sum.linearVel.x.into(InchesPerSecond) shouldBe (4.0 plusOrMinus epsilon)
      sum.angVel.into(RadiansPerSecond) shouldBe (3.0 plusOrMinus epsilon)

      val diff = v2 - v1
      diff.linearVel.x.into(InchesPerSecond) shouldBe (2.0 plusOrMinus epsilon)
      diff.angVel.into(RadiansPerSecond) shouldBe (1.0 plusOrMinus epsilon)

      val neg = -v1
      neg.linearVel.x.into(InchesPerSecond) shouldBe (-1.0 plusOrMinus epsilon)
      neg.angVel.into(RadiansPerSecond) shouldBe (-1.0 plusOrMinus epsilon)

      val mult = v1 * 2.0
      mult.linearVel.x.into(InchesPerSecond) shouldBe (2.0 plusOrMinus epsilon)
      mult.angVel.into(RadiansPerSecond) shouldBe (2.0 plusOrMinus epsilon)

      val div = v1 / 2.0
      div.linearVel.x.into(InchesPerSecond) shouldBe (0.5 plusOrMinus epsilon)
      div.angVel.into(RadiansPerSecond) shouldBe (0.5 plusOrMinus epsilon)
    }

    test("toPose") {
      val v = ChassisVelocities(Vector2d(1.0.inchesPerSecond, 0.0.inchesPerSecond), 1.0.radiansPerSecond)
      val heading = Rotation2d.exp(PI / 2)
      val fieldVel = v.toPose(heading)

      fieldVel.linearVel.x.into(InchesPerSecond) shouldBe (0.0 plusOrMinus epsilon)
      fieldVel.linearVel.y.into(InchesPerSecond) shouldBe (1.0 plusOrMinus epsilon)
      fieldVel.angVel.into(RadiansPerSecond) shouldBe (1.0 plusOrMinus epsilon)
    }

    test("lerp") {
      val v1 = ChassisVelocities(Vector2d(0.0.inchesPerSecond, 0.0.inchesPerSecond), 0.0.radiansPerSecond)
      val v2 =
        ChassisVelocities(Vector2d(10.0.inchesPerSecond, 10.0.inchesPerSecond), 2.0.radiansPerSecond)

      val mid = v1.lerp(v2, 0.5)
      mid.linearVel.x.into(InchesPerSecond) shouldBe (5.0 plusOrMinus epsilon)
      mid.angVel.into(RadiansPerSecond) shouldBe (1.0 plusOrMinus epsilon)
    }
  })
