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
import dev.nextftc.util.struct.StructSchema
import dev.nextftc.util.struct.StructSchemaRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PedroStructsTest :
  FunSpec({
    val epsilon = 1e-9

    fun <T> pack(schema: StructSchema<T>, value: T): ByteBuffer {
      val buffer = ByteBuffer.allocate(schema.size).order(ByteOrder.LITTLE_ENDIAN)
      schema.pack(buffer, value)
      buffer.flip()
      return buffer
    }

    test("a pose matches WPILib's Pose2d layout") {
      PedroStructs.pose.typeName shouldBe "Pose2d"
      PedroStructs.pose.schema shouldBe "Translation2d translation; Rotation2d rotation"
      PedroStructs.pose.size shouldBe 24
    }

    test("a pose is written in meters and radians") {
      val buffer = pack(PedroStructs.pose, Pose(100.0, 50.0, Math.PI / 2))

      buffer.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (1.27 plusOrMinus epsilon)
      buffer.getDouble() shouldBe (Math.PI / 2 plusOrMinus epsilon)
    }

    test("a vector and a twist are written in meters") {
      val vector = pack(PedroStructs.vector2d, Vector2D.cartesian(100.0, 200.0))
      vector.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      vector.getDouble() shouldBe (5.08 plusOrMinus epsilon)

      val twist = pack(PedroStructs.twist, Twist(100.0, 0.0, 3.0))
      twist.getDouble() shouldBe (2.54 plusOrMinus epsilon)
      twist.getDouble() shouldBe (0.0 plusOrMinus epsilon)
      twist.getDouble() shouldBe (3.0 plusOrMinus epsilon)
    }

    test("registerAll lets a registry find every type") {
      val registry = StructSchemaRegistry()
      PedroStructs.registerAll(registry)

      registry.schemaOf<Pose>() shouldBe PedroStructs.pose
      registry.schemaOf<Twist>() shouldBe PedroStructs.twist
    }
  })
