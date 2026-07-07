/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

/**
 * The root container for your robot's hardware and mechanisms.
 *
 * Implementing this interface allows the NextFTC scanning architecture to automatically
 * detect and inject your robot instance into OpModes.
 */
interface NextRobot {
  /**
   * Called periodically during the OpMode loop, before individual mechanisms are updated.
   * Useful for global robot updates, clearing caches, or logging.
   */
  fun periodic() {}

  /**
   * A collection of all mechanisms that belong to this robot.
   * These mechanisms will have their [Mechanism.periodic] methods called automatically.
   */
  val mechanisms: Set<Mechanism>
    get() = emptySet()
}
