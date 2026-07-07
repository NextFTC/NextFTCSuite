/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.Inches
import dev.nextftc.units.inches
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.inchesPerSecondSquared
import dev.nextftc.units.seconds
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.sqrt

class Vector2dTest :
  FunSpec({
    val epsilon = 1e-6

    test("vector addition") {
      val v1 = Vector2d(1.0.inches, 2.0.inches)
      val v2 = Vector2d(3.0.inches, 4.0.inches)
      val sum = v1 + v2
      sum.x.into(Inches) shouldBe (4.0 plusOrMinus epsilon)
      sum.y.into(Inches) shouldBe (6.0 plusOrMinus epsilon)
    }

    test("vector subtraction") {
      val v1 = Vector2d(3.0.inches, 4.0.inches)
      val v2 = Vector2d(1.0.inches, 2.0.inches)
      val diff = v1 - v2
      diff.x.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
      diff.y.into(Inches) shouldBe (2.0 plusOrMinus epsilon)
    }

    test("vector negation") {
      val v = Vector2d(3.0.inches, -4.0.inches)
      val neg = -v
      neg.x.into(Inches) shouldBe (-3.0 plusOrMinus epsilon)
      neg.y.into(Inches) shouldBe (4.0 plusOrMinus epsilon)
    }

    test("scalar multiplication and division") {
      val v = Vector2d(2.0.inches, 3.0.inches)
      val mult = v * 2.5
      mult.x.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mult.y.into(Inches) shouldBe (7.5 plusOrMinus epsilon)

      val div = v / 2.0
      div.x.into(Inches) shouldBe (1.0 plusOrMinus epsilon)
      div.y.into(Inches) shouldBe (1.5 plusOrMinus epsilon)
    }

    test("dot product") {
      val v1 = Vector2d(1.0.inches, 2.0.inches)
      val v2 = Vector2d(3.0.inches, 4.0.inches)
      val dot = v1 dot v2
      dot shouldBe (11.0 plusOrMinus epsilon)
    }

    test("norm and sqrNorm") {
      val v = Vector2d(3.0.inches, 4.0.inches)
      v.sqrNorm() shouldBe (25.0 plusOrMinus epsilon)
      v.norm() shouldBe (5.0 plusOrMinus epsilon)
    }

    test("angle and angleCast") {
      val v = Vector2d(1.0.inches, 1.0.inches)
      val angle = v.angle()
      angle.log() shouldBe (PI / 4.0 plusOrMinus epsilon)

      // For angleCast, vector must be normalized
      val normalized = v / v.norm()
      val casted = normalized.angleCast()
      casted.log() shouldBe (PI / 4.0 plusOrMinus epsilon)
    }

    test("lerp") {
      val v1 = Vector2d(0.0.inches, 0.0.inches)
      val v2 = Vector2d(10.0.inches, 10.0.inches)
      val mid = v1.lerp(v2, 0.5)
      mid.x.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
      mid.y.into(Inches) shouldBe (5.0 plusOrMinus epsilon)
    }

    test("times time (velocity to displacement)") {
      val v = Vector2d(2.0.inchesPerSecond, 3.0.inchesPerSecond)
      val t = 2.0.seconds
      val d = v * t
      d.x.into(Inches) shouldBe (4.0 plusOrMinus epsilon)
      d.y.into(Inches) shouldBe (6.0 plusOrMinus epsilon)
    }

    test("times time (acceleration to velocity)") {
      val a = Vector2d(1.0.inchesPerSecondSquared, 2.0.inchesPerSecondSquared)
      val t = 3.0.seconds
      val v = a * t
      v.x.into(dev.nextftc.units.InchesPerSecond) shouldBe (3.0 plusOrMinus epsilon)
      v.y.into(dev.nextftc.units.InchesPerSecond) shouldBe (6.0 plusOrMinus epsilon)
    }
  })
