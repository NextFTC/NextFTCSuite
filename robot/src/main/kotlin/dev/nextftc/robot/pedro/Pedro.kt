/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.pedro

import com.pedropathing.follower.Follower
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.nextftc.hardware.RobotController
import java.util.function.Function

/**
 * Global access point for the active Pedro Pathing [Follower].
 *
 * Call [configure] once (e.g. in your robot class's constructor) with a reference to your
 * `Constants.create` method. The follower is built lazily on first access and automatically
 * updated and cleaned up if you register [PedroHook] on your OpMode — see that class for why
 * it's required for the follower to actually do anything.
 */
object Pedro {
  private var factory: Function<HardwareMap, Follower>? = null
  internal var follower: Follower? = null

  /** Registers the function used to build the [Follower]. Typically `Constants::create`. */
  @JvmStatic
  fun configure(followerFactory: Function<HardwareMap, Follower>) {
    factory = followerFactory
  }

  /** Returns the active [Follower], building it on first access. */
  @JvmStatic
  fun follower(): Follower {
    follower?.let { return it }
    val f = factory
      ?: error(
        "Call Pedro.configure(Constants::create) once, e.g. in your robot class's constructor.",
      )
    return f.apply(RobotController.hardwareMap).also { follower = it }
  }
}
