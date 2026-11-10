/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import dev.nextftc.telemetry.TelemetryBackend
import dev.nextftc.telemetry.TelemetryEntry
import dev.nextftc.telemetry.TelemetryType
import dev.nextftc.util.struct.StructSchema
import java.util.concurrent.ConcurrentHashMap
import org.firstinspires.ftc.robotcore.external.Telemetry as SdkTelemetry

/**
 * Shows logged values on the Driver Station, one line per path, using an FTC SDK [SdkTelemetry].
 *
 * Each path gets a retained line, captioned with the path and updated in place, so the lines survive
 * the SDK's auto-clear and don't disturb ones the OpMode adds itself. Logging never calls
 * [SdkTelemetry.update], so a value reaches the Driver Station on the next [update]. The SDK calls it
 * after every loop of an iterative `OpMode`, but not for a `LinearOpMode`, which has to call
 * `telemetry.update()` itself or [FtcTelemetry.update]. Until then, the Driver Station shows the
 * previous values.
 *
 * Values are shown as text. A unit property is added after the value, and a struct or an array is
 * shown with its `toString` or contents. The Driver Station has limited room, so register this with
 * path prefixes if the log is large.
 *
 * After the backend is closed, its lines are removed and its entries drop values quietly.
 *
 * @param telemetry The telemetry of the OpMode that is running
 */
class DriverStationBackend(private val telemetry: SdkTelemetry) : TelemetryBackend {
  private val items = ConcurrentHashMap<String, SdkTelemetry.Item>()

  @Volatile private var closed = false

  override fun entry(path: String, type: TelemetryType, properties: Map<String, Any>): TelemetryEntry {
    val unit = (properties["unit"] as? String)?.takeIf { it.isNotEmpty() }
    val item = items[path] ?: run {
      // An Any, so the SDK doesn't treat the value as a format string
      val empty: Any = ""
      val added: SdkTelemetry.Item? = telemetry.addData(path.removePrefix("/"), empty)
      added?.also { it.setRetained(true) }?.let { items.putIfAbsent(path, it) ?: it }
    }
    return LineEntry(item, unit)
  }

  /** Sends the lines to the Driver Station, like `telemetry.update()`. */
  fun update() {
    if (!closed) telemetry.update()
  }

  override fun close() {
    closed = true
    for (item in items.values) {
      try {
        telemetry.removeItem(item)
      } catch (_: Exception) {
        // The OpMode is ending, and the Driver Station clears its lines anyway.
      }
    }
    items.clear()
  }

  override fun toString() = "DriverStationBackend"

  private inner class LineEntry(private val item: SdkTelemetry.Item?, private val unit: String?) :
    TelemetryEntry {
    private fun show(text: String) {
      if (closed || item == null) return
      val line: Any = if (unit == null) text else "$text $unit"
      item.setValue(line)
    }

    override fun logBoolean(value: Boolean, timestampMicros: Long) = show(value.toString())

    override fun logInteger(value: Long, timestampMicros: Long) = show(value.toString())

    override fun logFloat(value: Float, timestampMicros: Long) = show(value.toString())

    override fun logDouble(value: Double, timestampMicros: Long) = show(value.toString())

    override fun logString(value: String, timestampMicros: Long) = show(value)

    override fun logBooleanArray(value: BooleanArray, timestampMicros: Long) = show(value.contentToString())

    override fun logIntegerArray(value: LongArray, timestampMicros: Long) = show(value.contentToString())

    override fun logFloatArray(value: FloatArray, timestampMicros: Long) = show(value.contentToString())

    override fun logDoubleArray(value: DoubleArray, timestampMicros: Long) = show(value.contentToString())

    override fun logStringArray(value: List<String>, timestampMicros: Long) = show(value.toString())

    override fun <T> logStruct(schema: StructSchema<T>, value: T, timestampMicros: Long) =
      show(value.toString())

    override fun <T> logStructArray(schema: StructSchema<T>, values: List<T>, timestampMicros: Long) =
      show(values.toString())
  }
}
