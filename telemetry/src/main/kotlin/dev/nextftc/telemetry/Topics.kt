/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import kotlin.time.ComparableTimeMark

/**
 * A handle on one telemetry path, which values are set on.
 *
 * Get one from a [TelemetryTable], and hold on to it instead of looking it up every loop. A handle
 * is cheap to set: it keeps the entry it logs to, and only asks the registry for new ones when the
 * backends change. That makes it safe to create before any backend is registered, and it keeps
 * working when OpModes swap one backend for another.
 *
 * A path isn't published to any backend until its first value is set.
 *
 * Setting a value never throws. A value that can't be logged is reported through the registry's
 * warning handler and dropped.
 *
 * A handle may be used from any thread. Concurrent sets on one handle are safe but land in no
 * guaranteed order.
 *
 * @property path The full path of the topic, such as `/Drive/leftVelocity`
 */
abstract class TelemetryTopic
internal constructor(
  internal val registry: TelemetryRegistry,
  val path: String,
  /** What the path holds, or null for a handle that couldn't be created and drops every value. */
  internal val type: TelemetryType?,
) {
  /**
   * Whether to log a value that is the same as the last one.
   *
   * By default a repeated value is skipped, which keeps logs and network traffic small. It only
   * applies to numbers, booleans and strings. Arrays and structs are always logged, because the
   * same array or object can be changed in place between calls.
   */
  @Volatile var keepDuplicates: Boolean = false

  @Volatile private var settings: Map<String, Any> = emptyMap()

  /**
   * The topic's properties, such as its [unit], which backends pass on to dashboards and log files.
   *
   * Values are strings, booleans or finite numbers. A value of another kind is reported and left
   * out. They are sent when the path is first published to a backend, so set them before the first
   * value. Changing them afterwards doesn't reach a backend that has already published the path.
   */
  var properties: Map<String, Any>
    get() = settings
    set(value) {
      val valid = LinkedHashMap<String, Any>()
      for ((key, item) in value) {
        if (isValidPropertyValue(item)) {
          valid[key] = item
        } else {
          registry.warn(
            "Could not set property '$key' on '$path': $item is not a string, boolean or finite number",
          )
        }
      }
      settings = valid
    }

  /**
   * The unit of the topic's values, such as `m` or `rad/s`, or null if it has none. It is the
   * [TelemetryProperties.UNIT] property, with the same timing rules as [properties].
   */
  var unit: String?
    get() = settings[TelemetryProperties.UNIT] as? String
    set(value) {
      properties = if (value == null) {
        settings - TelemetryProperties.UNIT
      } else {
        settings + (TelemetryProperties.UNIT to value)
      }
    }

  /** The entry this handle logs to, for one generation of the registry's backends. */
  internal class Binding(val generation: Long, val entry: TelemetryEntry) {
    // These are written without locking. Two threads setting one handle at once can race here, which
    // at worst logs a duplicate or skips one.
    @JvmField var hasLast = false

    @JvmField var lastBits = 0L

    @JvmField var lastReference: Any? = null
  }

  @Volatile private var binding: Binding? = null

  internal fun binding(): Binding {
    if (type == null) return DETACHED
    val generation = registry.generation
    val current = binding
    if (current != null && current.generation == generation) return current

    val fresh = Binding(generation, registry.entryFor(path, type, settings))
    binding = fresh
    return fresh
  }

  /** Returns [timestampMicros], or the current time if it is [NOW]. */
  internal fun stamp(timestampMicros: Long): Long {
    if (timestampMicros == NOW) return registry.now()
    require(timestampMicros >= 0) { "A timestamp can't be negative, but was $timestampMicros" }
    return timestampMicros
  }

  internal inline fun guarded(block: () -> Unit) {
    try {
      block()
    } catch (e: Exception) {
      registry.warn("Could not log '$path': $e")
    }
  }

  override fun toString(): String = "${javaClass.simpleName}($path)"

  internal companion object {
    /** Asks for the current time. It is not a timestamp anyone would pass. */
    const val NOW = Long.MIN_VALUE

    val DETACHED = Binding(-1, DiscardEntry)
  }
}

