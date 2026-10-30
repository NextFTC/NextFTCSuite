/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The names of topic properties that dashboards understand.
 *
 * A property is a named setting on a topic, such as the unit its values are in. Set them on a handle
 * with [TelemetryTopic.properties], or [withUnit] for the common one. Values are strings, booleans or
 * finite numbers.
 */
object TelemetryProperties {
  /** The unit of the topic's values, such as `m`, `rad`, or `V`. AdvantageScope shows and converts it. */
  const val UNIT = "unit"
}

/**
 * Sets the unit of [this] topic's values, which dashboards show, and returns the handle.
 *
 * ```kotlin
 * private val velocity = table.doubleTopic("wheelVelocity").withUnit("m/s")
 * ```
 *
 * Properties are sent when the path is first published, so set them before the first value.
 */
fun <T : TelemetryTopic> T.withUnit(unit: String): T = apply { this.unit = unit }

/** Sets [properties] on [this] topic, in addition to the ones it has, and returns the handle. */
fun <T : TelemetryTopic> T.withProperties(vararg properties: Pair<String, Any>): T = apply {
  this.properties = this.properties + properties
}

/** Whether [value] is something a property can hold. */
internal fun isValidPropertyValue(value: Any?): Boolean = when (value) {
  is String,
  is Boolean,
  -> true
  is Double -> value.isFinite()
  is Float -> value.isFinite()
  is Number -> true
  else -> false
}

/**
 * Returns these properties as the JSON object that goes in an NT4 `announce`, or in the metadata of
 * a log entry.
 *
 * @throws IllegalArgumentException if a value isn't a string, boolean, or finite number
 */
internal fun Map<String, Any>.toJsonObject(): JsonObject = buildJsonObject {
  for ((key, value) in this@toJsonObject) {
    require(isValidPropertyValue(value)) { "Property '$key' can't be $value" }
    when (value) {
      is String -> put(key, value)
      is Boolean -> put(key, value)
      is Number -> put(key, value)
    }
  }
}
