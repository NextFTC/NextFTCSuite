/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.telemetry.struct.StructSchema
import dev.nextftc.telemetry.struct.StructSchemaRegistry
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/** Receives the warnings telemetry reports instead of throwing. */
fun interface TelemetryWarningHandler {
  fun warn(message: String)
}

/**
 * The set of [TelemetryBackend]s that logged values are routed to, plus the clock that stamps them.
 *
 * Most code uses [Telemetry], which wraps one shared registry. This is a class so a test can build
 * its own with a [kotlin.time.TestTimeSource] and a mock backend without affecting anything else.
 *
 * ## Timestamps
 *
 * Every value is stamped in microseconds since [origin], as measured by [timeSource]. Give the same
 * source and origin to a log writer and a server and they agree with this registry on every
 * timestamp.
 *
 * ## Backends
 *
 * A registered backend receives every value, or only the values whose path is under the prefixes it
 * was registered with. Backends can change while other threads are logging. Handles notice the
 * change on their next value and ask the new set of backends for their entries.
 *
 * ## Warnings
 *
 * Telemetry reports a wrong type, a value it can't log, or a backend that threw through
 * [warningHandler], then carries on. A logging mistake should not end a match. Each distinct message
 * is reported once, so a bad value logged every loop doesn't flood the output.
 *
 * @param timeSource The clock that stamps values
 * @param origin The moment timestamp zero refers to, a mark from [timeSource]. The default is the
 *   moment the registry is created.
 * @throws IllegalArgumentException if [origin] isn't from [timeSource], or is in the future
 */
class TelemetryRegistry
@JvmOverloads
constructor(
  val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
  val origin: ComparableTimeMark = timeSource.markNow(),
) {
  init {
    // Subtracting marks from different sources throws, which checks that they match.
    require(!(timeSource.markNow() - origin).isNegative()) { "The origin is in the future" }
  }

  /** Builds and keeps the schemas of the classes that are logged as structs. */
  val structs = StructSchemaRegistry()

  /** Returns the current time as a timestamp in microseconds since [origin]. */
  fun now(): Long = timestampOf(timeSource.markNow())

  /**
   * Returns [mark] as a timestamp in microseconds since [origin].
   *
   * @throws IllegalArgumentException if [mark] is from a different time source, or is earlier than
   *   [origin]
   */
  fun timestampOf(mark: ComparableTimeMark): Long {
    val micros = (mark - origin).inWholeMicroseconds
    require(micros >= 0) { "The mark is $micros microseconds before the origin" }
    return micros
  }

  private class Registration(val backend: TelemetryBackend, val prefixes: List<String>) {
    /** The type each path was given to the backend as, which a path keeps while it is registered. */
    val types = ConcurrentHashMap<String, TelemetryType>()

    fun matches(path: String): Boolean =
      prefixes.isEmpty() || prefixes.any { path == it || path.startsWith("$it/") }
  }

  private val registrations = CopyOnWriteArrayList<Registration>()

  /** Bumped whenever the backends change, which is how handles learn their entries are stale. */
  private val generationCounter = AtomicLong()
  internal val generation: Long
    get() = generationCounter.get()

  private val warned: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

  /**
   * Where warnings go. The default prints to standard error. A handler must not throw, and may be
   * called from any thread.
   */
  @Volatile
  var warningHandler: TelemetryWarningHandler = TelemetryWarningHandler {
    System.err.println("[NextFTC Telemetry] $it")
  }

  /** The root table, whose path is empty. */
  val root: TelemetryTable = TelemetryTable(this, "")

  /** Returns the table [name] under the root, creating it if needed. */
  fun table(name: String): TelemetryTable = root.table(name)

  /** How many backends are registered. */
  val backendCount: Int
    get() = registrations.size

  /**
   * Adds [backend]. It receives every value logged after this call, including on paths that were
   * already being logged.
   *
   * @param prefixes If any are given, the backend only receives values whose path is one of these
   *   or is under one, such as `/Drive`, which matches `/Drive/pose` but not `/Driver`. With none,
   *   it receives everything.
   */
  fun register(backend: TelemetryBackend, vararg prefixes: String) {
    registrations.add(Registration(backend, prefixes.map(::normalizePrefix).filter { it.isNotEmpty() }))
    generationCounter.incrementAndGet()
  }

  /**
   * Removes [backend] without closing it.
   *
   * @return `true` if it was registered
   */
  fun unregister(backend: TelemetryBackend): Boolean {
    val removed = registrations.removeAll { it.backend === backend }
    if (removed) generationCounter.incrementAndGet()
    return removed
  }

  /**
   * Closes and removes every backend, and forgets which warnings have been reported.
   *
   * A backend that throws on close is skipped with a warning, so one bad backend can't leave the rest
   * open.
   */
  fun reset() {
    val closing = registrations.map { it.backend }.distinct()
    registrations.clear()
    generationCounter.incrementAndGet()
    warned.clear()

    for (backend in closing) {
      try {
        backend.close()
      } catch (e: Exception) {
        warn("Backend $backend failed to close: $e")
      }
    }
  }

  /**
   * Reports [message] to [warningHandler], unless this exact message has been reported before. After
   * 1000 distinct messages, new ones are dropped.
   */
  fun warn(message: String) {
    // Past the cap, drop new messages instead of remembering them, so the set can't grow without
    // bound. A robot with this many distinct warnings has bigger problems than a repeated line.
    if (warned.size >= MAX_REMEMBERED_WARNINGS || !warned.add(message)) return

    try {
      warningHandler.warn(message)
    } catch (e: Exception) {
      // The handler is documented not to throw. If it does, there is nowhere left to report to.
    }
  }

  /**
   * Builds the entry a handle uses for [path]. It wraps one entry per backend that wants the path,
   * so a handle makes a single call, and a backend that fails can't stop the others.
   *
   * A backend is only ever given one type for a path. If a path that a backend already has as one
   * type is logged as another, the backend is left out of it and the mistake is reported. The
   * backend's types are forgotten when it is unregistered, so the next OpMode's backend starts clean.
   */
  internal fun entryFor(
    path: String,
    type: TelemetryType,
    properties: Map<String, Any> = emptyMap(),
  ): TelemetryEntry {
    val entries = ArrayList<TelemetryEntry>()
    for (registration in registrations) {
      if (!registration.matches(path)) continue
      val known = registration.types.putIfAbsent(path, type)
      if (known != null && known != type) {
        warn(
          "Could not log '$path' to ${registration.backend}: it already holds $known, but this is " +
            "$type. A path keeps one type for as long as a backend is registered, so use a " +
            "different name.",
        )
        continue
      }
      try {
        entries += registration.backend.entry(path, type, properties)
      } catch (e: Exception) {
        warn("Backend ${registration.backend} could not create an entry for '$path': $e")
      }
    }
    return if (entries.isEmpty()) DiscardEntry else FanOutEntry(entries, path, this)
  }

  private fun normalizePrefix(prefix: String): String {
    val trimmed = prefix.trim('/')
    return if (trimmed.isEmpty()) "" else "/$trimmed"
  }

  private companion object {
    const val MAX_REMEMBERED_WARNINGS = 1000
  }
}

