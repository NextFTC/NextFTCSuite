/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder

private data class Translation(val x: Double, val y: Double)

private data class Heading(val radians: Double)

private data class Pose(val translation: Translation, val heading: Heading)

private val translationSchema =
  structSchema<Translation>("Translation2d") {
    float64("x") { it.x }
    float64("y") { it.y }
  }

private val headingSchema = structSchema<Heading>("Rotation2d") { float64("value") { it.radians } }

private val poseSchema =
  structSchema<Pose>("Pose2d") {
    struct("translation", translationSchema) { it.translation }
    struct("rotation", headingSchema) { it.heading }
  }

private fun <T> StructSchema<T>.packed(value: T): ByteBuffer =
  ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).also { pack(it, value) }

class StructSchemaTest :
  FunSpec({
    test("schema text matches WPILib's Pose2d layout") {
      translationSchema.schema shouldBe "float64 x; float64 y"
      headingSchema.schema shouldBe "float64 value"
      poseSchema.schema shouldBe "Translation2d translation; Rotation2d rotation"
    }

    test("type strings and schema entry name") {
      poseSchema.typeString shouldBe "struct:Pose2d"
      poseSchema.arrayTypeString shouldBe "struct:Pose2d[]"
      poseSchema.schemaTopicName shouldBe "/.schema/struct:Pose2d"
    }

    test("size is the sum of the fields, including nested structs") {
      translationSchema.size shouldBe 16
      poseSchema.size shouldBe 24
    }

    test("nested lists direct dependencies once") {
      poseSchema.nested.map { it.typeName }.shouldContainExactly("Translation2d", "Rotation2d")
    }

    test("packs nested fields little-endian in declaration order") {
      val buffer = poseSchema.packed(Pose(Translation(1.0, 2.0), Heading(3.0)))

      buffer.position() shouldBe 24
      buffer.getDouble(0) shouldBe 1.0
      buffer.getDouble(8) shouldBe 2.0
      buffer.getDouble(16) shouldBe 3.0
      // 1.0 is 0x3FF0000000000000, so little-endian puts the 0xF0 and 0x3F bytes last.
      buffer.get(6) shouldBe 0xF0.toByte()
      buffer.get(7) shouldBe 0x3F.toByte()
    }

    test("every primitive packs to its declared width") {
      val schema =
        structSchema<Unit>("Primitives") {
          boolean("a") { true }
          int8("b") { 1 }
          int16("c") { 2 }
          int32("d") { 3 }
          int64("e") { 4 }
          float32("f") { 5f }
          float64("g") { 6.0 }
        }

      schema.schema shouldBe
        "bool a; int8 b; int16 c; int32 d; int64 e; float32 f; float64 g"
      schema.size shouldBe 1 + 1 + 2 + 4 + 8 + 4 + 8
      schema.packed(Unit).position() shouldBe schema.size
    }

    test("fixed-size arrays declare their length and pack every element") {
      val schema =
        structSchema<DoubleArray>("Quad") { float64Array("values", 4) { it } }

      schema.schema shouldBe "float64 values[4]"
      schema.size shouldBe 32
      schema.packed(doubleArrayOf(1.0, 2.0, 3.0, 4.0)).getDouble(24) shouldBe 4.0
    }

    test("an array of the wrong length is rejected when packing") {
      val schema = structSchema<DoubleArray>("Quad") { float64Array("values", 4) { it } }

      shouldThrow<IllegalArgumentException> { schema.packed(doubleArrayOf(1.0)) }
    }

    test("a big-endian buffer is rejected") {
      val buffer = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)

      shouldThrow<IllegalArgumentException> { translationSchema.pack(buffer, Translation(0.0, 0.0)) }
    }

    test("duplicate field names are rejected") {
      shouldThrow<IllegalArgumentException> {
        structSchema<Unit>("Dupes") {
          float64("x") { 0.0 }
          float64("x") { 0.0 }
        }
      }
    }

    test("invalid names are rejected") {
      shouldThrow<IllegalArgumentException> {
        structSchema<Unit>("Bad Name") { float64("x") { 0.0 } }
      }
      shouldThrow<IllegalArgumentException> {
        structSchema<Unit>("Ok") { float64("1x") { 0.0 } }
      }
    }

    test("a struct with no fields is rejected") {
      shouldThrow<IllegalArgumentException> { structSchema<Unit>("Empty") {} }
    }

    test("nesting two different schemas with one name is rejected") {
      val other = structSchema<Unit>("Translation2d") { float32("x") { 0f } }

      shouldThrow<IllegalArgumentException> {
        structSchema<Unit>("Conflict") {
          struct("a", translationSchema) { Translation(0.0, 0.0) }
          struct("b", other) { }
        }
      }
    }
  })
