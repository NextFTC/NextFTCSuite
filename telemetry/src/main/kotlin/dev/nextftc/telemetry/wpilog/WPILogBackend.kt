/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.wpilog

import dev.nextftc.telemetry.StructType
import dev.nextftc.telemetry.TelemetryBackend
import dev.nextftc.telemetry.TelemetryEntry
import dev.nextftc.telemetry.TelemetryType
import dev.nextftc.telemetry.ValueType
import dev.nextftc.telemetry.struct.StructSchema
import dev.nextftc.telemetry.toJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap

/**
 * Records logged values to a [WPILogWriter], one entry per path.
 *
 * The writer's output decides where the log goes. Closing this backend closes the writer.
 *
 * Values are written with the timestamp the registry gave them, so the writer's own clock isn't
 * involved. To keep a log lined up with a server or another backend, build the registry from the
 * clock you want and give the writer the same source and origin.
 *
 * After the backend is closed, its entries drop values quietly.
 */
class WPILogBackend(val writer: WPILogWriter) : TelemetryBackend {
  private class Created(val type: TelemetryType, val entry: TelemetryEntry)

  private val created = ConcurrentHashMap<String, Created>()

  @Volatile private var closed = false

  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    val existing = created.computeIfAbsent(path) { Created(type, createEntry(path, type, properties)) }
    require(existing.type == type) {
      "'$path' was already created as ${existing.type}, so it can't be $type"
    }
    return existing.entry
  }

  private fun createEntry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    val metadata = metadataOf(properties)
    val id =
      when (type) {
        is StructType ->
          if (type.isArray) {
            writer.startStructArrayEntry(path, type.schema, writer.now(), metadata)
          } else {
            writer.startStructEntry(path, type.schema, writer.now(), metadata)
          }
        ValueType.BOOLEAN -> writer.startEntry(path, WPILogType.BOOLEAN, metadata = metadata)
        ValueType.INTEGER -> writer.startEntry(path, WPILogType.INTEGER, metadata = metadata)
        ValueType.FLOAT -> writer.startEntry(path, WPILogType.FLOAT, metadata = metadata)
        ValueType.DOUBLE -> writer.startEntry(path, WPILogType.DOUBLE, metadata = metadata)
        ValueType.STRING -> writer.startEntry(path, WPILogType.STRING, metadata = metadata)
        ValueType.BOOLEAN_ARRAY -> writer.startEntry(path, WPILogType.BOOLEAN_ARRAY, metadata = metadata)
        ValueType.INTEGER_ARRAY -> writer.startEntry(path, WPILogType.INTEGER_ARRAY, metadata = metadata)
        ValueType.FLOAT_ARRAY -> writer.startEntry(path, WPILogType.FLOAT_ARRAY, metadata = metadata)
        ValueType.DOUBLE_ARRAY -> writer.startEntry(path, WPILogType.DOUBLE_ARRAY, metadata = metadata)
        ValueType.STRING_ARRAY -> writer.startEntry(path, WPILogType.STRING_ARRAY, metadata = metadata)
      }
    return LogEntry(id)
  }

  /**
   * The entry metadata for [properties], laid out as WPILib logs NetworkTables topics, which is where
   * AdvantageScope looks for a unit. Empty when there are none.
   */
  private fun metadataOf(properties: Map<String, Any>): String = if (properties.isEmpty()) {
    ""
  } else {
    buildJsonObject {
      put("properties", properties.toJsonObject())
      put("source", "NT")
    }
      .toString()
  }

  /** Flushes pending records and closes the writer. Does nothing if already closed. */
  override fun close() {
    closed = true
    writer.close()
  }

  override fun toString() = "WPILogBackend($writer)"

  private inner class LogEntry(private val id: Int) : TelemetryEntry {
    override fun logBoolean(value: Boolean, timestampMicros: Long) {
      if (!closed) writer.appendBoolean(id, value, timestampMicros)
    }

    override fun logInteger(value: Long, timestampMicros: Long) {
      if (!closed) writer.appendInteger(id, value, timestampMicros)
    }

    override fun logFloat(value: Float, timestampMicros: Long) {
      if (!closed) writer.appendFloat(id, value, timestampMicros)
    }

    override fun logDouble(value: Double, timestampMicros: Long) {
      if (!closed) writer.appendDouble(id, value, timestampMicros)
    }

    override fun logString(value: String, timestampMicros: Long) {
      if (!closed) writer.appendString(id, value, timestampMicros)
    }

    override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) {
      if (!closed) writer.appendBooleanArray(id, value, timestampMicros)
    }

    override fun logIntegerArray(value: LongArray, timestampMicros: Long) {
      if (!closed) writer.appendIntegerArray(id, value, timestampMicros)
    }

    override fun logFloatArray(value: FloatArray, timestampMicros: Long) {
      if (!closed) writer.appendFloatArray(id, value, timestampMicros)
    }

    override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) {
      if (!closed) writer.appendDoubleArray(id, value, timestampMicros)
    }

    override fun logStringArray(value: List<String>, timestampMicros: Long) {
      if (!closed) writer.appendStringArray(id, value, timestampMicros)
    }

    override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) {
      if (!closed) writer.appendStruct(id, schema, value, timestampMicros)
    }

    override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) {
      if (!closed) writer.appendStructArray(id, schema, values, timestampMicros)
    }
  }
}
