/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("SizedVectors")

package dev.nextftc.linalg

import org.ejml.simple.SimpleMatrix

/**
 * A dimensionally type-safe column vector of doubles.
 *
 * The dimension [N] is encoded at the type level using [Nat] types,
 * allowing the compiler to catch dimension mismatches at compile time.
 *
 * Example:
 * ```kotlin
 * val a: SizedVector<N3> = SizedVector.of(N3, 1.0, 2.0, 3.0)
 * val b: SizedVector<N3> = SizedVector.of(N3, 4.0, 5.0, 6.0)
 * val c: SizedVector<N3> = a + b  // Compiles!
 * val dot: Double = a dot b
 * // val d: SizedVector<N2> = a + SizedVector.zero(N2)  // Won't compile - dimensions don't match
 * ```
 *
 * @param N The dimension type
 */
class Vector<N : Nat> internal constructor(simple: SimpleMatrix, internal val dimNat: N) :
  Matrix<N, N1>(simple, dimNat, N1) {
  init {
    require(simple.numCols() == 1) { "Vector must have exactly one column" }
  }

  constructor(matrix: Matrix<N, N1>) : this(matrix.simple, matrix.rowNat)

  @Suppress("ktlint")
    companion object {
        /**
         * Creates a zero vector with dimension specified by the [Nat] type parameter.
         */
        @JvmStatic
        fun <N : Nat> zero(dim: N): Vector<N> = Vector(SimpleMatrix(dim.num, 1), dim)

        /**
         * Creates a vector from the given values.
         */
        @JvmStatic
        fun <N : Nat> of(dim: N, vararg data: Double): Vector<N> {
            require(data.size == dim.num) { "Data size must match dimension" }
            return Vector(SimpleMatrix(data.size, 1, false, data), dim)
        }

        /**
         * Creates a vector from a collection.
         */
        @JvmStatic
        fun <N : Nat> of(dim: N, data: Collection<Double>): Vector<N> =
            of(dim, *data.toDoubleArray())
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
  override fun copy(): Vector<N> = Vector(simple.copy(), dimNat)

  /** Negates all elements of this vector. */
  override operator fun unaryMinus(): Vector<N> = Vector(simple.negative(), dimNat)

  /** Adds another vector with the same dimension. */
  operator fun plus(other: Vector<N>): Vector<N> = Vector(simple + other.simple, dimNat)

  /** Subtracts another vector with the same dimension. */
  operator fun minus(other: Vector<N>): Vector<N> = Vector(simple - other.simple, dimNat)

  /** Multiplies this vector by a scalar. */
  override operator fun times(scalar: Double): Vector<N> = Vector(simple.scale(scalar), dimNat)

  /** Multiplies this vector by a scalar. */
  override operator fun times(scalar: Number): Vector<N> = times(scalar.toDouble())

  /** Computes the dot product of this vector with another vector of the same dimension. */
  infix fun dot(other: Vector<N>): Double = simple.transpose().mult(other.simple)[0, 0]

  /** Returns the Euclidean norm (magnitude) of this vector. */
  @get:JvmName("magnitude")
  val magnitude: Double
    get() = simple.normF()

  /** Returns a normalized (unit) vector in the same direction. */
  fun normalized(): Vector<N> = this * (1.0 / magnitude)

  /** Converts to a [DynamicVector]. */
  fun toDynamicVector(): DynamicVector = DynamicVector(simple)

  override fun toString(): String = buildString {
    append("SizedVector<$dimension>: [")
    for (i in 0 until dimension) {
      append("%10.4f".format(simple[i, 0]))
      if (i < dimension - 1) append(", ")
    }
    append("]")
  }
}

/** Scalar multiplication from the left. */
operator fun <N : Nat> Double.times(vector: Vector<N>): Vector<N> = vector * this

/** Scalar multiplication from the left. */
operator fun <N : Nat> Int.times(vector: Vector<N>): Vector<N> = vector * this
