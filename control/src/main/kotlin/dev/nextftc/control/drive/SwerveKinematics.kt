/*
 * Copyright (c) FIRST and other WPILib contributors.
 * Open Source Software; you can modify and/or share it under the terms of
 * the WPILib BSD license file in the root directory of this project.
 *
 * Copyright (c) 2026 NextFTC Team
 * Portions of this file are original code or adaptations by the NextFTC Team.
 * Use of this source code is governed by an BSD-3-clause
 * license that can be found in the LICENSE.md file at the root of this repository or at
 * https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.drive

import dev.nextftc.control.geometry.ChassisVelocities
import dev.nextftc.control.geometry.Rotation2d
import dev.nextftc.control.geometry.Vector2d
import dev.nextftc.units.Meters
import dev.nextftc.linalg.DynamicMatrix
import dev.nextftc.linalg.DynamicVector
import dev.nextftc.units.MetersPerSecond
import dev.nextftc.units.RadiansPerSecond
import dev.nextftc.units.unittypes.DistanceUnit
import kotlin.math.hypot

/** A single swerve module's commanded velocity and angle. */
data class SwerveModuleVelocity(val velocity: Double, val angle: Rotation2d)

/**
 * Converts a desired chassis velocity into individual swerve module velocities and angles.
 *
 * @param moduleLocations The locations of each module relative to the physical center of the
 *   robot. Order determines the order of returned module velocities.
 */
class SwerveKinematics(vararg moduleLocations: Vector2d<DistanceUnit>) {

    private val numModules = moduleLocations.size
    private val moduleXs = DoubleArray(numModules) { moduleLocations[it].x.into(Meters) }
    private val moduleYs = DoubleArray(numModules) { moduleLocations[it].y.into(Meters) }

    private var moduleHeadings = Array(numModules) { Rotation2d.zero }

    private var prevCorX = 0.0
    private var prevCorY = 0.0
    private var inverseKinematics = buildInverseKinematics(0.0, 0.0)

    private fun buildInverseKinematics(corX: Double, corY: Double): DynamicMatrix {
        val m = DynamicMatrix.zero(rows = numModules * 2, cols = 3)
        for (i in 0 until numModules) {
            val rx = moduleXs[i] - corX
            val ry = moduleYs[i] - corY
            m[i * 2, 0] = 1.0; m[i * 2, 1] = 0.0; m[i * 2, 2] = -ry
            m[i * 2 + 1, 0] = 0.0; m[i * 2 + 1, 1] = 1.0; m[i * 2 + 1, 2] = rx
        }
        return m
    }

    /**
     * Performs inverse kinematics to return module velocities/angles from a desired chassis
     * velocity, optionally about a custom center of rotation (e.g. for evasive maneuvers).
     */
    @JvmOverloads
    fun toSwerveModuleVelocities(
        chassisVelocities: ChassisVelocities,
        centerOfRotationX: Double = 0.0,
        centerOfRotationY: Double = 0.0,
    ): Array<SwerveModuleVelocity> {
        val vx = chassisVelocities.linearVel.x.into(unit = MetersPerSecond)
        val vy = chassisVelocities.linearVel.y.into(unit = MetersPerSecond)
        val omega = chassisVelocities.angVel.into(unit = RadiansPerSecond)

        if (vx == 0.0 && vy == 0.0 && omega == 0.0) {
            return Array(numModules) { SwerveModuleVelocity(0.0, moduleHeadings[it]) }
        }

        if (centerOfRotationX != prevCorX || centerOfRotationY != prevCorY) {
            inverseKinematics = buildInverseKinematics(centerOfRotationX, centerOfRotationY)
            prevCorX = centerOfRotationX
            prevCorY = centerOfRotationY
        }

        val chassisVector = DynamicVector.of(vx, vy, omega)
        val moduleVelocitiesMatrix = inverseKinematics * chassisVector

        return Array(numModules) { i ->
            val x = moduleVelocitiesMatrix[i * 2, 0]
            val y = moduleVelocitiesMatrix[i * 2 + 1, 0]
            val velocity = hypot(x, y)
            val angle = if (velocity > 1e-6) Rotation2d(real = x, imag = y) else moduleHeadings[i]
            moduleHeadings[i] = angle
            SwerveModuleVelocity(velocity, angle)
        }
    }

    companion object {
        /**
         * Renormalizes module velocities if any exceed [attainableMaxVelocity], preserving the
         * ratio of velocities between modules (and therefore the direction of net motion).
         */
        @JvmStatic
        fun desaturateWheelVelocities(
            moduleVelocities: Array<SwerveModuleVelocity>,
            attainableMaxVelocity: Double,
        ): Array<SwerveModuleVelocity> {
            val realMaxVelocity = moduleVelocities.maxOf { kotlin.math.abs(it.velocity) }

            return if (realMaxVelocity > attainableMaxVelocity) {
                Array(moduleVelocities.size) { i ->
                    SwerveModuleVelocity(
                        moduleVelocities[i].velocity / realMaxVelocity * attainableMaxVelocity,
                        moduleVelocities[i].angle,
                    )
                }
            } else {
                moduleVelocities.copyOf()
            }
        }
    }
}