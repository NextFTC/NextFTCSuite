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

class SimpleFeedforwardTest :
  FunSpec({
    context("SimpleFFCoefficients") {
      test("default kA is zero") {
        val coefficients = SimpleFFCoefficients(1.0, 2.0)
        coefficients.kS shouldBe 1.0
        coefficients.kV shouldBe 2.0
        coefficients.kA shouldBe 0.0
      }

      test("all values can be set") {
        val coefficients = SimpleFFCoefficients(1.0, 2.0, 3.0)
        coefficients.kS shouldBe 1.0
        coefficients.kV shouldBe 2.0
        coefficients.kA shouldBe 3.0
      }

      test("values are mutable") {
        val coefficients = SimpleFFCoefficients(1.0, 2.0, 3.0)
        coefficients.kS = 5.0
        coefficients.kV = 6.0
        coefficients.kA = 7.0
        coefficients.kS shouldBe 5.0
        coefficients.kV shouldBe 6.0
        coefficients.kA shouldBe 7.0
      }
    }

    context("SimpleFeedforward construction") {
      test("can be constructed with coefficients object") {
        val coefficients = SimpleFFCoefficients(1.0, 2.0, 3.0)
        val feedforward = SimpleFeedforward(coefficients)
        feedforward.coefficients shouldBe coefficients
      }
    }

    context("SimpleFeedforward calculate") {
      test("calculates correctly with positive velocity") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // output = kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 0.5 * 1 + 2.0 * 3.0 + 0.1 * 0.5 = 0.5 + 6.0 + 0.05 = 6.55
        feedforward.calculate(3.0, 0.5) shouldBe (6.55 plusOrMinus 0.001)
      }

      test("calculates correctly with negative velocity") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // output = kS * sign(velocity) + kV * velocity + kA * acceleration
        // output = 0.5 * (-1) + 2.0 * (-3.0) + 0.1 * 0.5 = -0.5 - 6.0 + 0.05 = -6.45
        feedforward.calculate(-3.0, 0.5) shouldBe (-6.45 plusOrMinus 0.001)
      }

      test("calculates correctly with zero velocity") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // output = kS * sign(0) + kV * 0 + kA * acceleration
        // output = 0 + 0 + 0.1 * 2.0 = 0.2
        feedforward.calculate(0.0, 2.0) shouldBe (0.2 plusOrMinus 0.001)
      }

      test("calculates correctly with default acceleration") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // output = kS * sign(velocity) + kV * velocity + kA * 0
        // output = 0.5 * 1 + 2.0 * 3.0 + 0 = 6.5
        feedforward.calculate(3.0) shouldBe (6.5 plusOrMinus 0.001)
      }
    }

    context("SimpleFeedforward velocity constraints") {
      test("maxAchievableVelocity calculates correctly") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // maxVel = (maxVoltage - kS - acceleration * kA) / kV
        // maxVel = (12.0 - 0.5 - 1.0 * 0.1) / 2.0 = (12.0 - 0.5 - 0.1) / 2.0 = 11.4 / 2.0 = 5.7
        feedforward.maxAchievableVelocity(12.0, 1.0) shouldBe (5.7 plusOrMinus 0.001)
      }

      test("minAchievableVelocity calculates correctly") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // minVel = (-maxVoltage + kS - acceleration * kA) / kV
        // minVel = (-12.0 + 0.5 - 1.0 * 0.1) / 2.0 = (-12.0 + 0.5 - 0.1) / 2.0 = -11.6 / 2.0 = -5.8
        feedforward.minAchievableVelocity(12.0, 1.0) shouldBe (-5.8 plusOrMinus 0.001)
      }
    }

    context("SimpleFeedforward acceleration constraints") {
      test("maxAchievableAcceleration calculates correctly with positive velocity") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // maxAccel = (maxVoltage - kS * sign(velocity) - velocity * kV) / kA
        // maxAccel = (12.0 - 0.5 * 1 - 2.0 * 2.0) / 0.1 = (12.0 - 0.5 - 4.0) / 0.1 = 7.5 / 0.1 = 75
        feedforward.maxAchievableAcceleration(12.0, 2.0) shouldBe (75.0 plusOrMinus 0.001)
      }

      test("maxAchievableAcceleration calculates correctly with negative velocity") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // maxAccel = (maxVoltage - kS * sign(velocity) - velocity * kV) / kA
        // maxAccel = (12.0 - 0.5 * (-1) - (-2.0) * 2.0) / 0.1 = (12.0 + 0.5 + 4.0) / 0.1 = 16.5 / 0.1 = 165
        feedforward.maxAchievableAcceleration(12.0, -2.0) shouldBe (165.0 plusOrMinus 0.001)
      }

      test("minAchievableAcceleration calculates correctly") {
        val feedforward = SimpleFeedforward(SimpleFFCoefficients(0.5, 2.0, 0.1))
        // minAccel = maxAchievableAcceleration(-maxVoltage, velocity)
        // minAccel = (-12.0 - 0.5 * 1 - 2.0 * 2.0) / 0.1 = (-12.0 - 0.5 - 4.0) / 0.1 = -16.5 / 0.1 = -165
        feedforward.minAchievableAcceleration(12.0, 2.0) shouldBe (-165.0 plusOrMinus 0.001)
      }
    }
  })
