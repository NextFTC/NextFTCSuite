/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.webcams

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.Limelight3A
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController

/**
 * A NextFTC wrapper around the [Limelight3A] vision sensor that resolves the device
 * lazily from the hardware map, so you never have to fetch it yourself.
 *
 * Wraps the common lifecycle methods directly; anything else on the underlying
 * sensor is reachable through [camera].
 *
 * @param initializer supplies the underlying [Limelight3A] when first accessed.
 */
class NextLimelight(initializer: () -> Limelight3A) {
  constructor(name: String) : this(
    { RobotController.hardwareMap[name] as Limelight3A },
  )

  private val limelight by LazyHardware(initializer)

  /** The underlying [Limelight3A], for anything not wrapped. */
  val camera: Limelight3A get() = limelight

  /** The most recent result from the polling loop. */
  val latestResult: LLResult get() = limelight.latestResult

  /** True while the polling loop is active. */
  val isRunning: Boolean get() = limelight.isRunning

  /** True if the sensor has reported data within the last ~250ms. */
  val isConnected: Boolean get() = limelight.isConnected

  /** Starts the polling loop. */
  fun start() = limelight.start()

  /** Stops the polling loop. */
  fun stop() = limelight.stop()

  /** Switches to the pipeline at the given index. */
  fun setPipeline(pipeline: Int) = limelight.pipelineSwitch(pipeline)

  /** Sets the poll rate in Hz; must be called before [start]. */
  fun setPollRate(hz: Int) = limelight.setPollRateHz(hz)

  /** Sets the poll rate and pipeline, then starts polling in one call. */
  @JvmOverloads
  fun startReading(pipeline: Int, hz: Int = 100) {
    setPollRate(hz)
    setPipeline(pipeline)
    start()
  }
}
