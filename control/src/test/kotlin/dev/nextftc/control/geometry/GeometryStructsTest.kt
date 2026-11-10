/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.geometry

import dev.nextftc.units.inches
import dev.nextftc.units.inchesPerSecond
import dev.nextftc.units.inchesPerSecondSquared
import dev.nextftc.units.meters
import dev.nextftc.units.metersPerSecond
import dev.nextftc.units.metersPerSecondSquared
import dev.nextftc.units.radiansPerSecond
import dev.nextftc.units.radiansPerSecondSquared
import dev.nextftc.util.struct.StructSchema
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeometryStructsTest :
  FunSpec({
    val epsilon = 1e-9

    fun <T> pack(schema: StructSchema<T>, value: T): ByteBuffer {
      val buffer = ByteBuffer.allocate(schema.size).order(ByteOrder.LITTLE_ENDIAN)
      schema.pack(buffer, value)
      buffer.flip()
      return buffer
    }

    test("schemas match WPILib's type names, layouts and sizes") {
      Vector2d.translation2dStruct.typeName shouldBe "Translation2d"
      Vector2d.translation2dStruct.schema shouldBe "float64 x; float64 y"

      Rotation2d.struct.typeName shouldBe "Rotation2d"
      Rotation2d.struct.schema shouldBe "float64 value"
      Rotation2d.struct.size shouldBe 8

      Pose2d.struct.typeName shouldBe "Pose2d"
      Pose2d.struct.schema shouldBe "Translation2d translation; Rotation2d rotation"
      Pose2d.struct.size shouldBe 24

      Transform2d.struct.schema shouldBe "Translation2d translation; Rotation2d rotation"
      Twist2d.struct.schema shouldBe "float64 dx; float64 dy; float64 dtheta"
      ChassisVelocities.struct.schema shouldBe "float64 vx; float64 vy; float64 omega"
      ChassisAccelerations.struct.schema shouldBe "float64 ax; float64 ay; float64 alpha"
    }

    test("a pose is written in meters and radians") {
      val buffer = pack(Pose2d.struct, Pose2d(100.0.inches, 0.0.inches, Rotation2d.exp(Math.PI / 2)))

      buffer.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (0.0 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (Math.PI / 2 plusOrMinus epsilon)
    }

    test("a transform and a twist are written in meters and radians") {
      val transform = pack(
        Transform2d.struct,
        Transform2d(Vector2d(1.0.meters, 2.0.meters), Rotation2d.exp(0.5)),
      )
      transform.getDouble() shouldBe (1.0 plusOrMinus epsilon)
      transform.getDouble() shouldBe (2.0 plusOrMinus epsilon)
      transform.getDouble() shouldBe (0.5 plusOrMinus epsilon)

      val twist = pack(Twist2d.struct, Twist2d(Vector2d(3.0.meters, 4.0.meters), 0.25))
      twist.getDouble() shouldBe (3.0 plusOrMinus epsilon)
      twist.getDouble() shouldBe (4.0 plusOrMinus epsilon)
      twist.getDouble() shouldBe (0.25 plusOrMinus epsilon)
    }

    test("chassis velocities are written in meters per second and radians per second") {
      val buffer = pack(
        ChassisVelocities.struct,
        ChassisVelocities(Vector2d(1.0.metersPerSecond, 100.0.inchesPerSecond), 2.0.radiansPerSecond),
      )

      buffer.getDouble() shouldBe (1.0 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (2.0 plusOrMinus epsilon)
    }

    test("field-frame velocities and accelerations use the chassis layouts") {
      PoseVelocity2d.struct.typeName shouldBe ChassisVelocities.struct.typeName
      PoseVelocity2d.struct.schema shouldBe ChassisVelocities.struct.schema
      PoseAcceleration2d.struct.typeName shouldBe ChassisAccelerations.struct.typeName
      PoseAcceleration2d.struct.schema shouldBe ChassisAccelerations.struct.schema

      val velocity = pack(
        PoseVelocity2d.struct,
        PoseVelocity2d(Vector2d(1.0.metersPerSecond, 100.0.inchesPerSecond), 2.0.radiansPerSecond),
      )
      velocity.getDouble() shouldBe (1.0 plusOrMinus epsilon)
      velocity.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      velocity.getDouble() shouldBe (2.0 plusOrMinus epsilon)

      val acceleration = pack(
        PoseAcceleration2d.struct,
        PoseAcceleration2d(
          Vector2d(1.0.metersPerSecondSquared, 100.0.inchesPerSecondSquared),
          3.0.radiansPerSecondSquared,
        ),
      )
      acceleration.getDouble() shouldBe (1.0 plusOrMinus epsilon)
      acceleration.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      acceleration.getDouble() shouldBe (3.0 plusOrMinus epsilon)
    }
  })
