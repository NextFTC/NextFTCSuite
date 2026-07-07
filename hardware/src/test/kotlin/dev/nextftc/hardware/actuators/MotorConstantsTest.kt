/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.actuators

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MotorConstantsTest :
  FunSpec({
    context("MotorPositionConstants") {
      test("defaults are all zero") {
        val constants = MotorPositionConstants()
        constants.kP shouldBe 0.0
        constants.kI shouldBe 0.0
        constants.kD shouldBe 0.0
        constants.kS shouldBe 0.0
        constants.kV shouldBe 0.0
        constants.kA shouldBe 0.0
        constants.kG shouldBe 0.0
        constants.kCos shouldBe 0.0
        constants.kCosRatio shouldBe 0.0
      }

      test("with* builders return the same instance updated in place") {
        val constants = MotorPositionConstants()
        val result = constants
          .withP(1.0)
          .withI(2.0)
          .withD(3.0)
          .withS(4.0)
          .withV(5.0)
          .withA(6.0)
          .withG(7.0)
          .withCos(8.0)
          .withCosRatio(9.0)

        result shouldBe constants
        constants.kP shouldBe 1.0
        constants.kI shouldBe 2.0
        constants.kD shouldBe 3.0
        constants.kS shouldBe 4.0
        constants.kV shouldBe 5.0
        constants.kA shouldBe 6.0
        constants.kG shouldBe 7.0
        constants.kCos shouldBe 8.0
        constants.kCosRatio shouldBe 9.0
      }

      test("kP/kI/kD delegate to the shared PID coefficients") {
        val constants = MotorPositionConstants()
        constants.kP = 1.5
        constants.pidConstants.kP shouldBe 1.5
      }

      test("kS/kV/kA delegate to the shared feedforward coefficients") {
        val constants = MotorPositionConstants()
        constants.kV = 2.5
        constants.ffCoefficients.kV shouldBe 2.5
      }
    }

    context("MotorVelocityConstants") {
      test("defaults are all zero") {
        val constants = MotorVelocityConstants()
        constants.kP shouldBe 0.0
        constants.kI shouldBe 0.0
        constants.kD shouldBe 0.0
        constants.kS shouldBe 0.0
        constants.kV shouldBe 0.0
        constants.kA shouldBe 0.0
      }

      test("with* builders return the same instance updated in place") {
        val constants = MotorVelocityConstants()
        val result = constants
          .withP(1.0)
          .withI(2.0)
          .withD(3.0)
          .withS(4.0)
          .withV(5.0)
          .withA(6.0)

        result shouldBe constants
        constants.kP shouldBe 1.0
        constants.kI shouldBe 2.0
        constants.kD shouldBe 3.0
        constants.kS shouldBe 4.0
        constants.kV shouldBe 5.0
        constants.kA shouldBe 6.0
      }

      test("kP/kI/kD delegate to the shared PID coefficients") {
        val constants = MotorVelocityConstants()
        constants.kD = 3.5
        constants.pidConstants.kD shouldBe 3.5
      }

      test("kS/kV/kA delegate to the shared feedforward coefficients") {
        val constants = MotorVelocityConstants()
        constants.kA = 4.5
        constants.ffCoefficients.kA shouldBe 4.5
      }
    }
  })
