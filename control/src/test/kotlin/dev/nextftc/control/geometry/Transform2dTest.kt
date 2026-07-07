/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.Inches
import dev.nextftc.units.degrees
import dev.nextftc.units.inches
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI

class Transform2dTest :
  FunSpec({
    val epsilon = 1e-6

    test("canonical constructors with type-safe units") {
      // Distance, Distance, Rotation2d
      val t1 = Transform2d(1.0.inches, 2.0.inches, Rotation2d.exp(PI / 2))
      t1.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t1.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Vector2d, Rotation2d (via primary constructor)
      val t2 = Transform2d(Vector2d(1.0.inches, 2.0.inches), Rotation2d.exp(PI / 2))
      t2.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t2.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Distance, Distance, Angle
      val t3 = Transform2d(1.0.inches, 2.0.inches, 90.0.degrees)
      t3.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Pose2d, Pose2d (relative transform)
      val t4 = Transform2d(Pose2d(0.0, 0.0, 0.0), Pose2d(1.0, 2.0, PI / 2))
      t4.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t4.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("legacy double constructors") {
      // Double, Double, Double
      val t1 = Transform2d(1.0, 2.0, PI / 2)
      t1.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t1.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Double, Double, Rotation2d
      val t2 = Transform2d(1.0, 2.0, Rotation2d.exp(PI / 2))
      t2.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t2.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Vector2d, Double
      val t3 = Transform2d(Vector2d(1.0.inches, 2.0.inches), PI / 2)
      t3.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("math operations") {
      val t1 = Transform2d(1.0, 0.0, PI / 2)
      val t2 = Transform2d(1.0, 0.0, 0.0)

      val sum = t1 + t2
      sum.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      sum.translation.y.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      sum.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      val inv = t1.inverse()
      inv.translation.x.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      inv.translation.y.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      inv.rotation.log() shouldBe (-PI / 2 plusOrMinus epsilon)

      val diff = sum - t2
      diff.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      diff.translation.y.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      diff.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)

      val scaled = t1 * 2.0
      scaled.translation.x.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      scaled.rotation.log() shouldBe (PI plusOrMinus epsilon)

      val div = t1 / 2.0
      div.translation.x.into(Inches) shouldBe (0.5 plusOrMinus epsilon)
      div.rotation.log() shouldBe (PI / 4 plusOrMinus epsilon)
    }

    test("times vector") {
      val t = Transform2d(1.0, 1.0, PI / 2)
      val v = Vector2d(1.0.inches, 0.0.inches)
      val res = t * v
      res.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      res.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
    }

    test("lerp") {
      val t1 = Transform2d(0.0, 0.0, 0.0)
      val t2 = Transform2d(10.0, 10.0, PI)

      val mid = t1.lerp(t2, 0.5)
      mid.translation.x.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mid.rotation.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }
  })
