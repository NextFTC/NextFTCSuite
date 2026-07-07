/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("VectorBuilder")
@file:Suppress("ktlint:standard:parameter-list-wrapping")

package dev.nextftc.linalg

import org.ejml.simple.SimpleMatrix

/** Creates a 1-dimensional vector. */
fun vectorOf(x: Double): Vector<N1> = Vector(SimpleMatrix(1, 1, false, doubleArrayOf(x)), N1)

/** Creates a 2-dimensional vector. */
fun vectorOf(x: Double, y: Double): Vector<N2> = Vector(SimpleMatrix(2, 1, false, doubleArrayOf(x, y)), N2)

/** Creates a 3-dimensional vector. */
fun vectorOf(x: Double, y: Double, z: Double): Vector<N3> =
  Vector(SimpleMatrix(3, 1, false, doubleArrayOf(x, y, z)), N3)

/** Creates a 4-dimensional vector. */
fun vectorOf(x1: Double, x2: Double, x3: Double, x4: Double): Vector<N4> =
  Vector(SimpleMatrix(4, 1, false, doubleArrayOf(x1, x2, x3, x4)), N4)

/** Creates a 5-dimensional vector. */
fun vectorOf(x1: Double, x2: Double, x3: Double, x4: Double, x5: Double): Vector<N5> =
  Vector(SimpleMatrix(5, 1, false, doubleArrayOf(x1, x2, x3, x4, x5)), N5)

/** Creates a 6-dimensional vector. */
fun vectorOf(x1: Double, x2: Double, x3: Double, x4: Double, x5: Double, x6: Double): Vector<N6> =
  Vector(SimpleMatrix(6, 1, false, doubleArrayOf(x1, x2, x3, x4, x5, x6)), N6)

/** Creates a 7-dimensional vector. */
fun vectorOf(
  x1: Double,
  x2: Double,
  x3: Double,
  x4: Double,
  x5: Double,
  x6: Double,
  x7: Double,
): Vector<N7> = Vector(SimpleMatrix(7, 1, false, doubleArrayOf(x1, x2, x3, x4, x5, x6, x7)), N7)

/** Creates an 8-dimensional vector. */
fun vectorOf(
  x1: Double,
  x2: Double,
  x3: Double,
  x4: Double,
  x5: Double,
  x6: Double,
  x7: Double,
  x8: Double,
): Vector<N8> = Vector(SimpleMatrix(8, 1, false, doubleArrayOf(x1, x2, x3, x4, x5, x6, x7, x8)), N8)

/** Creates a 9-dimensional vector. */
fun vectorOf(
  x1: Double,
  x2: Double,
  x3: Double,
  x4: Double,
  x5: Double,
  x6: Double,
  x7: Double,
  x8: Double,
  x9: Double,
): Vector<N9> = Vector(SimpleMatrix(9, 1, false, doubleArrayOf(x1, x2, x3, x4, x5, x6, x7, x8, x9)), N9)

/** Creates a 10-dimensional vector. */
fun vectorOf(
  x1: Double,
  x2: Double,
  x3: Double,
  x4: Double,
  x5: Double,
  x6: Double,
  x7: Double,
  x8: Double,
  x9: Double,
  x10: Double,
): Vector<N10> = Vector(
  SimpleMatrix(10, 1, false, doubleArrayOf(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10)),
  N10,
)
