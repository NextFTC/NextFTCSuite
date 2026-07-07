/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.util

import java.util.TreeMap

/**
 * @usesMathJax
 *
 * A 2D map that supports flexible interpolation between Double values.
 *
 * `InterpolatingMap2D` is a wrapper around a 2D grid of `TreeMap<Double, TreeMap<Double, Double>>` that
 * interpolates values between stored keys using a custom interpolation strategy. Exact matches
 * return stored values; intermediate keys are interpolated using the configured strategy.
 *
 * ## Interpolation Strategies
 *
 * Custom interpolation functions accept four corner values and blend parameters and return
 * an interpolated result: `(q00: Double, q10: Double, q01: Double, q11: Double, tx: Double, ty: Double) -> Double`
 *
 * where:
 * - \(q_{00}, q_{10}, q_{01}, q_{11}\) are the values at corners \((x_0, y_0), (x_1, y_0), (x_0, y_1), (x_1, y_1)\)
 * - \(t_x = \frac{x - x_0}{x_1 - x_0}\) and \(t_y = \frac{y - y_0}{y_1 - y_0}\) are blend parameters in \([0, 1]\)
 *
 * ## Bilinear Interpolation (Default)
 *
 * The default interpolation uses bilinear blending:
 *
 * \(q(x, y) = (1 - t_x)(1 - t_y) \cdot q_{00} + t_x(1 - t_y) \cdot q_{10} + (1 - t_x) t_y \cdot q_{01} + t_x t_y \cdot q_{11}\)
 *
 * ## Usage Examples
 *
 * **Basic 2D bilinear interpolation**:
 * ```kotlin
 * val map = InterpolatingMap2D.bilinear()
 * map[0.0, 0.0] = 0.0
 * map[1.0, 0.0] = 1.0
 * map[0.0, 1.0] = 2.0
 * map[1.0, 1.0] = 3.0
 * println(map[0.5, 0.5])  // 1.5 (bilinearly interpolated)
 * ```
 *
 * **With lookup table**:
 * ```kotlin
 * val xKeys = listOf(0.0, 1.0, 2.0)
 * val yKeys = listOf(0.0, 1.0, 2.0)
 * val values = listOf(
 *     listOf(0.0, 1.0, 2.0),
 *     listOf(1.0, 2.0, 3.0),
 *     listOf(2.0, 3.0, 4.0)
 * )
 * val map = InterpolatingMap2D.bilinear(xKeys, yKeys, values)
 * println(map[0.5, 0.5])  // Interpolated value
 * ```
 *
 * @property grid the underlying 2D `TreeMap<Double, TreeMap<Double, Double>>` structure
 * @property interpolate a function that performs interpolation given four corner values and blend parameters
 *
 * @constructor Creates an empty 2D map with a custom interpolation function
 * @constructor Creates a 2D map initialized with x-keys, y-keys, values, and custom interpolation
 *
 * @throws IllegalArgumentException if yKeys.size != values.size or if any row has inconsistent column count
 */
