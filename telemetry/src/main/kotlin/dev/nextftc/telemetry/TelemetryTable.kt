/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.struct.StructSchema
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * An object that knows how to publish itself into a [TelemetryTable].
 *
 * Implement this for a type that is several values. Call sites log one object, and the type decides
 * what appears under it:
 *
 * ```kotlin
 * class DriveState(val left: Double, val right: Double, val pose: Pose2d) : TelemetryLoggable {
 *   override fun logTo(table: TelemetryTable) {
 *     table.log("left", left)
 *     table.log("right", right)
 *     table.log("pose", pose)
 *   }
 * }
 *
 * Telemetry.log("drive", driveState) // lands under /drive/left, /drive/right, and /drive/pose
 * ```
 *
 * Logging a loggable is a series of separate writes, not one atomic snapshot. A reader can see some
 * fields from this loop and some from the last. If the fields must agree, log a struct instead.
 */
fun interface TelemetryLoggable {
  /** Writes this object's values into [table], which is the child table named by the caller. */
  fun logTo(table: TelemetryTable)
}

/**
 * A namespace of logged values: a path like `/Drive`, whose topics are `/Drive/leftVelocity` and so
 * on, and whose children are tables like `/Drive/Odometry`.
 *
 * Tables are cheap and cache what they hand out, but hold on to a table or a topic in a field rather
 * than looking it up every loop. The fastest way to log is a typed handle:
 *
 * ```kotlin
 * private val table = Telemetry.table("Shooter")
 * private val velocity = table.doubleTopic("wheelVelocity")
 * private val ready = table.booleanTopic("ready")
 *
 * fun periodic() {
 *   velocity.set(encoder.velocity)
 *   ready.set(atSpeed())
 * }
 * ```
 *
 * `log` is the shorter way, which looks the handle up each time:
 * `table.log("wheelVelocity", encoder.velocity)`.
 *
 * ## Names
 *
 * A name with slashes, like `a/b`, is the same as asking for the table `a` and then the name `b`.
 * Leading and doubled slashes are ignored, and so are all the slashes of a table name. A topic name
 * that ends in a slash has no name, so it warns and drops the value.
 *
 * ## Types
 *
 * A path holds one type for as long as a backend is registered. Logging a different type to a path
 * that a backend already has warns, and the value doesn't reach that backend. Telemetry never throws
 * into the code that calls it. It reports what it can't log through the registry's warning handler.
 *
 * Because the rule belongs to the backend, not the program, each OpMode starts clean: its backends
 * are new, so it can give a path a type that an earlier OpMode gave it a different one for.
 *
 * ## Struct values
 *
 * Any other object is logged as a struct, with its schema reflected from its class by the registry's
 * [TelemetryRegistry.structs]. See [dev.nextftc.telemetry.struct.StructSchemaRegistry] for what is
 * supported, and use [topic] with a hand-written [StructSchema] for a class that can't be reflected
 * or to log faster.
 *
 * A list can't be logged with `log`, because its element type is erased. Use [arrayTopic], or the
 * [logArray] extension.
 *
 * A table may be used from any thread.
 *
 * @property path The path of this table, such as `/Drive`, or empty for the root
 */
class TelemetryTable internal constructor(private val registry: TelemetryRegistry, val path: String) {
  private val children = ConcurrentHashMap<String, TelemetryTable>()
  private class TopicKey(val name: String, val type: TelemetryType) {
    override fun equals(other: Any?) = other is TopicKey && name == other.name && type == other.type

    override fun hashCode() = 31 * name.hashCode() + type.hashCode()
  }

  private val topics = ConcurrentHashMap<TopicKey, TelemetryTopic>()

  /** Returns the child table [name], creating it if needed. */
  fun table(name: String): TelemetryTable {
    var table = this
    for (part in name.split('/')) {
      if (part.isEmpty()) continue
      val parent = table
      table = parent.children.computeIfAbsent(part) { TelemetryTable(registry, "${parent.path}/$part") }
    }
    return table
  }