/** Used when no backend wants a path. Drops every value. */
internal object DiscardEntry : TelemetryEntry {
  override fun logBoolean(value: Boolean, timestampMicros: Long) {}

  override fun logInteger(value: Long, timestampMicros: Long) {}

  override fun logFloat(value: Float, timestampMicros: Long) {}

  override fun logDouble(value: Double, timestampMicros: Long) {}

  override fun logString(value: String, timestampMicros: Long) {}

  override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) {}

  override fun logIntegerArray(value: LongArray, timestampMicros: Long) {}

  override fun logFloatArray(value: FloatArray, timestampMicros: Long) {}

  override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) {}

  override fun logStringArray(value: List<String>, timestampMicros: Long) {}

  override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) {}

  override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) {}
}

/** Sends each value to every backend's entry, so one failing backend can't starve the others. */
private class FanOutEntry(
  private val entries: List<TelemetryEntry>,
  private val path: String,
  private val registry: TelemetryRegistry,
) : TelemetryEntry {
  private inline fun each(block: (TelemetryEntry) -> Unit) {
    for (entry in entries) {
      try {
        block(entry)
      } catch (e: Exception) {
        registry.warn("Backend entry $entry failed to log to '$path': $e")
      }
    }
  }

  override fun logBoolean(value: Boolean, timestampMicros: Long) =
    each { it.logBoolean(value, timestampMicros) }

  override fun logInteger(value: Long, timestampMicros: Long) =
    each { it.logInteger(value, timestampMicros) }

  override fun logFloat(value: Float, timestampMicros: Long) = each { it.logFloat(value, timestampMicros) }

  override fun logDouble(value: Double, timestampMicros: Long) =
    each { it.logDouble(value, timestampMicros) }

  override fun logString(value: String, timestampMicros: Long) =
    each { it.logString(value, timestampMicros) }

  override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) =
    each { it.logBooleanArray(value, timestampMicros) }

  override fun logIntegerArray(value: LongArray, timestampMicros: Long) =
    each { it.logIntegerArray(value, timestampMicros) }

  override fun logFloatArray(value: FloatArray, timestampMicros: Long) =
    each { it.logFloatArray(value, timestampMicros) }

  override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) =
    each { it.logDoubleArray(value, timestampMicros) }

  override fun logStringArray(value: List<String>, timestampMicros: Long) =
    each { it.logStringArray(value, timestampMicros) }

  override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) =
    each { it.logStruct(schema, value, timestampMicros) }

  override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) =
    each { it.logStructArray(schema, values, timestampMicros) }
}
