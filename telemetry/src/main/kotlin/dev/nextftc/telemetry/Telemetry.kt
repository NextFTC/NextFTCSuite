/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.struct.StructSchema
import kotlin.reflect.KClass

/**
 * The entry point for telemetry: a shared [TelemetryRegistry] and its root table.
 *
 * ```kotlin
 * Telemetry.log("Battery/Voltage", voltageSensor.voltage)
 *
 * val drive = Telemetry.table("Drive")
 * val velocity = drive.doubleTopic("leftVelocity")
 * velocity.set(left.velocity)
 * drive.log("pose", follower.pose)
 * ```
 *
 * Values go to the backends registered on [registry]. On a robot, NextFTC registers them for each
 * OpMode. In a unit test, register a [dev.nextftc.telemetry.backend.MockTelemetryBackend] and read
 * back what was logged, or build a [TelemetryRegistry] of your own.
 *
 * Everything here forwards to the root table. See [TelemetryTable] for how values are typed, what a
 * name holds, and what thread safety to expect.
 */
object Telemetry {
  /** The registry this facade logs through. */
  @JvmStatic val registry: TelemetryRegistry = TelemetryRegistry()

  /** The root table, which every other table is under. */
  @JvmStatic
  val root: TelemetryTable
    get() = registry.root

  /** Returns the table [name], creating it if needed. Hold on to it instead of looking it up every loop. */
  @JvmStatic fun table(name: String): TelemetryTable = registry.table(name)

  // region Handles

  /** Returns the handle for the `boolean` topic [name]. */
  @JvmStatic fun booleanTopic(name: String): BooleanTopic = root.booleanTopic(name)

  /** Returns the handle for the integer topic [name], for values that fit in an `Int`. */
  @JvmStatic fun intTopic(name: String): IntTopic = root.intTopic(name)

  /** Returns the handle for the integer topic [name]. */
  @JvmStatic fun longTopic(name: String): LongTopic = root.longTopic(name)

  /** Returns the handle for the `float` topic [name]. */
  @JvmStatic fun floatTopic(name: String): FloatTopic = root.floatTopic(name)

  /** Returns the handle for the `double` topic [name]. */
  @JvmStatic fun doubleTopic(name: String): DoubleTopic = root.doubleTopic(name)

  /** Returns the handle for the string topic [name]. */
  @JvmStatic fun stringTopic(name: String): Topic<String> = root.stringTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of booleans. */
  @JvmStatic fun booleanArrayTopic(name: String): Topic<BooleanArray> = root.booleanArrayTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of integers. */
  @JvmStatic fun intArrayTopic(name: String): Topic<IntArray> = root.intArrayTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of integers. */
  @JvmStatic fun longArrayTopic(name: String): Topic<LongArray> = root.longArrayTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of floats. */
  @JvmStatic fun floatArrayTopic(name: String): Topic<FloatArray> = root.floatArrayTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of doubles. */
  @JvmStatic fun doubleArrayTopic(name: String): Topic<DoubleArray> = root.doubleArrayTopic(name)

  /** Returns the handle for the topic [name] that holds arrays of strings. */
  @JvmStatic fun stringArrayTopic(name: String): Topic<List<String>> = root.stringArrayTopic(name)

  /** Returns the handle for the struct topic [name], which holds values of [schema]. */
  @JvmStatic fun <T : Any> topic(name: String, schema: StructSchema<T>): Topic<T> = root.topic(name, schema)

  /** Returns the handle for the struct topic [name], which holds values of [type]. */
  @JvmStatic fun <T : Any> topic(name: String, type: KClass<T>): Topic<T> = root.topic(name, type)

  /** Returns the handle for the struct topic [name], which holds values of [type]. */
  @JvmStatic fun <T : Any> topic(name: String, type: Class<T>): Topic<T> = root.topic(name, type)

  /** Returns the handle for the topic [name] that holds arrays of structs of [schema]. */
  @JvmStatic
  fun <T : Any> arrayTopic(name: String, schema: StructSchema<T>): Topic<List<T>> =
    root.arrayTopic(name, schema)

  /** Returns the handle for the topic [name] that holds arrays of structs of [type]. */
  @JvmStatic
  fun <T : Any> arrayTopic(name: String, type: KClass<T>): Topic<List<T>> = root.arrayTopic(name, type)

  /** Returns the handle for the topic [name] that holds arrays of structs of [type]. */
  @JvmStatic
  fun <T : Any> arrayTopic(name: String, type: Class<T>): Topic<List<T>> = root.arrayTopic(name, type)

  // endregion

  // region Logging by name

  /** Logs [value] to the `boolean` topic [name]. */
  @JvmStatic fun log(name: String, value: Boolean) = root.log(name, value)

  /** Logs [value] to the integer topic [name]. */
  @JvmStatic fun log(name: String, value: Int) = root.log(name, value)

  /** Logs [value] to the integer topic [name]. */
  @JvmStatic fun log(name: String, value: Long) = root.log(name, value)

  /** Logs [value] to the `float` topic [name]. */
  @JvmStatic fun log(name: String, value: Float) = root.log(name, value)

  /** Logs [value] to the `double` topic [name]. */
  @JvmStatic fun log(name: String, value: Double) = root.log(name, value)

  /** Logs [value] to the string topic [name]. */
  @JvmStatic fun log(name: String, value: String) = root.log(name, value)

  /** Logs [value] to the topic [name] that holds arrays of booleans. */
  @JvmStatic fun log(name: String, value: BooleanArray) = root.log(name, value)

  /** Logs [value] to the topic [name] that holds arrays of integers. */
  @JvmStatic fun log(name: String, value: IntArray) = root.log(name, value)

  /** Logs [value] to the topic [name] that holds arrays of integers. */
  @JvmStatic fun log(name: String, value: LongArray) = root.log(name, value)

  /** Logs [value] to the topic [name] that holds arrays of floats. */
  @JvmStatic fun log(name: String, value: FloatArray) = root.log(name, value)

  /** Logs [value] to the topic [name] that holds arrays of doubles. */
  @JvmStatic fun log(name: String, value: DoubleArray) = root.log(name, value)

  /**
   * Logs [value] at [name], choosing how from what it is.
   *
   * @see TelemetryTable.log
   */
  @JvmStatic fun log(name: String, value: Any) = root.log(name, value)

  // endregion
}

/**
 * Logs [values], a list of structs of [T], to the topic [name] under the root.
 *
 * @see logArray
 */
inline fun <reified T : Any> Telemetry.logArray(name: String, values: List<T>) = root.logArray(name, values)

/** Returns the handle for the struct topic [name] under the root, which holds values of [T]. */
inline fun <reified T : Any> Telemetry.topic(name: String): Topic<T> = root.topic(name, T::class)

/** Returns the handle for the topic [name] under the root that holds arrays of structs of [T]. */
inline fun <reified T : Any> Telemetry.arrayTopic(name: String): Topic<List<T>> =
  root.arrayTopic(name, T::class)