  // region Handles

  /** Returns the handle for the `boolean` topic [name]. */
  fun booleanTopic(name: String): BooleanTopic =
    topicOf(name, ValueType.BOOLEAN, BooleanTopic::class) { path, type ->
      BooleanTopic(registry, path, type)
    }

  /** Returns the handle for the integer topic [name], for values that fit in an `Int`. */
  fun intTopic(name: String): IntTopic =
    topicOf(name, ValueType.INTEGER, IntTopic::class) { path, type -> IntTopic(registry, path, type) }

  /** Returns the handle for the integer topic [name]. */
  fun longTopic(name: String): LongTopic =
    topicOf(name, ValueType.INTEGER, LongTopic::class) { path, type -> LongTopic(registry, path, type) }

  /** Returns the handle for the `float` topic [name]. */
  fun floatTopic(name: String): FloatTopic =
    topicOf(name, ValueType.FLOAT, FloatTopic::class) { path, type -> FloatTopic(registry, path, type) }

  /** Returns the handle for the `double` topic [name]. */
  fun doubleTopic(name: String): DoubleTopic =
    topicOf(name, ValueType.DOUBLE, DoubleTopic::class) { path, type -> DoubleTopic(registry, path, type) }

  /** Returns the handle for the string topic [name]. */
  fun stringTopic(name: String): Topic<String> =
    topicOf(name, ValueType.STRING, Topic::class) { path, type ->
      Topic<String>(registry, path, type) { entry, value, timestamp ->
        entry.logString(value, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of booleans. */
  fun booleanArrayTopic(name: String): Topic<BooleanArray> =
    topicOf(name, ValueType.BOOLEAN_ARRAY, Topic::class) { path, type ->
      Topic<BooleanArray>(registry, path, type) { entry, value, timestamp ->
        entry.logBooleanArray(value, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of integers. */
  fun intArrayTopic(name: String): Topic<IntArray> =
    topicOf(name, ValueType.INTEGER_ARRAY, Topic::class) { path, type ->
      Topic<IntArray>(registry, path, type) { entry, value, timestamp ->
        entry.logIntegerArray(LongArray(value.size) { value[it].toLong() }, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of integers. */
  fun longArrayTopic(name: String): Topic<LongArray> =
    topicOf(name, ValueType.INTEGER_ARRAY, Topic::class) { path, type ->
      Topic<LongArray>(registry, path, type) { entry, value, timestamp ->
        entry.logIntegerArray(value, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of floats. */
  fun floatArrayTopic(name: String): Topic<FloatArray> =
    topicOf(name, ValueType.FLOAT_ARRAY, Topic::class) { path, type ->
      Topic<FloatArray>(registry, path, type) { entry, value, timestamp ->
        entry.logFloatArray(value, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of doubles. */
  fun doubleArrayTopic(name: String): Topic<DoubleArray> =
    topicOf(name, ValueType.DOUBLE_ARRAY, Topic::class) { path, type ->
      Topic<DoubleArray>(registry, path, type) { entry, value, timestamp ->
        entry.logDoubleArray(value, timestamp)
      }
    }

  /** Returns the handle for the topic [name] that holds arrays of strings. */
  fun stringArrayTopic(name: String): Topic<List<String>> =
    topicOf(name, ValueType.STRING_ARRAY, Topic::class) { path, type ->
      Topic<List<String>>(registry, path, type) { entry, value, timestamp ->
        entry.logStringArray(value, timestamp)
      }
    }

  /** Returns the handle for the struct topic [name], which holds values of [schema]. */
  fun <T : Any> topic(name: String, schema: StructSchema<T>): Topic<T> =
    topicOf(name, StructType(schema), Topic::class) { path, type ->
      Topic<T>(registry, path, type) { entry, value, timestamp ->
        entry.logStruct(schema, value, timestamp)
      }
    }

  /**
   * Returns the handle for the struct topic [name], which holds values of [type].
   *
   * The schema is reflected from [type]. If that isn't possible, this warns and returns a handle
   * that drops every value.
   */
  fun <T : Any> topic(name: String, type: KClass<T>): Topic<T> {
    val schema =
      try {
        registry.structs.schemaOf(type)
      } catch (e: IllegalArgumentException) {
        registry.warn("Could not create a topic at '${pathOf(name)}': ${e.message}")
        return Topic(registry, pathOf(name), null) { _, _, _ -> }
      }
    return topic(name, schema)
  }

  /** Returns the handle for the struct topic [name], which holds values of [type]. */
  fun <T : Any> topic(name: String, type: Class<T>): Topic<T> = topic(name, type.kotlin)

  /** Returns the handle for the topic [name] that holds arrays of structs of [schema]. */
  fun <T : Any> arrayTopic(name: String, schema: StructSchema<T>): Topic<List<T>> =
    topicOf(name, StructType(schema, isArray = true), Topic::class) { path, type ->
      Topic<List<T>>(registry, path, type) { entry, value, timestamp ->
        entry.logStructArray(schema, value, timestamp)
      }
    }

  /**
   * Returns the handle for the topic [name] that holds arrays of structs of [type]. See [topic] for
   * how the schema is found.
   */
  fun <T : Any> arrayTopic(name: String, type: KClass<T>): Topic<List<T>> {
    val schema =
      try {
        registry.structs.schemaOf(type)
      } catch (e: IllegalArgumentException) {
        registry.warn("Could not create a topic at '${pathOf(name)}': ${e.message}")
        return Topic(registry, pathOf(name), null) { _, _, _ -> }
      }
    return arrayTopic(name, schema)
  }

  /** Returns the handle for the topic [name] that holds arrays of structs of [type]. */
  fun <T : Any> arrayTopic(name: String, type: Class<T>): Topic<List<T>> = arrayTopic(name, type.kotlin)

  /**
   * Returns the cached handle for [name], or makes one with [create].
   *
   * Handles are cached per name and type. Two types for one name get two handles, and it is the
   * backends, when a value is logged, that refuse the second one. A cached handle of a different
   * class but the same type, such as an [IntTopic] where a [LongTopic] was first made, is fine: both
   * log the same thing, so this makes another handle and leaves the cached one alone.
   */
  private fun <H : TelemetryTopic> topicOf(
    name: String,
    type: TelemetryType,
    kind: KClass<*>,
    create: (path: String, type: TelemetryType?) -> H,
  ): H {
    val slash = name.lastIndexOf('/')
    if (slash >= 0) {
      return table(name.substring(0, slash)).topicOf(name.substring(slash + 1), type, kind, create)
    }
    if (name.isEmpty()) {
      registry.warn("Could not create a topic in '${path.ifEmpty { "/" }}': a topic needs a name")
      return create("$path/", null)
    }

    val fullPath = "$path/$name"
    val existing = topics.computeIfAbsent(TopicKey(name, type)) { create(fullPath, type) }
    @Suppress("UNCHECKED_CAST")
    return if (existing::class == kind) existing as H else create(fullPath, type)
  }

  private fun pathOf(name: String): String =
    "$path/" + name.split('/').filter { it.isNotEmpty() }.joinToString("/")

  // endregion

  // region Logging by name

  /** Logs [value] to the `boolean` topic [name]. */
  fun log(name: String, value: Boolean) = booleanTopic(name).set(value)

  /** Logs [value] to the integer topic [name]. */
  fun log(name: String, value: Int) = intTopic(name).set(value)

  /** Logs [value] to the integer topic [name]. */
  fun log(name: String, value: Long) = longTopic(name).set(value)

  /** Logs [value] to the `float` topic [name]. */
  fun log(name: String, value: Float) = floatTopic(name).set(value)

  /** Logs [value] to the `double` topic [name]. */
  fun log(name: String, value: Double) = doubleTopic(name).set(value)

  /** Logs [value] to the string topic [name]. */
  fun log(name: String, value: String) = stringTopic(name).set(value)

  /** Logs [value] to the topic [name] that holds arrays of booleans. */
  fun log(name: String, value: BooleanArray) = booleanArrayTopic(name).set(value)

  /** Logs [value] to the topic [name] that holds arrays of integers. */
  fun log(name: String, value: IntArray) = intArrayTopic(name).set(value)

  /** Logs [value] to the topic [name] that holds arrays of integers. */
  fun log(name: String, value: LongArray) = longArrayTopic(name).set(value)

  /** Logs [value] to the topic [name] that holds arrays of floats. */
  fun log(name: String, value: FloatArray) = floatArrayTopic(name).set(value)

  /** Logs [value] to the topic [name] that holds arrays of doubles. */
  fun log(name: String, value: DoubleArray) = doubleArrayTopic(name).set(value)

  /**
   * Logs [value] at [name], choosing how from what it is.
   *
   * Numbers, booleans, strings, and primitive arrays go to the topics above, with a `Byte` or
   * `Short` logged as an integer. An array of objects is logged as an array of structs, or of
   * strings. A [TelemetryLoggable] is logged into the child table [name]. Anything else is logged as
   * a struct.
   *
   * A collection or map is rejected with a warning, because its element type is erased. Use
   * [arrayTopic] or [logArray] for a list.
   */
  fun log(name: String, value: Any) {
    try {
      logAny(name, value)
    } catch (e: Exception) {
      registry.warn("Could not log '${pathOf(name)}': $e")
    }
  }

  private fun logAny(name: String, value: Any) {
    when (value) {
      is Double -> log(name, value)
      is Float -> log(name, value)
      is Int -> log(name, value)
      is Long -> log(name, value)
      is Boolean -> log(name, value)
      is Byte -> log(name, value.toInt())
      is Short -> log(name, value.toInt())
      is String -> log(name, value)
      is BooleanArray -> log(name, value)
      is IntArray -> log(name, value)
      is LongArray -> log(name, value)
      is FloatArray -> log(name, value)
      is DoubleArray -> log(name, value)
      is TelemetryLoggable -> value.logTo(table(name))
      is Collection<*>,
      is Map<*, *>,
      ->
        registry.warn(
          "Could not log '${pathOf(name)}': a ${value.javaClass.simpleName} has an erased element " +
            "type. Use arrayTopic or logArray to log a list of structs.",
        )
      is Array<*> -> logObjectArray(name, value)
      else -> topic(name, value.javaClass.kotlin).set(value)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun logObjectArray(name: String, value: Array<*>) {
    val element = value.javaClass.componentType
    if (element == String::class.java) {
      stringArrayTopic(name).set((value as Array<String>).asList())
    } else {
      arrayTopic(name, element.kotlin as KClass<Any>).set(value.asList() as List<Any>)
    }
  }

  // endregion

  override fun toString(): String = "TelemetryTable(${path.ifEmpty { "/" }})"
}

/**
 * Logs [values], a list of structs of [T], to the topic [name].
 *
 * This is an extension because it needs [T], which a plain function can't see through erasure. See
 * [TelemetryTable.arrayTopic] for the handle that [TelemetryTable.log] can't give you.
 */
inline fun <reified T : Any> TelemetryTable.logArray(name: String, values: List<T>) =
  arrayTopic(name, T::class).set(values)

/** Returns the handle for the struct topic [name], which holds values of [T]. */
inline fun <reified T : Any> TelemetryTable.topic(name: String): Topic<T> = topic(name, T::class)

/** Returns the handle for the topic [name] that holds arrays of structs of [T]. */
inline fun <reified T : Any> TelemetryTable.arrayTopic(name: String): Topic<List<T>> =
  arrayTopic(name, T::class)
