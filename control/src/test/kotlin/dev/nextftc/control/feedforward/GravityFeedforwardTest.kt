/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.feedforward

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.math.PI
import kotlin.math.sqrt

class GravityFeedforwardTest :
  FunSpec({
    context("GravityFeedforwardParameters") {
      test("default values are zero") {
        val params = GravityFeedforwardParameters()
        params.kG shouldBe 0.0
        params.kS shouldBe 0.0
        params.kV shouldBe 0.0
        params.kA shouldBe 0.0
      }

      test("all values can be set via constructor") {
        val params = GravityFeedforwardParameters(1.0, 2.0, 3.0, 4.0)
        params.kG shouldBe 1.0
        params.kS shouldBe 2.0
        params.kV shouldBe 3.0
        params.kA shouldBe 4.0
      }

      test("values are mutable") {
        val params = GravityFeedforwardParameters()
        params.kG = 5.0
        params.kS = 6.0
        params.kV = 7.0
        params.kA = 8.0
        params.kG shouldBe 5.0
        params.kS shouldBe 6.0
        params.kV shouldBe 7.0
        params.kA shouldBe 8.0
      }
    }

    context("ElevatorFeedforward construction") {
      test("can be constructed with coefficients object") {
        val coefficients = GravityFeedforwardParameters(1.0, 2.0, 3.0, 4.0)
        val feedforward = ElevatorFeedforward(coefficients)
        feedforward.coefficients shouldBe coefficients
      }
    }

    context("ElevatorFeedforward calculate") {
      test("calculates correctly with positive velocity") {
        val feedforward =
          ElevatorFeedforward(GravityFeedforwardParameters(0.5, 0.2, 2.0, 0.1))
        // output = kG + kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 0.5 + 0.2 * 1 + 2.0 * 3.0 + 0.1 * 0.5 = 0.5 + 0.2 + 6.0 + 0.05 = 6.75
        feedforward.calculate(3.0, 0.5) shouldBe (6.75 plusOrMinus 0.001)
      }

      test("calculates correctly with negative velocity") {
        val feedforward =
          ElevatorFeedforward(GravityFeedforwardParameters(0.5, 0.2, 2.0, 0.1))
        // output = kG + kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 0.5 + 0.2 * (-1) + 2.0 * (-3.0) + 0.1 * 0.5 = 0.5 - 0.2 - 6.0 + 0.05 = -5.65
        feedforward.calculate(-3.0, 0.5) shouldBe (-5.65 plusOrMinus 0.001)
      }

      test("calculates correctly with zero velocity") {
        val feedforward =
          ElevatorFeedforward(GravityFeedforwardParameters(0.5, 0.2, 2.0, 0.1))
        // output = kG + kS * sign(0) + kV * 0 + kA * acceleration
        // output = 0.5 + 0 + 0 + 0.1 * 2.0 = 0.7
        feedforward.calculate(0.0, 2.0) shouldBe (0.7 plusOrMinus 0.001)
      }

      test("gravity term is constant regardless of velocity") {
        val feedforward =
          ElevatorFeedforward(GravityFeedforwardParameters(1.5, 0.0, 0.0, 0.0))
        feedforward.calculate(0.0, 0.0) shouldBe 1.5
        feedforward.calculate(10.0, 0.0) shouldBe 1.5
        feedforward.calculate(-10.0, 0.0) shouldBe 1.5
      }
    }

    context("ArmFeedforward construction") {
      test("can be constructed with coefficients object") {
        val coefficients = GravityFeedforwardParameters(1.0, 2.0, 3.0, 4.0)
        val feedforward = ArmFeedforward(coefficients)
        feedforward.coefficients shouldBe coefficients
      }
    }

    context("ArmFeedforward calculate") {
      test("calculates correctly at horizontal position (0 radians)") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(1.0, 0.2, 2.0, 0.1))
        // position = 0, cos(0) = 1
        // output = kG * cos(position) + kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 1.0 * 1 + 0.2 * 1 + 2.0 * 3.0 + 0.1 * 0.5 = 1.0 + 0.2 + 6.0 + 0.05 = 7.25
        feedforward.calculate(0.0, 3.0, 0.5) shouldBe (7.25 plusOrMinus 0.001)
      }

      test("calculates correctly at vertical position (π/2 radians)") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(1.0, 0.2, 2.0, 0.1))
        // position = π/2, cos(π/2) ≈ 0
        // output = kG * cos(position) + kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 1.0 * 0 + 0.2 * 1 + 2.0 * 3.0 + 0.1 * 0.5 = 0 + 0.2 + 6.0 + 0.05 = 6.25
        feedforward.calculate(PI / 2, 3.0, 0.5) shouldBe (6.25 plusOrMinus 0.001)
      }

      test("calculates correctly at 45 degrees (π/4 radians)") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(1.0, 0.0, 0.0, 0.0))
        // position = π/4, cos(π/4) = √2/2
        // output = kG * cos(position) = 1.0 * √2/2 ≈ 0.7071
        feedforward.calculate(PI / 4, 0.0, 0.0) shouldBe (sqrt(2.0) / 2 plusOrMinus 0.001)
      }

      test("calculates correctly at 180 degrees (π radians) - pointing down") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(1.0, 0.0, 0.0, 0.0))
        // position = π, cos(π) = -1
        // output = kG * cos(position) = 1.0 * (-1) = -1.0
        feedforward.calculate(PI, 0.0, 0.0) shouldBe (-1.0 plusOrMinus 0.001)
      }

      test("calculates correctly with negative velocity") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(0.5, 0.2, 2.0, 0.1))
        // position = 0, cos(0) = 1
        // output = kG * cos(position) + kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 0.5 * 1 + 0.2 * (-1) + 2.0 * (-3.0) + 0.1 * 0.5 = 0.5 - 0.2 - 6.0 + 0.05 = -5.65
        feedforward.calculate(0.0, -3.0, 0.5) shouldBe (-5.65 plusOrMinus 0.001)
      }

      test("calculates correctly with zero velocity") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(0.5, 0.2, 2.0, 0.1))
        // position = 0, cos(0) = 1
        // output = kG * cos(position) + kS * sign(0) + kV * 0 + kA * acceleration
        // output = 0.5 * 1 + 0 + 0 + 0.1 * 2.0 = 0.7
        feedforward.calculate(0.0, 0.0, 2.0) shouldBe (0.7 plusOrMinus 0.001)
      }

      test("gravity compensation varies with angle") {
        val feedforward = ArmFeedforward(GravityFeedforwardParameters(kG = 2.0))

        // At horizontal, full gravity compensation
        feedforward.calculate(0.0, 0.0, 0.0) shouldBe (2.0 plusOrMinus 0.001)

        // At 60 degrees, half gravity compensation
        feedforward.calculate(PI / 3, 0.0, 0.0) shouldBe (1.0 plusOrMinus 0.001)

        // At vertical, no gravity compensation needed
        feedforward.calculate(PI / 2, 0.0, 0.0) shouldBe (0.0 plusOrMinus 0.001)

        // Past vertical, negative gravity compensation (arm wants to fall the other way)
        feedforward.calculate(2 * PI / 3, 0.0, 0.0) shouldBe (-1.0 plusOrMinus 0.001)
      }
    }
  })
