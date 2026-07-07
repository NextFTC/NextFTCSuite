/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.Inches
import dev.nextftc.units.Radians
import dev.nextftc.units.degrees
import dev.nextftc.units.inches
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.radiansPerSecond
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.sqrt

class Rotation2dTest :
  FunSpec({
    val epsilon = 1e-6

    test("plus and minus angles") {
      val r1 = Rotation2d.exp(PI / 4)
      val r2 = r1 + (PI / 4)
      r2.log() shouldBe (PI / 2 plusOrMinus epsilon)

      val diff = r2 - r1
      diff shouldBe (PI / 4 plusOrMinus epsilon)
    }

    test("vector rotation") {
      val r = Rotation2d.exp(PI / 2)
      val v = Vector2d(1.0.inches, 0.0.inches)
      val rotated = r * v
      rotated.x.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      rotated.y.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
    }

    test("pose velocity rotation") {
      val r = Rotation2d.exp(PI / 2)
      val pv = PoseVelocity2d(Vector2d(1.0.inchesPerSecond, 0.0.inchesPerSecond), 1.0.radiansPerSecond)
      val rotated = r * pv
      rotated.linearVel.x.into(dev.nextftc.units.InchesPerSecond) shouldBe (0.0 plusOrMinus epsilon)
      rotated.linearVel.y.into(dev.nextftc.units.InchesPerSecond) shouldBe (1.0 plusOrMinus epsilon)
      rotated.angVel.into(dev.nextftc.units.RadiansPerSecond) shouldBe (1.0 plusOrMinus epsilon)
    }

    test("rotation composition") {
      val r1 = Rotation2d.exp(PI / 4)
      val r2 = Rotation2d.exp(PI / 4)
      val r3 = r1 * r2
      r3.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("vec and inverse") {
      val r = Rotation2d.exp(PI / 4)
      val v = r.vec()
      v.x.into(Inches) shouldBe (sqrt(2.0) / 2.0 plusOrMinus epsilon)
      v.y.into(Inches) shouldBe (sqrt(2.0) / 2.0 plusOrMinus epsilon)

      val inv = r.inverse()
      inv.log() shouldBe (-PI / 4 plusOrMinus epsilon)
    }

    test("lerp") {
      val r1 = Rotation2d.exp(0.0)
      val r2 = Rotation2d.exp(PI)
      val mid = r1.lerp(r2, 0.5)
      mid.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("factory methods") {
      val r = Rotation2d.fromAngle(90.0.degrees)
      r.log() shouldBe (PI / 2 plusOrMinus epsilon)

      val r2 = Rotation2d.fromDouble(PI)
      r2.log() shouldBe (PI plusOrMinus epsilon)
      r2.toDouble() shouldBe (PI plusOrMinus epsilon)
    }
  })
