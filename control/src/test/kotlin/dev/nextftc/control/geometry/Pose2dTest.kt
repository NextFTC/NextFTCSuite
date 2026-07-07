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

class Pose2dTest :
  FunSpec({
    val epsilon = 1e-6

    test("canonical constructors with type-safe units") {
      // Distance, Distance, Rotation2d
      val p1 = Pose2d(1.0.inches, 2.0.inches, Rotation2d.exp(PI / 2))
      p1.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p1.position.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      p1.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Vector2d, Rotation2d (via primary constructor)
      val p2 = Pose2d(Vector2d(1.0.inches, 2.0.inches), Rotation2d.exp(PI / 2))
      p2.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p2.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Distance, Distance, Angle
      val p3 = Pose2d(1.0.inches, 2.0.inches, 90.0.degrees)
      p3.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("legacy double constructors") {
      // Double, Double, Double
      val p1 = Pose2d(1.0, 2.0, PI / 2)
      p1.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p1.position.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      p1.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Double, Double, Rotation2d
      val p2 = Pose2d(1.0, 2.0, Rotation2d.exp(PI / 2))
      p2.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p2.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)

      // Vector2d, Double
      val p3 = Pose2d(Vector2d(1.0.inches, 2.0.inches), PI / 2)
      p3.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("plus twist") {
      val p = Pose2d(0.0, 0.0, 0.0)
      val t = Twist2d(1.0, 0.0, PI / 2)
      val p2 = p + t
      val tBack = p2.log()
      tBack.line.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      tBack.line.y.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      tBack.angle shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("plus transform") {
      val p = Pose2d(1.0, 1.0, PI / 2)
      val t = Transform2d(1.0, 0.0, -PI / 2)
      val p2 = p + t
      p2.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p2.position.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      p2.heading.log() shouldBe (0.0 plusOrMinus epsilon)
    }

    test("relativeTo") {
      val p1 = Pose2d(1.0, 1.0, PI / 2)
      val p2 = Pose2d(1.0, 2.0, 0.0)
      val t = p1.relativeTo(p2)
      t.translation.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t.translation.y.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      t.rotation.log() shouldBe (-PI / 2 plusOrMinus epsilon)
    }

    test("minus (twist)") {
      val p1 = Pose2d(0.0, 0.0, 0.0)
      val p2 = Pose2d(1.0, 0.0, PI / 2)
      val twist = p2 - p1
      val p3 = p1 + twist
      p3.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p3.position.y.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      p3.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("times (compose)") {
      val p1 = Pose2d(1.0, 1.0, PI / 2)
      val p2 = Pose2d(1.0, 0.0, -PI / 2)
      val p3 = p1 * p2
      p3.position.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      p3.position.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      p3.heading.log() shouldBe (0.0 plusOrMinus epsilon)
    }

    test("times (transform vector)") {
      val p = Pose2d(1.0, 1.0, PI / 2)
      val v = Vector2d(1.0.inches, 0.0.inches)
      val v2 = p * v
      v2.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      v2.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
    }

    test("inverse") {
      val p = Pose2d(1.0, 1.0, PI / 2)
      val inv = p.inverse()
      val identity = p * inv
      identity.position.x.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      identity.position.y.into(Inches) shouldBe (0.0 plusOrMinus epsilon)
      identity.heading.log() shouldBe (0.0 plusOrMinus epsilon)
    }

    test("lerp") {
      val p1 = Pose2d(0.0, 0.0, 0.0)
      val p2 = Pose2d(10.0, 10.0, PI)
      val mid = p1.lerp(p2, 0.5)
      mid.position.x.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mid.position.y.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mid.heading.log() shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("distanceTo") {
      val p1 = Pose2d(0.0, 0.0, 0.0)
      val p2 = Pose2d(3.0, 4.0, PI)
      p1.distanceTo(p2) shouldBe (5.0 plusOrMinus epsilon)
    }
  })
