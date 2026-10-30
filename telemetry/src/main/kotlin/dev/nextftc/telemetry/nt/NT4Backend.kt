/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.nt

import dev.nextftc.telemetry.StructType
import dev.nextftc.telemetry.TelemetryBackend
import dev.nextftc.telemetry.TelemetryEntry
import dev.nextftc.telemetry.TelemetryType
import dev.nextftc.telemetry.ValueType
import dev.nextftc.telemetry.struct.StructSchema
import java.util.concurrent.ConcurrentHashMap

/**
 * Publishes logged values to an [NT4Server], one topic per path.
 *
 * A topic is published when its path first gets a value, so dashboards only see topics that have
 * data. Closing the backend removes the topics it published, which is how an OpMode's topics go away
 * when it ends. The next OpMode can publish the same names again, but not with different types: the
 * server remembers each name's type for as long as it exists, because dashboards keep a field's first
 * type. Publishing a name with another type is reported as a warning and its values aren't sent.
 *
 * The server is not started or stopped for you unless [stopServerOnClose] is set, because a server
 * normally outlives any one backend. Values go out with the timestamp the registry gave them, so for
 * clients to read them correctly the server and the registry must share a time source and origin.
 *
 * After the backend is closed, its entries drop values quietly.
 *
 * @param server The server to publish to
 * @param stopServerOnClose Whether [close] also stops [server]
 */
class NT4Backend @JvmOverloads constructor(
  val server: NT4Server,
  private val stopServerOnClose: Boolean = false,
) : TelemetryBackend {
  private class Created(val type: TelemetryType, val id: Int, val entry: TelemetryEntry)

  private val created = ConcurrentHashMap<String, Created>()

  @Volatile private var closed = false

  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    val existing = created.computeIfAbsent(path) {
      val id = publish(path, type, properties)
      Created(type, id, TopicEntry(id))
    }
    require(existing.type == type) {
      "'$path' was already published as ${existing.type}, so it can't be $type"
    }
    return existing.entry
  }

  private fun publish(path: String, type: TelemetryType, properties: Map<String, Any>): Int = when (type) {
    is StructType ->
      if (type.isArray) {
        server.publishStructArray(path, type.schema, properties = properties)
      } else {
        server.publishStruct(path, type.schema, properties = properties)
      }
    ValueType.BOOLEAN -> server.publish(path, NT4Type.BOOLEAN, properties = properties)
    ValueType.INTEGER -> server.publish(path, NT4Type.INTEGER, properties = properties)
    ValueType.FLOAT -> server.publish(path, NT4Type.FLOAT, properties = properties)
    ValueType.DOUBLE -> server.publish(path, NT4Type.DOUBLE, properties = properties)
    ValueType.STRING -> server.publish(path, NT4Type.STRING, properties = properties)
    ValueType.BOOLEAN_ARRAY -> server.publish(path, NT4Type.BOOLEAN_ARRAY, properties = properties)
    ValueType.INTEGER_ARRAY -> server.publish(path, NT4Type.INTEGER_ARRAY, properties = properties)
    ValueType.FLOAT_ARRAY -> server.publish(path, NT4Type.FLOAT_ARRAY, properties = properties)
    ValueType.DOUBLE_ARRAY -> server.publish(path, NT4Type.DOUBLE_ARRAY, properties = properties)
    ValueType.STRING_ARRAY -> server.publish(path, NT4Type.STRING_ARRAY, properties = properties)
  }

  /**
   * Removes every topic this backend published, and stops the server if [stopServerOnClose] is set.
   * Does nothing if already closed.
   */
  override fun close() {
    if (closed) return
    closed = true
    for (topic in created.values) server.unpublish(topic.id)
    created.clear()
    if (stopServerOnClose) server.stop()
  }

  override fun toString() = "NT4Backend(port ${server.port})"

  private inner class TopicEntry(private val id: Int) : TelemetryEntry {
    override fun logBoolean(value: Boolean, timestampMicros: Long) {
      if (!closed) server.setBoolean(id, value, timestampMicros)
    }

    override fun logInteger(value: Long, timestampMicros: Long) {
      if (!closed) server.setInteger(id, value, timestampMicros)
    }

    override fun logFloat(value: Float, timestampMicros: Long) {
      if (!closed) server.setFloat(id, value, timestampMicros)
    }

    override fun logDouble(value: Double, timestampMicros: Long) {
      if (!closed) server.setDouble(id, value, timestampMicros)
    }

    override fun logString(value: String, timestampMicros: Long) {
      if (!closed) server.setString(id, value, timestampMicros)
    }

    override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) {
      if (!closed) server.setBooleanArray(id, value, timestampMicros)
    }

    override fun logIntegerArray(value: LongArray, timestampMicros: Long) {
      if (!closed) server.setIntegerArray(id, value, timestampMicros)
    }

    override fun logFloatArray(value: FloatArray, timestampMicros: Long) {
      if (!closed) server.setFloatArray(id, value, timestampMicros)
    }

    override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) {
      if (!closed) server.setDoubleArray(id, value, timestampMicros)
    }

    override fun logStringArray(value: List<String>, timestampMicros: Long) {
      if (!closed) server.setStringArray(id, value, timestampMicros)
    }

    override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) {
      if (!closed) server.setStruct(id, schema, value, timestampMicros)
    }

    override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) {
      if (!closed) server.setStructArray(id, schema, values, timestampMicros)
    }
  }
}
