/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.backend

import dev.nextftc.telemetry.TelemetryBackend
import dev.nextftc.telemetry.TelemetryEntry
import dev.nextftc.telemetry.TelemetryType
import dev.nextftc.telemetry.struct.StructSchema

/**
 * Keeps everything it is given, for tests that want to check what robot code logged.
 *
 * ```kotlin
 * val clock = TestTimeSource()
 * val registry = TelemetryRegistry(clock)
 * val mock = MockTelemetryBackend().also(registry::register)
 *
 * registry.table("Drive").log("left", 1.5)
 *
 * assertEquals(listOf(1.5), mock.valuesAt("/Drive/left"))
 * ```
 *
 * Arrays and lists are copied when they are logged, so changing one afterwards doesn't change what
 * the backend recorded. A struct is recorded as the object that was logged, so tests can compare it
 * with `==`. It is safe to log from several threads. The inspection methods return copies, which can
 * be stale if logging is still running.
 */
class MockTelemetryBackend : TelemetryBackend {
  /** One logged value. */
  data class Record(val path: String, val value: Any?, val timestampMicros: Long)

  private val lock = Any()
  private val log = mutableListOf<Record>()
  private val types = LinkedHashMap<String, TelemetryType>()
  private val properties = HashMap<String, Map<String, Any>>()
  private var closed = false

  /** Every value logged, in the order the backend received them. */
  val records: List<Record>
    get() = synchronized(lock) { log.toList() }

  /** The paths that have an entry, in the order they were created. */
  val paths: List<String>
    get() = synchronized(lock) { types.keys.toList() }

  /** Whether [close] has been called. */
  val isClosed: Boolean
    get() = synchronized(lock) { closed }

  /** The values logged to [path], oldest first. Arrays are `BooleanArray`, `DoubleArray` and so on. */
  fun valuesAt(path: String): List<Any?> =
    synchronized(lock) { log.filter { it.path == path }.map { it.value } }

  /** The timestamps of the values logged to [path], oldest first, in microseconds. */
  fun timestampsAt(path: String): List<Long> =
    synchronized(lock) { log.filter { it.path == path }.map { it.timestampMicros } }

  /** The type [path] was created with, or `null` if nothing has created it. */
  fun typeAt(path: String): TelemetryType? = synchronized(lock) { types[path] }

  /** Forgets every logged value, keeping the entries. */
  fun clear() = synchronized(lock) { log.clear() }

  /** The properties [path] was created with, or an empty map if nothing has created it. */
  fun propertiesAt(path: String): Map<String, Any> = synchronized(lock) { properties[path].orEmpty() }

  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    synchronized(lock) {
      types[path] = type
      this.properties[path] = properties.toMap()
    }
    return MockEntry(path)
  }

  override fun close() = synchronized(lock) { closed = true }

  override fun toString() = "MockTelemetryBackend"

  private fun record(path: String, value: Any?, timestampMicros: Long) {
    synchronized(lock) { log += Record(path, value, timestampMicros) }
  }

  private inner class MockEntry(private val path: String) : TelemetryEntry {
    override fun logBoolean(value: Boolean, timestampMicros: Long) = record(path, value, timestampMicros)

    override fun logInteger(value: Long, timestampMicros: Long) = record(path, value, timestampMicros)

    override fun logFloat(value: Float, timestampMicros: Long) = record(path, value, timestampMicros)

    override fun logDouble(value: Double, timestampMicros: Long) = record(path, value, timestampMicros)

    override fun logString(value: String, timestampMicros: Long) = record(path, value, timestampMicros)

    override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) =
      record(path, value.copyOf(), timestampMicros)

    override fun logIntegerArray(value: LongArray, timestampMicros: Long) =
      record(path, value.copyOf(), timestampMicros)

    override fun logFloatArray(value: FloatArray, timestampMicros: Long) =
      record(path, value.copyOf(), timestampMicros)

    override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) =
      record(path, value.copyOf(), timestampMicros)

    override fun logStringArray(value: List<String>, timestampMicros: Long) =
      record(path, value.toList(), timestampMicros)

    override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) =
      record(path, value, timestampMicros)

    override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) =
      record(path, values.toList(), timestampMicros)
  }
}
