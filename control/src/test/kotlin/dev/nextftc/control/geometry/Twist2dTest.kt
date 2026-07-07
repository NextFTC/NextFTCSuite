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

class Twist2dTest :
  FunSpec({
    val epsilon = 1e-6

    test("canonical constructors with type-safe units") {
      // Distance, Distance, Rotation2d
      val t1 = Twist2d(1.0.inches, 2.0.inches, Rotation2d.exp(PI / 2))
      t1.line.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t1.angle shouldBe (PI / 2 plusOrMinus epsilon)

      // Distance, Distance, Angle
      val t2 = Twist2d(1.0.inches, 2.0.inches, 90.0.degrees)
      t2.line.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t2.angle shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("legacy double constructors") {
      // Double, Double, Double
      val t1 = Twist2d(1.0, 2.0, PI / 2)
      t1.line.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t1.angle shouldBe (PI / 2 plusOrMinus epsilon)

      // Double, Double, Rotation2d
      val t2 = Twist2d(1.0, 2.0, Rotation2d.exp(PI / 2))
      t2.line.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      t2.angle shouldBe (PI / 2 plusOrMinus epsilon)
    }

    test("math operations") {
      val t1 = Twist2d(1.0, 2.0, PI / 4)
      val t2 = Twist2d(3.0, 4.0, PI / 4)

      val sum = t1 + t2
      sum.line.x.into(Inches) shouldBe (4.0 plusOrMinus epsilon)
      sum.angle shouldBe (PI / 2 plusOrMinus epsilon)

      val diff = t2 - t1
      diff.line.x.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      diff.angle shouldBe (0.0 plusOrMinus epsilon)

      val neg = -t1
      neg.line.x.into(Inches) shouldBe (-1.0 plusOrMinus epsilon)
      neg.angle shouldBe (-PI / 4 plusOrMinus epsilon)

      val mult = t1 * 2.0
      mult.line.x.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      mult.angle shouldBe (PI / 2 plusOrMinus epsilon)

      val div = t1 / 2.0
      div.line.x.into(Inches) shouldBe (0.5 plusOrMinus epsilon)
      div.angle shouldBe (PI / 8 plusOrMinus epsilon)
    }

    test("lerp") {
      val t1 = Twist2d(0.0, 0.0, 0.0)
      val t2 = Twist2d(10.0, 10.0, PI)

      val mid = t1.lerp(t2, 0.5)
      mid.line.x.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mid.angle shouldBe (PI / 2 plusOrMinus epsilon)
    }
  })
