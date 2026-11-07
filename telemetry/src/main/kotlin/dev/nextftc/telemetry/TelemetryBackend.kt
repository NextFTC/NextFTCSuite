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
 * Where the values of one path end up.
 *
 * A [TelemetryRegistry] creates one entry per path, the first time a value is logged there, and only
 * ever calls the method for the path's [TelemetryType]. Timestamps are in microseconds since the
 * registry's origin, and every backend that a value reaches gets the same one.
 *
 * Entries must be safe to call from any thread. They must copy any array or list they keep after a
 * method returns, because the caller may reuse it. They should not throw, but if one does, the
 * registry reports it and carries on.
 */
interface TelemetryEntry {
  fun logBoolean(value: Boolean, timestampMicros: Long)

  /** Receives integers of every width, which the caller has widened to a `Long`. */
  fun logInteger(value: Long, timestampMicros: Long)

  fun logFloat(value: Float, timestampMicros: Long)

  fun logDouble(value: Double, timestampMicros: Long)

  fun logString(value: String, timestampMicros: Long)

  fun logBooleanArray(value: BooleanArray, timestampMicros: Long)

  fun logIntegerArray(value: LongArray, timestampMicros: Long)

  fun logFloatArray(value: FloatArray, timestampMicros: Long)

  fun logDoubleArray(value: DoubleArray, timestampMicros: Long)

  fun logStringArray(value: List<String>, timestampMicros: Long)

  /** Receives a value of the path's [StructType], with the schema that type was created from. */
  fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long)

  /** Receives an array of the path's [StructType]. */
  fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long)
}

/**
 * A destination for logged values: a log file, a live server, a test double.
 *
 * Register one with [TelemetryRegistry.register]. Robot code never sees backends. It logs to a
 * [TelemetryTable], and the registry sends each value to the backends registered.
 *
 * A backend must be thread-safe.
 */
interface TelemetryBackend : AutoCloseable {
  /**
   * Returns the entry for [path], creating it if this backend hasn't seen the path.
   *
   * [path] is the full name, such as `/Drive/leftVelocity`, and [type] is what every value on it will
   * be. This is called again for a path that already has an entry whenever the registry's backends
   * change, so return the entry you already made instead of making a second one.
   *
   * [properties] are the topic's settings, such as its `unit`, with values that are strings, booleans
   * or finite numbers. A backend keeps them where its format has room, and uses those from the first
   * call for a path.
   *
   * It may throw for a failure that only affects this path, such as a name that clashes with an
   * existing topic. The registry reports it and leaves this backend out of the path.
   */
  fun entry(path: String, type: TelemetryType, properties: Map<String, Any> = emptyMap()): TelemetryEntry
}
