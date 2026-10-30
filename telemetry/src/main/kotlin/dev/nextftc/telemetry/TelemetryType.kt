/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.struct.StructSchema

/**
 * What a telemetry path holds: one of the [ValueType]s, or a [StructType].
 *
 * A path holds one type for as long as the program runs, and backends are told it when they create
 * the path's entry.
 */
sealed interface TelemetryType

/** The types that aren't structs. Integers of every width are [INTEGER]. */
enum class ValueType : TelemetryType {
  BOOLEAN,
  INTEGER,
  FLOAT,
  DOUBLE,
  STRING,
  BOOLEAN_ARRAY,
  INTEGER_ARRAY,
  FLOAT_ARRAY,
  DOUBLE_ARRAY,
  STRING_ARRAY,
  ;

  override fun toString(): String = name.lowercase().replace('_', ' ')
}

/**
 * A struct, or an array of them, described by [schema].
 *
 * Two struct types are equal if they have the same type name, schema text, and array-ness.
 *
 * @property isArray Whether the path holds a variable-length array of [schema] values
 */
class StructType(val schema: StructSchema<*>, val isArray: Boolean = false) : TelemetryType {
  override fun equals(other: Any?): Boolean = other is StructType &&
    isArray == other.isArray &&
    schema.typeName == other.schema.typeName &&
    schema.schema == other.schema.schema

  override fun hashCode(): Int =
    31 * schema.typeName.hashCode() + schema.schema.hashCode() + if (isArray) 1 else 0

  override fun toString(): String = if (isArray) schema.arrayTypeString else schema.typeString
}
