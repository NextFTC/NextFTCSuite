/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.sensors.colors

import android.graphics.Color
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The Android unit-test stub for [android.graphics.Color] throws (or, with
 * `isReturnDefaultValues`, silently no-ops) instead of doing real HSV/RGB conversion. This
 * installs a real implementation of the handful of static methods this module relies on, so
 * conversion-dependent logic (like [ColorProfile] HSV matching) can be tested without pulling in
 * Robolectric.
 */
internal fun installFakeAndroidColor() {
  mockkStatic(Color::class)
  every { Color.RGBToHSV(any(), any(), any(), any()) } answers {
    val r = firstArg<Int>()
    val g = secondArg<Int>()
    val b = thirdArg<Int>()
    val out = arg<FloatArray>(3)
    val (h, s, v) = rgbToHsv(r, g, b)
    out[0] = h
    out[1] = s
    out[2] = v
  }
  every { Color.HSVToColor(any()) } answers {
    val hsv = firstArg<FloatArray>()
    val (r, g, b) = hsvToRgb(hsv[0], hsv[1], hsv[2])
    (0xFF shl 24) or (r shl 16) or (g shl 8) or b
  }
  every { Color.red(any()) } answers { (firstArg<Int>() shr 16) and 0xFF }
  every { Color.green(any()) } answers { (firstArg<Int>() shr 8) and 0xFF }
  every { Color.blue(any()) } answers { firstArg<Int>() and 0xFF }
}

internal fun uninstallFakeAndroidColor() {
  unmockkStatic(Color::class)
}

private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
  val rf = r / 255f
  val gf = g / 255f
  val bf = b / 255f
  val max = maxOf(rf, gf, bf)
  val min = minOf(rf, gf, bf)
  val delta = max - min
  val v = max
  val s = if (max == 0f) 0f else delta / max
  var h = when {
    delta == 0f -> 0f
    max == rf -> 60f * (((gf - bf) / delta).mod(6f))
    max == gf -> 60f * (((bf - rf) / delta) + 2f)
    else -> 60f * (((rf - gf) / delta) + 4f)
  }
  if (h < 0f) h += 360f
  return Triple(h, s, v)
}

private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Int, Int, Int> {
  val c = v * s
  val x = c * (1 - abs((h / 60f) % 2 - 1))
  val m = v - c
  val (rp, gp, bp) = when {
    h < 60f -> Triple(c, x, 0f)
    h < 120f -> Triple(x, c, 0f)
    h < 180f -> Triple(0f, c, x)
    h < 240f -> Triple(0f, x, c)
    h < 300f -> Triple(x, 0f, c)
    else -> Triple(c, 0f, x)
  }
  val r = ((rp + m) * 255).roundToInt().coerceIn(0, 255)
  val g = ((gp + m) * 255).roundToInt().coerceIn(0, 255)
  val b = ((bp + m) * 255).roundToInt().coerceIn(0, 255)
  return Triple(r, g, b)
}
