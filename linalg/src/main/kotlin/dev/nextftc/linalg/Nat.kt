/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

@file:JvmName("Naturals")

package dev.nextftc.linalg

/**
 * Type-level natural numbers for compile-time matrix dimension checking.
 * Use these as generic bounds on [Matrix] to ensure dimensional correctness at compile time.
 */
sealed interface Nat {
  val num: Int
}

/** Type-level representation of 1 */
data object N1 : Nat {
  override val num = 1
}

/** Type-level representation of 2 */
data object N2 : Nat {
  override val num = 2
}

/** Type-level representation of 3 */
data object N3 : Nat {
  override val num = 3
}

/** Type-level representation of 4 */
data object N4 : Nat {
  override val num = 4
}

/** Type-level representation of 5 */
data object N5 : Nat {
  override val num = 5
}

/** Type-level representation of 6 */
data object N6 : Nat {
  override val num = 6
}

/** Type-level representation of 7 */
data object N7 : Nat {
  override val num = 7
}

/** Type-level representation of 8 */
data object N8 : Nat {
  override val num = 8
}

/** Type-level representation of 9 */
data object N9 : Nat {
  override val num = 9
}

/** Type-level representation of 10 */
data object N10 : Nat {
  override val num = 10
}

/** Type-level representation of an unknown/dynamic dimension */
data class NDynamic(override val num: Int) : Nat

/**
 * Creates a [Nat] from a number.
 */
fun natOf(num: Int) = when (num) {
  1 -> N1
  2 -> N2
  3 -> N3
  4 -> N4
  5 -> N5
  6 -> N6
  7 -> N7
  8 -> N8
  9 -> N9
  10 -> N10
  else -> NDynamic(num)
}
