/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import dev.nextftc.units.unittypes.MulUnit
import dev.nextftc.units.unittypes.PerUnit
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Knows how to publish values of a type that can't publish themselves, such as one from a library.
 *
 * It is to [TelemetryLoggable] what `Comparator<T>` is to `Comparable<T>`: [TelemetryLoggable] is
 * implemented by the type, and a handler is supplied from outside for a type you don't own. Register
 * one with [TelemetryTypeHandlers.register], and [TelemetryTable.log] uses it for values of that type.
 *
 * ```kotlin
 * Telemetry.registry.handlers.register<Color> { table, name, color ->
 *   table.log(name, color.toHexString())
 * }
 * ```
 */
fun interface TelemetryTypeHandler<T : Any> {
  /** Logs [value] under [name] in [table]. */
  fun log(table: TelemetryTable, name: String, value: T)
}

/**
 * The [TelemetryTypeHandler]s of a [TelemetryRegistry], by the type they handle.
 *
 * A handler applies to its type and to every subtype. For a value, the handler registered for the
 * nearest type wins: its own class, then its superclasses from the closest, then its interfaces. A
 * handler beats the value's own [TelemetryLoggable], like a `Comparator` beats natural ordering,
 * but not the types that always have a fixed encoding, such as numbers, strings and primitive
 * arrays.
 *
 * Handlers for `Measure` are registered by default.
 */
class TelemetryTypeHandlers internal constructor() {
  private val handlers = ConcurrentHashMap<Class<*>, TelemetryTypeHandler<*>>()

  // The handler found for each class that has been looked up, with a marker for "none".
  private val resolved = ConcurrentHashMap<Class<*>, Any>()

  init {
    register(Measure::class, MeasureHandler)
  }

  /** Uses [handler] for values of [type] and its subtypes, replacing the one registered for [type]. */
  fun <T : Any> register(type: KClass<T>, handler: TelemetryTypeHandler<T>) {
    handlers[type.java] = handler
    resolved.clear()
  }

  /** Uses [handler] for values of [T] and its subtypes, replacing the one registered for [T]. */
  inline fun <reified T : Any> register(handler: TelemetryTypeHandler<T>) = register(T::class, handler)

  /** Stops using the handler registered for [type], if any. */
  fun unregister(type: KClass<*>) {
    handlers.remove(type.java)
    resolved.clear()
  }

  /** Returns the handler for values of [type], or null if there isn't one. */
  fun find(type: Class<*>): TelemetryTypeHandler<Any>? {
    val handler = resolved.getOrPut(type) { search(type) ?: None }
    @Suppress("UNCHECKED_CAST")
    return if (handler === None) null else handler as TelemetryTypeHandler<Any>
  }

  private fun search(type: Class<*>): TelemetryTypeHandler<*>? {
    var current: Class<*>? = type
    while (current != null) {
      handlers[current]?.let { return it }
      current = current.superclass
    }
    // Interfaces, breadth first from the type's own, so the closest wins.
    val queue = ArrayDeque<Class<*>>()
    var cls: Class<*>? = type
    while (cls != null) {
      queue.addAll(cls.interfaces)
      cls = cls.superclass
    }
    val seen = HashSet<Class<*>>()
    while (queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (!seen.add(next)) continue
      handlers[next]?.let { return it }
      queue.addAll(next.interfaces)
    }
    return null
  }

  private object None
}

/**
 * Logs a [Measure] as a `double` in its base unit, with that unit's symbol as the topic's unit, so
 * `12.0.inches` is logged as `0.3048` in `m`.
 */
private object MeasureHandler : TelemetryTypeHandler<Measure<*>> {
  override fun log(table: TelemetryTable, name: String, value: Measure<*>) {
    val topic = table.doubleTopic(name)
    if (topic.unit == null) topic.unit = baseSymbolOf(value.unit)
    topic.set(value.baseUnitMagnitude)
  }

  // Uses component unit symbols for compound units
  private fun baseSymbolOf(unit: Unit<*>): String = when (unit) {
    is PerUnit<*, *> -> "${baseSymbolOf(unit.numerator)}/${baseSymbolOf(unit.denominator)}"
    is MulUnit<*, *> -> "${baseSymbolOf(unit.first)}⋅${baseSymbolOf(unit.second)}"
    else -> unit.baseUnit.symbol()
  }
}
