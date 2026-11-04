/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

import dev.nextftc.telemetry.wpilog.WPILogWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@StructName("Translation2d")
private data class Vec2(val x: Double, val y: Double)

@StructName("Rotation2d")
private data class Angle(val value: Double)

@StructName("Pose2d")
private data class PoseLike(val translation: Vec2, val rotation: Angle)

private data class Reversed(val z: Double, val a: Double)

private class WithBody(val c: Double) {
  val b = 1.0
  val a = 2.0
}

private data class Primitives(
  val a: Boolean,
  val b: Byte,
  val c: Short,
  val d: Int,
  val e: Long,
  val f: Float,
  val g: Double,
)

private data class Ignoring(val x: Double, @StructIgnore val label: String)

private class Computed(val x: Double) {
  @StructIgnore val doubled get() = x * 2
}

private data class WithArray(@StructSize(3) val values: DoubleArray)

private data class MissingSize(val values: DoubleArray)

private data class Nullable(val x: Double?)

private data class HasString(val name: String)

private data class HasList(val items: List<Double>)

private enum class Mode { A, B }

private data class HasEnum(val mode: Mode)

private interface Shape

private data class HasInterface(val shape: Shape)

private class CycleA(val b: CycleB)

private class CycleB(val a: CycleA)

private class Empty

private data class Holder(val v: Vec2)

private fun <T> StructSchema<T>.bytes(value: T): ByteArray =
  ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).also { pack(it, value) }.array()

class StructSchemaRegistryTest :
  FunSpec({
    test("reflects nested classes into WPILib's Pose2d layout") {
      val schema = StructSchemaRegistry().schemaOf<PoseLike>()

      schema.typeName shouldBe "Pose2d"
      schema.schema shouldBe "Translation2d translation; Rotation2d rotation"
      schema.size shouldBe 24
      schema.nested.map { it.typeName }.shouldContainExactly("Translation2d", "Rotation2d")
      schema.nested[0].schema shouldBe "float64 x; float64 y"
    }

    test("packs the same bytes as a hand-written schema") {
      val vec =
        structSchema<Vec2>("Translation2d") {
          float64("x") { it.x }
          float64("y") { it.y }
        }
      val angle = structSchema<Angle>("Rotation2d") { float64("value") { it.value } }
      val handWritten =
        structSchema<PoseLike>("Pose2d") {
          struct("translation", vec) { it.translation }
          struct("rotation", angle) { it.rotation }
        }
      val pose = PoseLike(Vec2(1.5, -2.0), Angle(0.25))

      StructSchemaRegistry().schemaOf<PoseLike>().bytes(pose) shouldBe handWritten.bytes(pose)
    }

    test("the name defaults to the simple class name") {
      StructSchemaRegistry().schemaOf<Reversed>().typeName shouldBe "Reversed"
    }

    test("constructor order is the wire order, not alphabetical") {
      StructSchemaRegistry().schemaOf<Reversed>().schema shouldBe "float64 z; float64 a"
    }

    test("properties outside the constructor follow it, alphabetically") {
      StructSchemaRegistry().schemaOf<WithBody>().schema shouldBe
        "float64 c; float64 a; float64 b"
    }

    test("every supported primitive maps to its struct type and packs its value") {
      val schema = StructSchemaRegistry().schemaOf<Primitives>()

      schema.schema shouldBe "bool a; int8 b; int16 c; int32 d; int64 e; float32 f; float64 g"
      val buffer = ByteBuffer.wrap(schema.bytes(Primitives(true, 2, 3, 4, 5L, 6f, 7.0)))
      buffer.order(ByteOrder.LITTLE_ENDIAN)
      buffer.get() shouldBe 1.toByte()
      buffer.get() shouldBe 2.toByte()
      buffer.getShort() shouldBe 3.toShort()
      buffer.getInt() shouldBe 4
      buffer.getLong() shouldBe 5L
      buffer.getFloat() shouldBe 6f
      buffer.getDouble() shouldBe 7.0
    }

    test("ignored properties are left out") {
      StructSchemaRegistry().schemaOf<Ignoring>().schema shouldBe "float64 x"
      StructSchemaRegistry().schemaOf<Computed>().schema shouldBe "float64 x"
    }

    test("a DoubleArray with a declared size becomes a fixed array") {
      val schema = StructSchemaRegistry().schemaOf<WithArray>()

      schema.schema shouldBe "float64 values[3]"
      ByteBuffer.wrap(schema.bytes(WithArray(doubleArrayOf(1.0, 2.0, 3.0))))
        .order(ByteOrder.LITTLE_ENDIAN)
        .getDouble(16) shouldBe 3.0
    }

    test("the same registry returns the same schema every time") {
      val registry = StructSchemaRegistry()

      (registry.schemaOf<PoseLike>() === registry.schemaOf<PoseLike>()) shouldBe true
      (registry.schemaOf<PoseLike>().nested[0] === registry.schemaOf<Vec2>()) shouldBe true
    }

    test("a registered schema is used for a nested property") {
      val custom =
        structSchema<Vec2>("Custom") {
          float32("x") { it.x.toFloat() }
          float32("y") { it.y.toFloat() }
        }
      val registry = StructSchemaRegistry()
      registry.register(Vec2::class, custom)

      val schema = registry.schemaOf<Holder>()

      schema.schema shouldBe "Custom v"
      schema.size shouldBe 8
      schema.nested.single() shouldBe custom
    }

    test("registering a type that already has a schema is rejected") {
      val registry = StructSchemaRegistry()
      registry.schemaOf<Vec2>()

      shouldThrow<IllegalArgumentException> {
        registry.register(Vec2::class, structSchema<Vec2>("Other") { float64("x") { it.x } })
      }
    }

    test("a nullable property is rejected, naming the property") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<Nullable>() }
        .message!! shouldContain "Nullable.x"
    }

    test("strings, collections, and enums are rejected, naming the property") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<HasString>() }
        .message!! shouldContain "HasString.name"
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<HasList>() }
        .message!! shouldContain "HasList.items"
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<HasEnum>() }
        .message!! shouldContain "HasEnum.mode"
    }

    test("a DoubleArray without a size is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<MissingSize>() }
        .message!! shouldContain "@StructSize"
    }

    test("an interface-typed property is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<HasInterface>() }
        .message!! shouldContain "HasInterface.shape"
    }

    test("cyclic nesting is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<CycleA>() }
        .message!! shouldContain "cyclic"
    }

    test("a class with no properties is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<Empty>() }
    }

    test("a failed reflection leaves the registry usable") {
      val registry = StructSchemaRegistry()

      shouldThrow<IllegalArgumentException> { registry.schemaOf<CycleA>() }

      registry.schemaOf<Vec2>().schema shouldBe "float64 x; float64 y"
    }

    test("a reflected schema logs through the writer") {
      val bytes = ByteArrayOutputStream()
      val schema = StructSchemaRegistry().schemaOf<PoseLike>()

      WPILogWriter(bytes).use {
        val id = it.startStructEntry("/Pose", schema)
        it.appendStruct(id, schema, PoseLike(Vec2(1.0, 2.0), Angle(3.0)))
      }

      val text = String(bytes.toByteArray(), Charsets.ISO_8859_1)
      text shouldContain "/.schema/struct:Pose2d"
      text shouldContain "Translation2d translation; Rotation2d rotation"
    }
  })
