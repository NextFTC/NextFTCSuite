/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.v2.pedro

import com.pedropathing.follower.Follower
import com.qualcomm.robotcore.hardware.HardwareMap
import dev.nextftc.hardware.RobotController
import dev.nextftc.hardware.util.LazyHardware
import dev.nextftc.robot.Mechanism
import java.util.function.Function

/**
 * Owns a Pedro Pathing [Follower] and keeps it updated every loop.
 *
 * Add it to your robot's `mechanisms` and pass your `Constants.createFollower` method. The
 * follower is built on first access, so it is safe to construct this in your robot's constructor.
 *
 * As with other NextFTC hardware, the follower lives as long as the robot instance does, so set
 * its starting pose at the beginning of each OpMode.
 */
class PedroMechanism(followerFactory: (HardwareMap) -> Follower) : Mechanism {
  /** The [Follower], built on first access. */
  @get:JvmName("follower")
  val follower: Follower by LazyHardware { followerFactory(RobotController.hardwareMap) }

  override fun periodic() {
    follower.update()
  }
}
