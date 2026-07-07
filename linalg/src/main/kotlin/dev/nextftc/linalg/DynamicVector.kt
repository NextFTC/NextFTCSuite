/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Vectors")

package dev.nextftc.linalg

import org.ejml.simple.SimpleMatrix

/**
 * Represents a column vector of doubles with dynamic (runtime-checked) dimensions.
 * This is essentially a [DynamicMatrix] with a single column.
 */
class DynamicVector internal constructor(simple: SimpleMatrix) : DynamicMatrix(simple) {
  init {
    require(simple.numCols() == 1) { "Vector must have exactly one column" }
  }

  /**
   * Constructor to create a [DynamicVector] from an array of values.
   */
  constructor(data: DoubleArray) : this(SimpleMatrix(data.size, 1, false, data))

  /**
   * Constructor to create a [DynamicVector] from a collection of values.
   */
  constructor(data: Collection<Double>) : this(data.toDoubleArray())

  companion object {
    /**
     * Creates a zero vector with the given size.
     */
    @JvmStatic
    fun zero(size: Int) = DynamicVector(SimpleMatrix(size, 1))

    /**
     * Creates a vector from the given values.
     */
    @JvmStatic
    fun of(vararg data: Double) = DynamicVector(data)

    /**
     * Creates a vector from a collection.
     */
    @JvmStatic
    fun of(data: Collection<Double>) = DynamicVector(data)
  }

  /** The dimension (length) of this vector. */
  @JvmField
  val dimension: Int = numRows

  /** Returns the element at the given index. */
  operator fun get(i: Int): Double = simple[i, 0]

  /** Sets the element at the given index. */
  operator fun set(i: Int, value: Double) {
    simple[i, 0] = value
  }

  /** Returns a copy of this vector. */
  override fun copy(): DynamicVector = DynamicVector(simple.copy())

  /** Negates all elements of this vector. */
  override operator fun unaryMinus(): DynamicVector = DynamicVector(simple.negative())

  /** Adds another vector to this vector. */
  operator fun plus(other: DynamicVector): DynamicVector = DynamicVector(simple + other.simple)

  /** Subtracts another vector from this vector. */
  operator fun minus(other: DynamicVector): DynamicVector = DynamicVector(simple - other.simple)

  /** Multiplies this vector by a scalar. */
  override operator fun times(scalar: Double): DynamicVector = DynamicVector(simple.scale(scalar))

  /** Multiplies this vector by a scalar. */
  override operator fun times(scalar: Number): DynamicVector = times(scalar.toDouble())

  /** Computes the dot product of this vector with another vector. */
  fun dot(other: DynamicVector): Double = simple.transpose().mult(other.simple)[0, 0]

  /** Returns the Euclidean norm (magnitude) of this vector. */
  @get:JvmName("magnitude")
  val magnitude: Double
    get() = simple.normF()

  /** Returns a normalized (unit) vector in the same direction. */
  fun normalized(): DynamicVector = this * (1.0 / magnitude)

  override fun toString(): String = buildString {
    append("Vector($dimension): [")
    for (i in 0 until dimension) {
      append("%10.4f".format(simple[i, 0]))
      if (i < dimension - 1) append(", ")
    }
    append("]")
  }
}

/** Scalar multiplication from the left. */
operator fun Double.times(vector: DynamicVector): DynamicVector = vector * this

/** Scalar multiplication from the left. */
operator fun Int.times(vector: DynamicVector): DynamicVector = vector * this