/** A handle on a `boolean` topic. */
class BooleanTopic internal constructor(registry: TelemetryRegistry, path: String, type: TelemetryType?) :
  TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: Boolean) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: Boolean, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: Boolean, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: Boolean, timestampMicros: Long) {
    val binding = binding()
    val bits = if (value) 1L else 0L
    if (!keepDuplicates && binding.hasLast && binding.lastBits == bits) return
    binding.entry.logBoolean(value, stamp(timestampMicros))
    binding.hasLast = true
    binding.lastBits = bits
  }
}

/** A handle on an integer topic, for values that fit in an `Int`. For larger ones use [LongTopic]. */
class IntTopic internal constructor(registry: TelemetryRegistry, path: String, type: TelemetryType?) :
  TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: Int) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: Int, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: Int, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: Int, timestampMicros: Long) {
    val binding = binding()
    if (!keepDuplicates && binding.hasLast && binding.lastBits == value.toLong()) return
    binding.entry.logInteger(value.toLong(), stamp(timestampMicros))
    binding.hasLast = true
    binding.lastBits = value.toLong()
  }
}

/** A handle on an integer topic. */
class LongTopic internal constructor(registry: TelemetryRegistry, path: String, type: TelemetryType?) :
  TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: Long) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: Long, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: Long, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: Long, timestampMicros: Long) {
    val binding = binding()
    if (!keepDuplicates && binding.hasLast && binding.lastBits == value) return
    binding.entry.logInteger(value, stamp(timestampMicros))
    binding.hasLast = true
    binding.lastBits = value
  }
}

/** A handle on a `float` topic. */
class FloatTopic internal constructor(registry: TelemetryRegistry, path: String, type: TelemetryType?) :
  TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: Float) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: Float, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: Float, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: Float, timestampMicros: Long) {
    val binding = binding()
    val bits = value.toRawBits().toLong()
    if (!keepDuplicates && binding.hasLast && binding.lastBits == bits) return
    binding.entry.logFloat(value, stamp(timestampMicros))
    binding.hasLast = true
    binding.lastBits = bits
  }
}

/** A handle on a `double` topic. */
class DoubleTopic internal constructor(registry: TelemetryRegistry, path: String, type: TelemetryType?) :
  TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: Double) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: Double, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: Double, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: Double, timestampMicros: Long) {
    val binding = binding()
    val bits = value.toRawBits()
    if (!keepDuplicates && binding.hasLast && binding.lastBits == bits) return
    binding.entry.logDouble(value, stamp(timestampMicros))
    binding.hasLast = true
    binding.lastBits = bits
  }
}

/**
 * A handle on a topic of reference values: strings, arrays, structs, and lists of structs.
 *
 * Numbers and booleans have their own handles, which don't box. A string that repeats is skipped
 * unless [keepDuplicates] is set. Nothing else is, since an array or object can change in place.
 */
class Topic<T : Any>
internal constructor(
  registry: TelemetryRegistry,
  path: String,
  type: TelemetryType?,
  private val write: (entry: TelemetryEntry, value: T, timestampMicros: Long) -> Unit,
) : TelemetryTopic(registry, path, type) {
  /** Logs [value], stamped with the current time. */
  fun set(value: T) = guarded { put(value, NOW) }

  /** Logs [value] with a timestamp in microseconds since the registry's origin. */
  fun set(value: T, timestampMicros: Long) = guarded { put(value, timestampMicros) }

  /** Logs [value], stamped with [at], a mark from the registry's time source. */
  fun set(value: T, at: ComparableTimeMark) = guarded { put(value, registry.timestampOf(at)) }

  private fun put(value: T, timestampMicros: Long) {
    val binding = binding()
    val isString = value is String
    if (isString && !keepDuplicates && binding.hasLast && binding.lastReference == value) return
    write(binding.entry, value, stamp(timestampMicros))
    if (isString) {
      binding.hasLast = true
      binding.lastReference = value
    }
  }
}
