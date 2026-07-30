/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.feedback

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class PIDControllerTest :
  FunSpec({
    context("PIDCoefficients") {
      test("default values for kI and kD are zero") {
        val coefficients = PIDCoefficients(1.0)
        coefficients.kP shouldBe 1.0
        coefficients.kI shouldBe 0.0
        coefficients.kD shouldBe 0.0
      }

      test("all values can be set") {
        val coefficients = PIDCoefficients(1.0, 2.0, 3.0)
        coefficients.kP shouldBe 1.0
        coefficients.kI shouldBe 2.0
        coefficients.kD shouldBe 3.0
      }

      test("values are mutable") {
        val coefficients = PIDCoefficients(1.0)
        coefficients.kP = 5.0
        coefficients.kI = 6.0
        coefficients.kD = 7.0
        coefficients.kP shouldBe 5.0
        coefficients.kI shouldBe 6.0
        coefficients.kD shouldBe 7.0
      }
    }

    context("PIDController construction") {
      test("can be constructed with coefficients object") {
        val coefficients = PIDCoefficients(1.0, 2.0, 3.0)
        val controller = PIDController(coefficients)
        controller.coefficients shouldBe coefficients
      }

      test("can be constructed with individual values") {
        val controller = PIDController(1.0, 2.0, 3.0)
        controller.coefficients.kP shouldBe 1.0
        controller.coefficients.kI shouldBe 2.0
        controller.coefficients.kD shouldBe 3.0
      }

      test("default values work") {
        val controller = PIDController(1.0)
        controller.coefficients.kP shouldBe 1.0
        controller.coefficients.kI shouldBe 0.0
        controller.coefficients.kD shouldBe 0.0
        controller.resetIntegralOnZeroCrossover shouldBe true
      }

      test("resetIntegralOnZeroCrossover can be disabled") {
        val controller = PIDController(1.0, resetIntegralOnZeroCrossover = false)
        controller.resetIntegralOnZeroCrossover shouldBe false
      }
    }

    context("PIDController P term") {
      test("proportional output is kP * error") {
        val timeSource = TestTimeSource()
        val controller = PIDController(2.0, 0.0, 0.0)

        val output = controller.calculate(
          timeSource.markNow(),
          error = 5.0,
          errorDerivative = 0.0,
        )
        output shouldBe (2.0 * 5.0 plusOrMinus 0.001)
      }

      test("negative error produces negative output") {
        val timeSource = TestTimeSource()
        val controller = PIDController(2.0, 0.0, 0.0)

        val output = controller.calculate(
          timeSource.markNow(),
          error = -5.0,
          errorDerivative = 0.0,
        )
        output shouldBe (-10.0 plusOrMinus 0.001)
      }
    }

    context("PIDController D term") {
      test("derivative output uses provided errorDerivative") {
        val timeSource = TestTimeSource()
        val controller = PIDController(0.0, 0.0, 3.0)

        val output = controller.calculate(
          timeSource.markNow(),
          error = 0.0,
          errorDerivative = 2.0,
        )
        output shouldBe (6.0 plusOrMinus 0.001)
      }

      test("derivative output is kD * errorDerivative") {
        val timeSource = TestTimeSource()
        val controller = PIDController(0.0, 0.0, 5.0)

        val output = controller.calculate(
          timeSource.markNow(),
          error = 0.0,
          errorDerivative = -3.0,
        )
        output shouldBe (-15.0 plusOrMinus 0.001)
      }
    }

    context("PIDController combined terms") {
      test("P and D terms combine correctly") {
        val timeSource = TestTimeSource()
        val controller = PIDController(2.0, 0.0, 3.0)

        // P: 2 * 5 = 10, D: 3 * 2 = 6, Total: 16
        val output = controller.calculate(
          timeSource.markNow(),
          error = 5.0,
          errorDerivative = 2.0,
        )
        output shouldBe (16.0 plusOrMinus 0.001)
      }
    }

    context("PIDController reset") {
      test("reset clears internal state") {
        val timeSource = TestTimeSource()
        val controller = PIDController(1.0, 1.0, 1.0)

        // Make a calculation to set internal state
        controller.calculate(timeSource.markNow(), error = 5.0, errorDerivative = 1.0)
        timeSource += 10.milliseconds

        // Reset
        controller.reset()

        // After reset, should behave like a fresh controller
        val output = controller.calculate(
          timeSource.markNow(),
          error = 5.0,
          errorDerivative = 1.0,
        )
        // First call after reset: P = 5, I = 0 (no time passed since reset), D = 1
        output shouldBe (6.0 plusOrMinus 0.001)
      }
    }

    context("PIDController reference/measured overloads") {
      test("calculate with reference and measured computes error correctly") {
        val timeSource = TestTimeSource()
        val controller = PIDController(2.0, 0.0, 0.0)

        // reference - measured = 10 - 3 = 7, P = 2 * 7 = 14
        // Pass 0.0 for measuredDerivative since we're only testing P term
        val output = controller.calculate(
          timeSource.markNow(),
          reference = 10.0,
          measured = 3.0,
          measuredDerivative = 0.0,
        )
        output shouldBe (14.0 plusOrMinus 0.001)
      }

      test("calculate with reference and measured derivatives") {
        val timeSource = TestTimeSource()
        val controller = PIDController(0.0, 0.0, 3.0)

        // errorDerivative = refDeriv - measDeriv = 5 - 2 = 3
        // D = 3 * 3 = 9
        val output = controller.calculate(
          timeSource.markNow(),
          reference = 0.0,
          measured = 0.0,
          referenceDerivative = 5.0,
          measuredDerivative = 2.0,
        )
        output shouldBe (9.0 plusOrMinus 0.001)
      }
    }

    context("PIDController continuous input") {
      test("enableContinuousInput sets properties") {
        val controller = PIDController(1.0)
        controller.isContinuousInputEnabled shouldBe false

        controller.enableContinuousInput(-180.0, 180.0)
        controller.isContinuousInputEnabled shouldBe true
      }

      test("disableContinuousInput clears property") {
        val controller = PIDController(1.0)
        controller.enableContinuousInput(-180.0, 180.0)
        controller.disableContinuousInput()
        controller.isContinuousInputEnabled shouldBe false
      }

      test("wraps positive error that crosses maximum bound") {
        val timeSource = TestTimeSource()
        val controller = PIDController(1.0, 0.0, 0.0)
        controller.enableContinuousInput(-180.0, 180.0)

        // reference = 170, measured = -170
        // normal error = 340, wrapped error = -20
        // output = 1.0 * -20 = -20
        val output = controller.calculate(
          timeSource.markNow(),
          reference = 170.0,
          measured = -170.0,
          measuredDerivative = 0.0,
        )
        output shouldBe (-20.0 plusOrMinus 0.001)
      }

      test("wraps negative error that crosses minimum bound") {
        val timeSource = TestTimeSource()
        val controller = PIDController(1.0, 0.0, 0.0)
        controller.enableContinuousInput(-180.0, 180.0)

        // reference = -170, measured = 170
        // normal error = -340, wrapped error = 20
        // output = 1.0 * 20 = 20
        val output = controller.calculate(
          timeSource.markNow(),
          reference = -170.0,
          measured = 170.0,
          measuredDerivative = 0.0,
        )
        output shouldBe (20.0 plusOrMinus 0.001)
      }

      test("does not wrap error within bounds") {
        val timeSource = TestTimeSource()
        val controller = PIDController(1.0, 0.0, 0.0)
        controller.enableContinuousInput(-180.0, 180.0)

        // reference = 50, measured = -50
        // normal error = 100, which is within [-180, 180]
        // output = 1.0 * 100 = 100
        val output = controller.calculate(
          timeSource.markNow(),
          reference = 50.0,
          measured = -50.0,
          measuredDerivative = 0.0,
        )
        output shouldBe (100.0 plusOrMinus 0.001)
      }

      test("rejects a maximumInput not greater than minimumInput") {
        val controller = PIDController(1.0)
        shouldThrow<IllegalArgumentException> {
          controller.enableContinuousInput(180.0, -180.0)
        }
      }
    }

    context("PIDController integrator anti-windup") {
      test("errorSum is clamped to the configured integrator range") {
        val timeSource = TestTimeSource()
        val controller = PIDController(0.0, 1.0, 0.0, resetIntegralOnZeroCrossover = false)
        controller.setIntegratorRange(-1.0, 1.0)

        // First call just seeds lastTimestamp/lastError, no time has passed yet.
        controller.calculate(timeSource.markNow(), error = 10.0, errorDerivative = 0.0)

        // Accumulate a constant positive error over many ticks; without clamping this would
        // grow unbounded (10 * 1 = 10 per tick).
        var output = 0.0
        repeat(5) {
          timeSource += 1.seconds
          output = controller.calculate(timeSource.markNow(), error = 10.0, errorDerivative = 0.0)
        }

        // Output is kI * errorSum, so it should be clamped to kI * maximumIntegral = 1.0.
        output shouldBe (1.0 plusOrMinus 0.001)
      }

      test("rejects a maximumIntegral not greater than minimumIntegral") {
        val controller = PIDController(1.0)
        shouldThrow<IllegalArgumentException> {
          controller.setIntegratorRange(1.0, -1.0)
        }
      }
    }
  })
