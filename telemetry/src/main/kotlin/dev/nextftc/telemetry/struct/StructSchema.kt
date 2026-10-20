/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.struct

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Describes a WPILib struct: a fixed-size, packed, little-endian binary encoding of [T] together
 * with a text schema that tells readers how to decode it.
 *
 * This mirrors the shape of WPILib's `Struct<T>` interface, so a type that already has a WPILib
 * struct can be adapted by forwarding its members. A struct is logged as a raw value whose type
 * string is [typeString], plus one schema entry per type (see [schemaTopicName]) holding [schema].
 *
 * AdvantageScope recognizes WPILib's built-in types such as `Pose2d` by [typeName], so a schema
 * meant to be drawn on a field view must use the same name and field layout as WPILib's.
 */
interface StructSchema<T> {
  /** The struct's name without the `struct:` prefix, such as `Pose2d`. */
  val typeName: String

  /** The size in bytes of one packed value. Every value of this type packs to exactly this many. */
  val size: Int

  /**
   * The schema text, such as `double x; double y`. A field of another struct type refers to it by
   * its [typeName].
   */
  val schema: String

  /** The struct types referenced directly by [schema]. Types nested deeper are found through these. */
  val nested: List<StructSchema<*>>
    get() = emptyList()

  /** The type string for a single value, `struct:` followed by [typeName]. */
  val typeString: String
    get() = "struct:$typeName"

  /** The type string for a variable-length array of values, such as `struct:Pose2d[]`. */
  val arrayTypeString: String
    get() = "$typeString[]"

  /** The name of the entry that holds [schema], such as `/.schema/struct:Pose2d`. */
  val schemaTopicName: String
    get() = "/.schema/$typeString"

  /**
   * Writes [value] into [buffer] at its current position, in exactly [size] bytes.
   *
   * [buffer] is little-endian. Implementations must leave its byte order alone.
   */
  fun pack(buffer: ByteBuffer, value: T)

  companion object {
    /** The type string suffix of an entry that holds a struct schema. */
    const val SCHEMA_ENTRY_TYPE = "structschema"
  }
}

/**
 * Builds a [StructSchema] for [T] by declaring its fields in wire order.
 *
 * ```kotlin
 * val translation2d = structSchema<Translation2d>("Translation2d") {
 *   float64("x") { it.x }
 *   float64("y") { it.y }
 * }
 * val pose2d = structSchema<Pose2d>("Pose2d") {
 *   struct("translation", translation2d) { it.translation }
 *   struct("rotation", rotation2d) { it.rotation }
 * }
 * ```
 *
 * @throws IllegalArgumentException if a name is not a valid identifier, a field name repeats, no
 *   fields are declared, or two nested schemas share a type name but differ.
 */
fun <T> structSchema(typeName: String, block: StructSchemaBuilder<T>.() -> Unit): StructSchema<T> =
  StructSchemaBuilder<T>(typeName).apply(block).build()

/** Declares the fields of a struct. See [structSchema]. */
class StructSchemaBuilder<T> internal constructor(private val typeName: String) {
  private class Field<T>(
    val name: String,
    val declaration: String,
    val size: Int,
    val nested: StructSchema<*>?,
    val pack: (ByteBuffer, T) -> Unit,
  )

  private val fields = mutableListOf<Field<T>>()

  /** Declares a `bool` field, packed as one byte. */
  fun boolean(name: String, get: (T) -> Boolean) =
    add(name, "bool", 1) { buffer, value -> buffer.put(if (get(value)) 1 else 0) }

  /** Declares an `int8` field. */
  fun int8(name: String, get: (T) -> Byte) =
    add(name, "int8", Byte.SIZE_BYTES) { buffer, value -> buffer.put(get(value)) }

  /** Declares an `int16` field. */
  fun int16(name: String, get: (T) -> Short) =
    add(name, "int16", Short.SIZE_BYTES) { buffer, value -> buffer.putShort(get(value)) }

  /** Declares an `int32` field. */
  fun int32(name: String, get: (T) -> Int) =
    add(name, "int32", Int.SIZE_BYTES) { buffer, value -> buffer.putInt(get(value)) }

  /** Declares an `int64` field. */
  fun int64(name: String, get: (T) -> Long) =
    add(name, "int64", Long.SIZE_BYTES) { buffer, value -> buffer.putLong(get(value)) }

  /** Declares a `float32` field. */
  fun float32(name: String, get: (T) -> Float) =
    add(name, "float32", Float.SIZE_BYTES) { buffer, value -> buffer.putFloat(get(value)) }

  /** Declares a `float64` field. */
  fun float64(name: String, get: (T) -> Double) =
    add(name, "float64", Double.SIZE_BYTES) { buffer, value -> buffer.putDouble(get(value)) }

  /**
   * Declares a fixed-size `float64` array field of [length] elements.
   *
   * @throws IllegalArgumentException when packing, if [get] returns an array of another length
   */
  fun float64Array(name: String, length: Int, get: (T) -> DoubleArray) {
    require(length > 0) { "Array field '$name' must have a positive length, but was $length" }
    add(name, "float64", Double.SIZE_BYTES * length, suffix = "[$length]") { buffer, value ->
      val array = get(value)
      require(array.size == length) {
        "Array field '$name' of struct '$typeName' must have $length elements, but had ${array.size}"
      }
      array.forEach { buffer.putDouble(it) }
    }
  }

  /** Declares a field whose value is another struct, packed inline. */
  fun <N> struct(name: String, schema: StructSchema<N>, get: (T) -> N) =
    add(name, schema.typeName, schema.size, nested = schema) { buffer, value ->
      schema.pack(buffer, get(value))
    }

  private fun add(
    name: String,
    typeDeclaration: String,
    size: Int,
    suffix: String = "",
    nested: StructSchema<*>? = null,
    pack: (ByteBuffer, T) -> Unit,
  ) {
    requireIdentifier(name, "Field name")
    require(fields.none { it.name == name }) {
      "Struct '$typeName' declares field '$name' more than once"
    }
    fields += Field(name, "$typeDeclaration $name$suffix", size, nested, pack)
  }

  internal fun build(): StructSchema<T> {
    requireIdentifier(typeName, "Struct type name")
    require(fields.isNotEmpty()) { "Struct '$typeName' must declare at least one field" }
    val nested = fields.mapNotNull { it.nested }.distinctBy { it.typeName }
    for (field in fields) {
      val other = field.nested ?: continue
      val first = nested.first { it.typeName == other.typeName }
      require(first.schema == other.schema) {
        "Struct '$typeName' nests two different schemas named '${other.typeName}'"
      }
    }
    return BuiltStructSchema(
      typeName,
      fields.sumOf { it.size },
      fields.joinToString("; ") { it.declaration },
      nested,
      fields.map { it.pack },
    )
  }

  private fun requireIdentifier(value: String, what: String) {
    require(IDENTIFIER.matches(value)) {
      "$what '$value' must start with a letter or underscore and contain only letters, digits, " +
        "and underscores"
    }
  }

  private companion object {
    val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
  }
}

private class BuiltStructSchema<T>(
  override val typeName: String,
  override val size: Int,
  override val schema: String,
  override val nested: List<StructSchema<*>>,
  private val packers: List<(ByteBuffer, T) -> Unit>,
) : StructSchema<T> {
  override fun pack(buffer: ByteBuffer, value: T) {
    require(buffer.order() == ByteOrder.LITTLE_ENDIAN) {
      "Struct buffers must be little-endian, but was ${buffer.order()}"
    }
    for (packer in packers) packer(buffer, value)
  }

  override fun toString() = "StructSchema($typeString: $schema)"
}
