package dev.nextftc.robot.drive

import kotlin.math.absoluteValue
import kotlin.math.max

interface DriveKinematics {
    fun calculate(inputPowers: DoubleArray): DoubleArray
}

object Tankinematics : DriveKinematics{
    // Assume structure of inputPowers is [forwardPower, turnPower]
    override fun calculate(inputPowers: DoubleArray) {
        val (forward, turn) = inputPowers
        val left = forward + turn
        val right = forward - turn

    }
}

object MecanumKinematics : DriveKinematics{
    override fun calculate(inputPowers: DoubleArray): DoubleArray {
        val (forward, strafe, turn) = inputPowers
        val denominator = max(forward.absoluteValue + strafe.absoluteValue + turn.absoluteValue, 1.0)
        val frontLeft = (forward + strafe + turn) / denominator
        val frontRight = (forward - strafe - turn) / denominator
        val backLeft = (forward - strafe + turn) / denominator
        val backRight = (forward + strafe - turn) / denominator

        return doubleArrayOf(frontLeft, frontRight, backLeft, backRight)
    }
}