class InterpolatingMap2D(
  private val interpolate: (Double, Double, Double, Double, Double, Double) -> Double,
  private val grid: TreeMap<Double, TreeMap<Double, Double>> = TreeMap(),
) {
  constructor(
    interpolate: (Double, Double, Double, Double, Double, Double) -> Double,
    xKeys: List<Double>,
    yKeys: List<Double>,
    values: List<List<Double>>,
  ) : this(interpolate, TreeMap()) {
    require(values.size == yKeys.size) {
      "values.size (${values.size}) != yKeys.size (${yKeys.size})"
    }
    require(values.all { it.size == xKeys.size }) { "All rows must have ${xKeys.size} columns" }

    for (i in yKeys.indices) {
      val row = TreeMap<Double, Double>()
      for (j in xKeys.indices) {
        row[xKeys[j]] = values[i][j]
      }
      grid[yKeys[i]] = row
    }
  }

  /**
   * Gets the value at the given coordinates, using interpolation if necessary.
   *
   * If both coordinates exist exactly in the grid, the stored value is returned.
   * Otherwise, the configured interpolation strategy is used between the four nearest grid points.
   *
   * @param x the x-coordinate to query
   * @param y the y-coordinate to query
   * @return the exact value if coordinates match, or an interpolated value otherwise
   * @throws NoSuchElementException if coordinates are outside the grid bounds
   */
  operator fun get(x: Double, y: Double): Double {
    require(grid.isNotEmpty()) { "Grid is empty" }

    val y0Entry = grid.floorEntry(y) ?: throw NoSuchElementException("No entry with y <= $y")
    val y1Entry = grid.ceilingEntry(y) ?: throw NoSuchElementException("No entry with y >= $y")

    val xRow0 = y0Entry.value
    val xRow1 = y1Entry.value

    require(xRow0.isNotEmpty()) { "Row at y=${y0Entry.key} is empty" }
    require(xRow1.isNotEmpty()) { "Row at y=${y1Entry.key} is empty" }

    val x0Entry = xRow0.floorEntry(x) ?: throw NoSuchElementException("No entry with x <= $x")
    val x1Entry = xRow0.ceilingEntry(x) ?: throw NoSuchElementException("No entry with x >= $x")
    val x0Entry1 = xRow1.floorEntry(x) ?: throw NoSuchElementException("No entry with x <= $x")
    val x1Entry1 =
      xRow1.ceilingEntry(x) ?: throw NoSuchElementException("No entry with x >= $x")

    // Exact match
    if (x0Entry.key == x1Entry.key && y0Entry.key == y1Entry.key) {
      return x0Entry.value
    }

    val tx = if (x0Entry.key ==
      x1Entry.key
    ) {
      0.0
    } else {
      (x - x0Entry.key) / (x1Entry.key - x0Entry.key)
    }
    val ty = if (y0Entry.key ==
      y1Entry.key
    ) {
      0.0
    } else {
      (y - y0Entry.key) / (y1Entry.key - y0Entry.key)
    }

    return interpolate(x0Entry.value, x1Entry.value, x0Entry1.value, x1Entry1.value, tx, ty)
  }

  /**
   * Sets the value at the given coordinates.
   *
   * @param x the x-coordinate
   * @param y the y-coordinate
   * @param value the value to store
   */
  operator fun set(x: Double, y: Double, value: Double) {
    grid.computeIfAbsent(y) { TreeMap() }[x] = value
  }

  companion object {
    /**
     * @usesMathJax
     *
     * Creates an empty map with bilinear interpolation.
     *
     * Uses bilinear interpolation to approximate values between grid points using the formula:
     *
     * \(q(x, y) = (1 - t_x)(1 - t_y) \cdot q_{00} + t_x(1 - t_y) \cdot q_{10} + (1 - t_x) t_y \cdot q_{01} + t_x t_y \cdot q_{11}\)
     *
     * This is the most common interpolation method for 2D lookup tables.
     *
     * @return an empty InterpolatingMap2D with bilinear interpolation
     */
    @JvmStatic
    fun bilinear(): InterpolatingMap2D = InterpolatingMap2D({
        q00: Double,
        q10: Double,
        q01: Double,
        q11: Double,
        tx: Double,
        ty: Double,
      ->
      val bottom = q00 * (1 - tx) + q10 * tx
      val top = q01 * (1 - tx) + q11 * tx
      bottom * (1 - ty) + top * ty
    })

    /**
     * Creates a map with bilinear interpolation initialized with given grid data.
     *
     * @param xKeys the x-coordinates of the grid points (will be sorted)
     * @param yKeys the y-coordinates of the grid points (will be sorted)
     * @param values a 2D list where `values[yIndex][xIndex]` corresponds to point
     * `(xKeys[xIndex], yKeys[yIndex])`
     * @return an InterpolatingMap2D with the given data and bilinear interpolation
     * @throws IllegalArgumentException if dimensions don't match
     */
    @JvmStatic
    fun bilinear(
      xKeys: List<Double>,
      yKeys: List<Double>,
      values: List<List<Double>>,
    ): InterpolatingMap2D = InterpolatingMap2D(
      { q00: Double, q10: Double, q01: Double, q11: Double, tx: Double, ty: Double ->
        val bottom = q00 * (1 - tx) + q10 * tx
        val top = q01 * (1 - tx) + q11 * tx
        bottom * (1 - ty) + top * ty
      },
      xKeys,
      yKeys,
      values,
    )
  }
}
