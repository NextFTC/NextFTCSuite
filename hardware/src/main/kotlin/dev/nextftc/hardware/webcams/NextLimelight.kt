/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware.webcams

import com.qualcomm.hardware.limelightvision.LLResult
import com.qualcomm.hardware.limelightvision.LLResultTypes
import com.qualcomm.hardware.limelightvision.Limelight3A
import dev.nextftc.control.geometry.Pose2d
import dev.nextftc.hardware.LazyHardware
import dev.nextftc.hardware.RobotController
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A NextFTC wrapper around the [Limelight3A] vision sensor that resolves the device
 * lazily from the hardware map, so you never have to fetch it yourself.
 *
 * Wraps the common lifecycle methods directly; anything else on the underlying
 * sensor is reachable through [camera].
 *
 * @param initializer supplies the underlying [Limelight3A] when first accessed.
 *
 *  * @author 28shettr
 *
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

  /** The horizontal offset angle, in degrees, from the camera's crosshair to the target. */
  val tX: Double get() = latestResult.tx

  /** The vertical offset angle, in degrees, from the camera's crosshair to the target. */
  val tY: Double get() = latestResult.ty

  /** Starts the polling loop. */
  fun start() = limelight.start()

  /** Stops the polling loop. */
  fun stop() = limelight.stop()

  /** Switches to the pipeline at the given index. */
  fun setPipeline(pipeline: Int) = limelight.pipelineSwitch(pipeline)

  /** Sets the poll rate in Hz; must be called before [start]. */
  fun setPollRate(hz: Int) = limelight.setPollRateHz(hz)
  private var distanceMeters: Double = 0.0
  /** Returns the straight-line distance (hypotenuse) from the robot to the AprilTag matching [id] (or any visible tag if [id] is null) in the given [unit]. */
  @JvmOverloads
  fun getDistance(unit: DistanceUnit = DistanceUnit.INCH, id: Int? = null): Double {
    val tags: List<LLResultTypes.FiducialResult> = latestResult.fiducialResults
    for (tag in tags) {
      if (id == null || tag.fiducialId == id) {
        val pose: Pose3D = tag.robotPoseTargetSpace
          distanceMeters = sqrt(
          pose.position.x.pow(2.0) +
            pose.position.y.pow(2.0) +
            pose.position.z.pow(2.0),
        )
      }
    }
    return unit.fromUnit(DistanceUnit.METER, distanceMeters)
  }

  /** Returns the robot's field position from the latest Limelight result in Pedro coordinates, or `null` if no valid pose is available. */
  fun getPedroPoseFromLimelight(): Pose2d? {
    val result = limelight.getLatestResult()
    if (result == null || !result.isValid()) return null

    val botpose = result.botpose ?: return null

    val rawX = botpose.getPosition().x
    val rawY = botpose.getPosition().y
    val inchX = rawY / DistanceUnit.mPerInch
    val inchY = -(rawX) / DistanceUnit.mPerInch
    val heading = botpose.getOrientation().getYaw(AngleUnit.DEGREES) - 90

    val pedroX = inchX + 72
    val pedroY = inchY + 72
    val pedroPose = Pose2d(pedroX, pedroY, Math.toRadians(heading))

    return pedroPose
  }

  /** Sets the poll rate and pipeline, then starts polling in one call. */
  @JvmOverloads
  fun startReading(pipeline: Int, hz: Int = 100) {
    setPollRate(hz)
    setPipeline(pipeline)
    start()
  }
}
