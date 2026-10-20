package dev.nextftc.robot

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class RobotScannerTest : FunSpec({
  test("robot creation failures keep the root cause and suggest missing-hardware checks") {
    val throwable = NoSuchElementException("No device named 'leftLiftMotor'")

    val message = RobotScanner.creationFailureMessage(TestRobot::class, throwable)

    message shouldContain "Failed to create an instance of"
    message shouldContain "No device named 'leftLiftMotor'"
    message shouldContain "hardware object"
    message shouldContain "RobotController.hardwareMap"
  }
})

private class TestRobot : NextRobot
