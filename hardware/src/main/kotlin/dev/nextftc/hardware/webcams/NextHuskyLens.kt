/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.webcams

import com.qualcomm.hardware.dfrobot.HuskyLens
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * A NextFTC wrapper around the [HuskyLens] vision sensor that resolves the device
 * lazily from the hardware map, so you never have to fetch it yourself.
 *
 * Wraps the common read and setup methods directly; anything else on the underlying
 * sensor is reachable through [camera].
 *
 * @param initializer supplies the underlying [HuskyLens] when first accessed.
 *
 * @author 28shettr
 *
 */

// May be updated if, there are cooler things to add
class NextHuskyLens(initializer: () -> HuskyLens) {
  constructor(name: String) : this(
    { RobotController.hardwareMap[name] as HuskyLens },
  )

  private val huskyLens by LazyHardware(initializer)

  /** The underlying [HuskyLens], for anything not wrapped here. */
  val camera: HuskyLens get() = huskyLens

  /** Verifies the device is responding over I2C. */
  fun knock(): Boolean = huskyLens.knock()

  /** Selects the recognition algorithm; call once on startup. */
  fun selectAlgorithm(algorithm: HuskyLens.Algorithm) = huskyLens.selectAlgorithm(algorithm)

  /** Returns all currently seen blocks, capped at 6. */
  fun blocks(): Array<HuskyLens.Block> = huskyLens.blocks()

  /** Returns seen blocks with the given id, capped at 6. */
  fun blocks(id: Int): Array<HuskyLens.Block> = huskyLens.blocks(id)

  /** Returns all currently seen arrows, capped at 6. */
  fun arrows(): Array<HuskyLens.Arrow> = huskyLens.arrows()

  /** Returns seen arrows with the given id, capped at 6. */
  fun arrows(id: Int): Array<HuskyLens.Arrow> = huskyLens.arrows(id)
}
