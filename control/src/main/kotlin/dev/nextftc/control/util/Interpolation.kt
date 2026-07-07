/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Interpolation")

package dev.nextftc.control.util

import dev.nextftc.linalg.Matrix
import dev.nextftc.linalg.Nat
import dev.nextftc.units.Measure
import dev.nextftc.units.Unit
import java.util.NavigableMap

/**
 * @usesMathJax
 *
 * Linearly interpolates (remaps) a value from one range to another.
 *
 * Maps [value] from the input range \([inputMin, inputMax]\) to the output range \([outputMin, outputMax]\).
 *
 * @param value The value to interpolate
 * @param inputMin The lower bound of the input range
 * @param inputMax The upper bound of the input range
 * @param outputMin The lower bound of the output range
 * @param outputMax The upper bound of the output range
 * @return The interpolated value in the output range
 */
fun lerp(value: Double, inputMin: Double, inputMax: Double, outputMin: Double, outputMax: Double) =
  if (inputMin == inputMax) {
    0.0
  } else {
    outputMin + (value - inputMin) * (outputMax - outputMin) / (inputMax - inputMin)
  }

/**
 * @usesMathJax
 *
 * Linearly interpolates between \(low\) and \(high\) at value [t].
 *
 * @param [t] interpolation factor, typically in the range \([0, 1]\)
 */
fun lerp(low: Double, high: Double, t: Double) = low + (high - low) * t

/**
 * @usesMathJax
 *
 * Inverse of [lerp]. Given a value [value] in the range \([low, high]\), returns the interpolation factor
 * \(t\) such that \(\text{lerp}(low, high, t) = value\).
 *
 * @param [value] value in the range \([low, high]\)
 * @return interpolation factor \(t\)
 */
fun antiLerp(value: Double, low: Double, high: Double) = (value - low) / (high - low)

/**
 * Searches for the nearest value in [source] to the given [query] value.
 * If [query] exactly matches a value in [source], the corresponding value in [target] is returned.
 * If not, the two nearest values in [source] are found, and linear interpolation is performed
 * between the corresponding values in [target].
 *
 * @param [source] sorted list of source values
 * @param [target] sorted list of target values
 * @param [query] value to search for
 */
fun lerpLookup(source: List<Double>, target: List<Double>, query: Double): Double {
  require(source.size == target.size) {
    "source.size (${source.size}) != target.size (${target.size})"
  }
  require(source.isNotEmpty()) { "source is empty" }

  val index = source.binarySearch(query)
  return if (index >= 0) {
    target[index]
  } else {
    val insIndex = -(index + 1)
    when {
      insIndex <= 0 -> target.first()
      insIndex >= source.size -> target.last()
      else -> {
        val sLo = source[insIndex - 1]
        val sHi = source[insIndex]
        val tLo = target[insIndex - 1]
        val tHi = target[insIndex]
        lerp(query, inputMin = sLo, inputMax = sHi, outputMin = tLo, outputMax = tHi)
      }
    }
  }
}

/**
 * Linearly interpolates between two matrices at value [t].
 *
 * @param t interpolation factor, typically in the range \([0, 1]\)
 * @return interpolated matrix
 */
fun <R : Nat, C : Nat> lerpMatrix(t: Double, low: Matrix<R, C>, high: Matrix<R, C>) = low + (high - low) * t

/**
 * Linearly interpolates between two measures at value [t].
 *
 * @param t interpolation factor, typically in the range \([0, 1]\)
 * @return interpolated measure
 */
fun <U : Unit<U>> lerpMeasure(t: Double, low: Measure<U>, high: Measure<U>) = low + (high - low) * t

/**
 * @usesMathJax
 *
 * Performs Catmull-Rom spline interpolation between two points.
 *
 * This function implements cubic spline interpolation using the Catmull-Rom formulation,
 * which provides smooth curves through control points with continuous first derivatives.
 *
 * @param t interpolation factor, in the range \([0, 1]\) where:
 *   - \(t = 0\) returns \(p_1\)
 *   - \(t = 1\) returns \(p_2\)
 * @param p0 the first control point (used for computing tangent at p1)
 * @param p1 the start point of the interpolation segment
 * @param p2 the end point of the interpolation segment
 * @param p3 the fourth control point (used for computing tangent at p2)
 * @return the interpolated value at parameter \(t\)
 */
fun splerp(t: Double, p0: Double, p1: Double, p2: Double, p3: Double) = 0.5 *
  (
    (2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t +
      (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t
    )

/**
 * @usesMathJax
 *
 * Performs Catmull-Rom spline interpolation at a given key in a navigable map.
 *
 * Uses the two closest entries below and above the key as the main interpolation points,
 * and their neighbors to compute proper tangents for smooth interpolation. Falls back to
 * linear interpolation if neighbors are not available.
 *
 * @param map the navigable map containing control points
 * @param key the key to interpolate at
 * @return the spline-interpolated value
 * @throws NoSuchElementException if the map is empty or key is out of bounds
 */
internal fun splerp(map: NavigableMap<Double, Double>, key: Double): Double {
  require(map.isNotEmpty()) { "Map cannot be empty" }

  val low = map.floorEntry(key) ?: throw NoSuchElementException("No entry <= $key")
  val high = map.ceilingEntry(key) ?: throw NoSuchElementException("No entry >= $key")

  // If exact match, return the value
  if (low.key == high.key) {
    return high.value
  }

  // Normalize t to [0, 1]
  val t = lerp(key, inputMin = low.key, inputMax = high.key, outputMin = 0.0, outputMax = 1.0)

  // Get the previous and next neighbors for tangent computation
  val p0 = map.lowerEntry(low.key)?.value ?: low.value
  val p1 = low.value
  val p2 = high.value
  val p3 = map.higherEntry(high.key)?.value ?: high.value

  return splerp(t, p0, p1, p2, p3)
}

fun bilerp(
  x: Double,
  y: Double,
  x0: Double,
  y0: Double,
  x1: Double,
  y1: Double,
  q00: Double,
  q10: Double,
  q01: Double,
  q11: Double,
): Double {
  val tx = (x - x0) / (x1 - x0)
  val ty = (y - y0) / (y1 - y0)

  val bottom = q00 * (1 - tx) + q10 * tx
  val top = q01 * (1 - tx) + q11 * tx

  return bottom * (1 - ty) + top * ty
}
