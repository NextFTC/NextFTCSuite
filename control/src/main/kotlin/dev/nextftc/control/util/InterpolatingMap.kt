/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.control.util

import java.util.NavigableMap
import java.util.TreeMap

/**
 * A navigable map that supports flexible interpolation between values.
 *
 * `InterpolatingMap` is a wrapper around a `TreeMap<Double, T>` that queries a custom interpolation
 * strategy to approximate values between stored keys. Exact matches return stored values; intermediate
 * keys are interpolated using the configured strategy.
 *
 * ## Interpolation Strategies
 *
 * Two interpolation paradigms are supported:
 *
 * **Local Interpolation**: Uses an `(T, T, Double) -> T` function operating on two adjacent control
 * points and a blend parameter \(t \in [0.0, 1.0]\). Efficient and suitable for linear, polynomial,
 * and other pointwise methods.
 *
 * **Global Interpolation**: Uses a `(NavigableMap<Double, T>).(Double) -> T` lambda with access to
 * all control points. Enables spline interpolation and methods requiring multi-point context.
 *
 * ## Usage Examples
 *
 * **Linear interpolation** (local):
 * ```kotlin
 * val map = InterpolatingMap.linear()
 * map[0.0] = 0.0
 * map[1.0] = 1.0
 * map[2.0] = 4.0
 * println(map[0.5])  // 0.5 (linearly interpolated)
 * ```
 *
 * **Catmull-Rom spline interpolation** (local):
 * ```kotlin
 * val map = InterpolatingMap.spline()
 * map[0.0] = 0.0
 * map[1.0] = 1.0
 * map[2.0] = 0.5
 * println(map[1.5])  // Smooth spline-interpolated value
 * ```
 *
 * @param T the value type stored in the map
 * @property tree the underlying sorted `TreeMap<Double, T>` backing this map
 * @property getter a function accepting a key and returning an interpolated or exact value,
 *   with receiver access to the full `NavigableMap` for global context
 *
 * @constructor Creates an empty map with a custom getter function (global interpolation)
 * @constructor Creates an empty map with a local interpolation function
 * @constructor Creates a map initialized with keys and values, using a custom getter
 * @constructor Creates a map initialized with keys and values, using a local interpolator
 *
 * @throws IllegalArgumentException if keys and values lists have differing lengths
 */
class InterpolatingMap<T> private constructor(
  private val tree: TreeMap<Double, T>,
  private val getter: NavigableMap<Double, T>.(Double) -> T,
) : NavigableMap<Double, T> by tree {
  constructor(tree: TreeMap<Double, T>, interpolate: (T, T, Double) -> T) : this(tree, { key ->
    val low = floorEntry(key)
    val high = ceilingEntry(key)

    if (low.key == high.key) {
      tree[key]!!
    }

    val t = lerp(key, inputMin = low.key, inputMax = high.key, outputMin = 0.0, outputMax = 1.0)

    interpolate(low.value, high.value, t)
  })

  constructor(getter: NavigableMap<Double, T>.(Double) -> T) : this(TreeMap(), getter)

  constructor(interpolator: (T, T, Double) -> T) : this(TreeMap(), interpolator)

  constructor(
    getter: NavigableMap<Double, T>.(Double) -> T,
    keys: List<Double>,
    values: List<T>,
  ) : this(
    TreeMap(),
    getter,
  ) {
    require(keys.size == values.size) { "Keys and values must be the same size" }
    for (i in keys.indices) {
      tree[keys[i]] = values[i]
    }
  }

  constructor(interpolator: (T, T, Double) -> T, keys: List<Double>, values: List<T>) :
    this(TreeMap(), interpolator) {
    require(keys.size == values.size) { "Keys and values must be the same size" }
    for (i in keys.indices) {
      tree[keys[i]] = values[i]
    }
  }

  /**
   * Gets the value associated with the given key, using interpolation if necessary.
   *
   * If the key exists in the map, the stored value is returned. Otherwise, the value is
   * interpolated between the two nearest keys using the configured interpolation strategy.
   *
   * @param key the lookup key
   * @return the exact value if the key exists, or an interpolated value otherwise
   */
  override fun get(key: Double): T = this.getter(key)

  companion object {
    /**
     * @usesMathJax
     *
     * Creates an empty map with linear interpolation.
     *
     * Uses linear interpolation to approximate values between control points:
     * \(y = y_1 + (y_2 - y_1) \cdot t\) where \(t \in [0.0, 1.0]\).
     *
     * Linear interpolation is straightforward, efficient, and suitable for most general-purpose use cases.
     *
     * Example:
     * ```kotlin
     * val map = InterpolatingMap.linear()
     * map[0.0] = 0.0
     * map[1.0] = 1.0
     * val value = map[0.5]  // 0.5
     * ```
     *
     * @return an empty InterpolatingMap with linear interpolation
     */
    @JvmStatic fun linear() = InterpolatingMap(::lerp)

    /**
     * Creates a map with linear interpolation initialized with given control points.
     *
     * Uses linear interpolation to approximate values between control points.
     *
     * @param keys the x-coordinates of the control points (will be sorted)
     * @param values the y-coordinates of the control points
     * @return an InterpolatingMap with the given data and linear interpolation
     * @throws IllegalArgumentException if keys.size != values.size
     */
    @JvmStatic fun linear(keys: List<Double>, values: List<Double>) = InterpolatingMap(
      ::lerp,
      keys,
      values,
    )

    /**
     * Creates an empty map with Catmull-Rom spline interpolation.
     *
     * Uses Catmull-Rom spline interpolation to provide smooth, continuous curves
     * that pass through all control points. This method uses neighboring points to compute
     * smooth tangents, resulting in visually pleasing interpolation.
     *
     * Suitable for motion profiles, smooth animations, and other applications requiring
     * smooth interpolation across multiple points.
     *
     * Example:
     * ```kotlin
     * val map = InterpolatingMap.spline()
     * map[0.0] = 0.0
     * map[1.0] = 1.0
     * map[2.0] = 0.5
     * val value = map[1.5]  // Smooth spline interpolation
     * ```
     *
     * @return an empty InterpolatingMap with Catmull-Rom spline interpolation
     */
    @JvmStatic fun spline() = InterpolatingMap(::splerp)

    /**
     * Creates a map with Catmull-Rom spline interpolation initialized with given control points.
     *
     * Uses Catmull-Rom spline interpolation to provide smooth, continuous curves
     * that pass through all control points.
     *
     * @param keys the x-coordinates of the control points (will be sorted)
     * @param values the y-coordinates of the control points
     * @return an InterpolatingMap with the given data and Catmull-Rom spline interpolation
     * @throws IllegalArgumentException if keys.size != values.size
     */
    @JvmStatic fun spline(keys: List<Double>, values: List<Double>) = InterpolatingMap(
      ::splerp,
      keys,
      values,
    )
  }
}
