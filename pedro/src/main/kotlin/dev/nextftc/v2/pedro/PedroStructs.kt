/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.v2.pedro

import com.pedropathing.math.Pose
import com.pedropathing.math.Twist
import com.pedropathing.math.Vector2D
import com.pedropathing.math.Velocity
import dev.nextftc.telemetry.Telemetry
import dev.nextftc.units.Meters
import dev.nextftc.units.inches
import dev.nextftc.util.struct.StructSchema
import dev.nextftc.util.struct.StructSchemaRegistry
import dev.nextftc.util.struct.structSchema
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [StructSchema]s for Pedro Pathing's geometry types, so they can be logged like the `control` ones.
 *
 * Pedro works in inches, so distances and speeds are written in meters to match WPILib's layouts.
 * Positions stay in Pedro's coordinate frame; no offset is applied.
 *
 * | Pedro type | Struct type | Fields |
 * |---|---|---|
 * | [Pose] | `Pose2d` | `Translation2d translation; Rotation2d rotation` |
 * | [Vector2D] | `Translation2d` | `x`, `y` in meters |
 * | [Twist] (robot frame) | `ChassisVelocities` | `vx`, `vy` in m/s, `omega` in rad/s |
 * | [Velocity] (field frame) | `ChassisVelocities` | `vx`, `vy` in m/s, `omega` in rad/s |
 *
 * [PedroMechanism] registers these with [Telemetry] when it is created. To use another registry, call
 * [registerAll].
 */
object PedroStructs {
  private val translation2dOfPose: StructSchema<Pose> = structSchema("Translation2d") {
    float64("x") { it.x().inches.into(Meters) }
    float64("y") { it.y().inches.into(Meters) }
  }

  private val rotation2dOfHeading: StructSchema<Double> = structSchema("Rotation2d") {
    float64("value") { it }
  }

  /** A [Vector2D] as a WPILib `Translation2d`, in meters. */
  @JvmField
  val vector2d: StructSchema<Vector2D> = structSchema("Translation2d") {
    float64("x") { it.x().inches.into(Meters) }
    float64("y") { it.y().inches.into(Meters) }
  }

  /** A [Pose] as a WPILib `Pose2d`: its position in meters and its heading in radians. */
  @JvmField
  val pose: StructSchema<Pose> = structSchema("Pose2d") {
    struct("translation", translation2dOfPose) { it }
    struct("rotation", rotation2dOfHeading) { it.heading() }
  }

  /** A robot-frame [Twist] as WPILib `ChassisVelocities`, in meters and radians per second. */
  @JvmField
  val twist: StructSchema<Twist> = structSchema<Twist>("ChassisVelocities") {
    float64("vx") { it.vx.inches.into(Meters) }
    float64("vy") { it.vy.inches.into(Meters) }
    float64("omega") { it.omega }
  }

  /** A field-frame [Velocity] as WPILib `ChassisVelocities`, in meters and radians per second. */
  @JvmField
  val velocity: StructSchema<Velocity> = structSchema<Velocity>("ChassisVelocities") {
    float64("vx") { it.vx.inches.into(Meters) }
    float64("vy") { it.vy.inches.into(Meters) }
    float64("omega") { it.omega }
  }

  /** Registers every schema in this object with [registry], so these types can be logged. */
  fun registerAll(registry: StructSchemaRegistry) {
    registry.register(Pose::class, pose)
    registry.register(Vector2D::class, vector2d)
    registry.register(Twist::class, twist)
    registry.register(Velocity::class, velocity)
  }
}
