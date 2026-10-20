/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

import dev.nextftc.telemetry.javatypes.JavaTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.nio.ByteBuffer
import java.nio.ByteOrder

@StructOrder("b")
private class KotlinOrdered(val a: Double, val b: Double)

private fun <T> StructSchema<T>.packedBuffer(value: T): ByteBuffer =
  ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).also { pack(it, value) }

class JavaStructSchemaTest :
  FunSpec({
    test("public fields are reflected") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.PublicFields>()

      schema.schema shouldBe "float64 x; float64 y"
      val buffer = schema.packedBuffer(JavaTypes.PublicFields())
      buffer.getDouble(0) shouldBe 1.0
      buffer.getDouble(8) shouldBe 2.0
    }

    test("getters over private fields are reflected, and their values are read") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Getters>()

      schema.schema shouldBe "float64 x; float64 y"
      val buffer = schema.packedBuffer(JavaTypes.Getters(3.0, 4.0))
      buffer.getDouble(0) shouldBe 3.0
      buffer.getDouble(8) shouldBe 4.0
    }

    test("getters are ordered alphabetically, since Java doesn't keep method order") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Unordered>()

      schema.schema shouldBe "float64 a; float64 m; float64 z"
      val buffer = schema.packedBuffer(JavaTypes.Unordered())
      buffer.getDouble(0) shouldBe 1.0
      buffer.getDouble(8) shouldBe 2.0
      buffer.getDouble(16) shouldBe 3.0
    }

    test("StructOrder lists fields first, then the rest alphabetically") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Ordered>()

      schema.schema shouldBe "float64 z; float64 a; float64 b"
      val buffer = schema.packedBuffer(JavaTypes.Ordered())
      buffer.getDouble(0) shouldBe 3.0
      buffer.getDouble(8) shouldBe 1.0
    }

    test("StructOrder also overrides a Kotlin class's constructor order") {
      StructSchemaRegistry().schemaOf<KotlinOrdered>().schema shouldBe "float64 b; float64 a"
    }

    test("StructOrder naming a field that doesn't exist is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<JavaTypes.BadOrder>() }
        .message!! shouldContain "missing"
    }

    test("boolean getters may start with is or get, and is only counts for booleans") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Flags>()

      schema.schema shouldBe "bool active; bool boxed; bool enabled"
      val buffer = schema.packedBuffer(JavaTypes.Flags())
      buffer.get(0) shouldBe 1.toByte()
      buffer.get(1) shouldBe 0.toByte()
      buffer.get(2) shouldBe 1.toByte()
    }

    test("names follow the JavaBeans convention for acronyms") {
      StructSchemaRegistry().schemaOf<JavaTypes.Acronyms>().schema shouldBe
        "float64 RPM; float64 name"
    }

    test("boxed return types are reflected as their primitives") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Boxed>()

      schema.schema shouldBe "int32 count; float64 value"
      val buffer = schema.packedBuffer(JavaTypes.Boxed())
      buffer.getInt(0) shouldBe 3
      buffer.getDouble(4) shouldBe 1.5
    }

    test("a boxed getter that returns null fails to pack, naming the field") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.NullBoxed>()

      shouldThrow<IllegalStateException> { schema.packedBuffer(JavaTypes.NullBoxed()) }
        .message!! shouldContain "NullBoxed.value"
    }

    test("StructIgnore and StructSize work on Java fields and getters") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Annotated>()

      schema.schema shouldBe "float64 kept; float64 pair[2]"
      schema.size shouldBe 24
      val buffer = schema.packedBuffer(JavaTypes.Annotated())
      buffer.getDouble(0) shouldBe 1.0
      buffer.getDouble(8) shouldBe 4.0
      buffer.getDouble(16) shouldBe 5.0
    }

    test("a getter that returns the wrong array length fails to pack") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.WrongSize>()

      shouldThrow<IllegalArgumentException> { schema.packedBuffer(JavaTypes.WrongSize()) }
    }

    test("an array getter without StructSize is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<JavaTypes.MissingSize>() }
        .message!! shouldContain "@StructSize"
    }

    test("a public field wins over a getter of the same name") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.FieldAndGetter>()

      schema.schema shouldBe "float64 x"
      schema.packedBuffer(JavaTypes.FieldAndGetter()).getDouble(0) shouldBe 1.0
    }

    test("methods that aren't getters are skipped") {
      StructSchemaRegistry().schemaOf<JavaTypes.NotGetters>().schema shouldBe "float64 x"
    }

    test("inherited getters are included") {
      StructSchemaRegistry().schemaOf<JavaTypes.Derived>().schema shouldBe "float64 x; float64 y"
    }

    test("a class nested in a getter becomes a nested struct, named by StructName") {
      val schema = StructSchemaRegistry().schemaOf<JavaTypes.Pose>()

      schema.schema shouldBe "float64 heading; Translation2d translation"
      schema.size shouldBe 24
      schema.nested.single().typeName shouldBe "Translation2d"
      val buffer = schema.packedBuffer(JavaTypes.Pose(JavaTypes.Vec(1.0, 2.0), 0.5))
      buffer.getDouble(0) shouldBe 0.5
      buffer.getDouble(8) shouldBe 1.0
      buffer.getDouble(16) shouldBe 2.0
    }

    test("a package-private class with public getters can be read") {
      @Suppress("UNCHECKED_CAST")
      val schema = StructSchemaRegistry().schemaOf(JavaTypes.hiddenClass() as Class<Any>)

      schema.typeName shouldBe "Hidden"
      schema.packedBuffer(JavaTypes.hidden()).getDouble(0) shouldBe 1.0
    }

    test("unsupported getter types are rejected, naming the field") {
      shouldThrow<IllegalArgumentException> {
        StructSchemaRegistry().schemaOf<JavaTypes.HasStringGetter>()
      }
        .message!! shouldContain "HasStringGetter.name"
      shouldThrow<IllegalArgumentException> {
        StructSchemaRegistry().schemaOf<JavaTypes.HasListGetter>()
      }
        .message!! shouldContain "HasListGetter.items"
      shouldThrow<IllegalArgumentException> {
        StructSchemaRegistry().schemaOf<JavaTypes.HasCharGetter>()
      }
        .message!! shouldContain "primitive"
      shouldThrow<IllegalArgumentException> {
        StructSchemaRegistry().schemaOf<JavaTypes.HasIntArrayGetter>()
      }
        .message!! shouldContain "array"
    }

    test("a class with no public fields or getters is rejected") {
      shouldThrow<IllegalArgumentException> { StructSchemaRegistry().schemaOf<JavaTypes.NoGetters>() }
    }
  })
