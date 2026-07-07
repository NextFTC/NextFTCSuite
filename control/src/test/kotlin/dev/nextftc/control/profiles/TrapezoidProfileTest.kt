/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.profiles

import dev.nextftc.control.model.MotionState
import dev.nextftc.units.Measure
import dev.nextftc.units.Meters
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.ToleranceMatcher
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

class TrapezoidProfileTest :
  FunSpec({
    val tolerance = 1e-6

    context("TrapezoidProfileConstraints") {
      test("should create valid constraints with positive values") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        constraints.maxVelocity.magnitude shouldBe 5.0
        constraints.maxAcceleration.magnitude shouldBe 2.0
      }

      test("should accept zero values") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 0.0,
          maxAcceleration = 0.0,
        )
        constraints.maxVelocity.magnitude shouldBe 0.0
        constraints.maxAcceleration.magnitude shouldBe 0.0
      }

      test("should throw exception for negative maxVelocity") {
        shouldThrow<IllegalArgumentException> {
          TrapezoidProfileConstraints.linear(maxVelocity = -1.0, maxAcceleration = 2.0)
        }
      }

      test("should throw exception for negative maxAcceleration") {
        shouldThrow<IllegalArgumentException> {
          TrapezoidProfileConstraints.linear(maxVelocity = 5.0, maxAcceleration = -2.0)
        }
      }
    }

    context("TrapezoidProfile basic functionality") {
      test("should start at initial state") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        val result = profile.calculate(0.seconds, initial, goal)

        result.position.magnitude shouldBe
          (initial.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe
          (initial.velocity.magnitude plusOrMinus tolerance)
      }

      test("should reach goal state at end of profile") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val result = profile.calculate(totalTime.seconds, initial, goal)

        result.position.magnitude shouldBe (goal.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe (goal.velocity.magnitude plusOrMinus tolerance)
      }

      test("should respect maximum velocity constraint") {
        val maxVelocity = 5.0
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = maxVelocity,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 50.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        // Sample the profile at multiple points
        for (i in 0..100) {
          val t = (totalTime * i / 100.0).seconds
          val state = profile.calculate(t, initial, goal)
          abs(state.velocity.magnitude) shouldBeLessThanOrEqual (maxVelocity + tolerance)
        }
      }

      test("should respect maximum acceleration constraint") {
        val maxAcceleration = 2.0
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = maxAcceleration,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 50.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        // Sample the profile at multiple points
        for (i in 0..100) {
          val t = (totalTime * i / 100.0).seconds
          val state = profile.calculate(t, initial, goal)
          abs(state.acceleration.magnitude) shouldBeLessThanOrEqual
            (maxAcceleration + tolerance)
        }
      }
    }

    context("TrapezoidProfile motion types") {
      test("should generate full trapezoidal profile for long distance") {
        val constraints = TrapezoidProfileConstraints.linear(5.0, 2.0)
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 50.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        // Check that we reach max velocity somewhere in the middle
        var reachedMaxVelocity = false
        for (i in 0..100) {
          val t = (totalTime * i / 100.0).seconds
          val state = profile.calculate(t, initial, goal)
          if (abs(state.velocity.magnitude - constraints.maxVelocity.magnitude) <
            tolerance
          ) {
            reachedMaxVelocity = true
            break
          }
        }
        reachedMaxVelocity shouldBe true
      }

      test("should generate triangular profile for short distance") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 10.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 5.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        // Check that we never reach max velocity
        var reachedMaxVelocity = false
        for (i in 0..100) {
          val t = (totalTime * i / 100.0).seconds
          val state = profile.calculate(t, initial, goal)
          if (abs(state.velocity.magnitude - constraints.maxVelocity.magnitude) <
            tolerance
          ) {
            reachedMaxVelocity = true
            break
          }
        }
        reachedMaxVelocity shouldBe false
      }

      test("should handle backward motion") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 10.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 0.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val result = profile.calculate(totalTime.seconds, initial, goal)

        result.position.magnitude shouldBe (goal.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe (goal.velocity.magnitude plusOrMinus tolerance)
      }
    }

    context("TrapezoidProfile with non-zero initial velocity") {
      test("should handle positive initial velocity in forward direction") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 2.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val result = profile.calculate(totalTime.seconds, initial, goal)

        result.position.magnitude shouldBe (goal.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe (goal.velocity.magnitude plusOrMinus tolerance)
      }

      test("should handle positive initial velocity in backward direction") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 10.0, velocity = 2.0)
        val goal = MotionState(Meters, position = 0.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val result = profile.calculate(totalTime.seconds, initial, goal)

        result.position.magnitude shouldBe (goal.position.magnitude plusOrMinus tolerance)
      }
    }

    context("TrapezoidProfile with non-zero goal velocity") {
      test("should reach non-zero goal velocity") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 3.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val result = profile.calculate(totalTime.seconds, initial, goal)

        result.position.magnitude shouldBe (goal.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe (goal.velocity.magnitude plusOrMinus tolerance)
      }
    }

    context("TrapezoidProfile edge cases") {
      test("should handle zero distance movement") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 5.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 5.0, velocity = 0.0)

        val result = profile.calculate(0.seconds, initial, goal)

        result.position.magnitude shouldBe
          (initial.position.magnitude plusOrMinus tolerance)
        result.velocity.magnitude shouldBe
          (initial.velocity.magnitude plusOrMinus tolerance)
      }

      test("should handle already at goal") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 10.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        totalTime shouldBe (0.0 plusOrMinus tolerance)
      }

      test("should clamp initial velocity exceeding max velocity") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 10.0)
        val goal = MotionState(Meters, position = 20.0, velocity = 0.0)

        val result = profile.calculate(0.seconds, initial, goal)

        // Should clamp to max velocity
        abs(result.velocity.magnitude) shouldBeLessThanOrEqual
          (constraints.maxVelocity.magnitude + tolerance)
      }
    }

    context("TrapezoidProfile isFinished") {
      test("should not be finished at start") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)

        profile.isFinished(0.0) shouldBe false
      }

      test("should be finished at total time") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        profile.isFinished(totalTime) shouldBe true
      }

      test("should be finished after total time") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime

        profile.isFinished(totalTime + 1.0) shouldBe true
      }
    }

    context("TrapezoidProfile timeLeftUntil") {
      test("should return zero time for current position") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val timeLeft = profile.timeLeftUntil(0.0)

        timeLeft shouldBe (0.0 plusOrMinus tolerance)
      }

      test("should return positive time for target ahead") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val timeLeft = profile.timeLeftUntil(5.0)

        timeLeft shouldBeGreaterThan 0.0
      }

      test("should return negative time for target behind") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 5.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val timeLeft = profile.timeLeftUntil(3.0)

        timeLeft shouldBeLessThan 0.0
      }
    }

    context("TrapezoidProfile continuity") {
      test("position should be continuous throughout profile") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 20.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val dt = 0.01

        var previousState = profile.calculate(0.seconds, initial, goal)
        for (i in 1..((totalTime / dt).toInt())) {
          val t = (i * dt).seconds
          val currentState = profile.calculate(t, initial, goal)

          // Position should always increase (or stay same)
          currentState.position.magnitude shouldBeGreaterThanOrEqual
            (previousState.position.magnitude - tolerance)

          previousState = currentState
        }
      }

      test("velocity should be continuous throughout profile") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 20.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime = profile.totalTime
        val dt = 0.01

        var previousState = profile.calculate(0.seconds, initial, goal)
        for (i in 1..((totalTime / dt).toInt())) {
          val t = (i * dt).seconds
          val currentState = profile.calculate(t, initial, goal)

          // Velocity change should be bounded by acceleration * dt
          val velocityChange =
            abs(currentState.velocity.magnitude - previousState.velocity.magnitude)
          velocityChange shouldBeLessThanOrEqual
            (constraints.maxAcceleration.magnitude * dt + tolerance)

          previousState = currentState
        }
      }
    }

    context("TrapezoidProfile totalTime property") {
      test("should have positive total time for non-zero movement") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)

        profile.totalTime shouldBeGreaterThan 0.0
      }

      test("should have consistent total time across multiple calls") {
        val constraints = TrapezoidProfileConstraints.linear(
          maxVelocity = 5.0,
          maxAcceleration = 2.0,
        )
        val profile = TrapezoidProfile(constraints)
        val initial = MotionState(Meters, position = 0.0, velocity = 0.0)
        val goal = MotionState(Meters, position = 10.0, velocity = 0.0)

        profile.calculate(0.seconds, initial, goal)
        val totalTime1 = profile.totalTime

        profile.calculate(1.seconds, initial, goal)
        val totalTime2 = profile.totalTime

        totalTime1 shouldBe (totalTime2 plusOrMinus tolerance)
      }
    }
  })

infix fun Measure<*>.plusOrMinus(tolerance: Double): ToleranceMatcher =
  ToleranceMatcher(this.magnitude, tolerance)
