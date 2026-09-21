/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.pedro

import com.pedropathing.config.ConfigVar
import com.pedropathing.config.Configuration
import com.pedropathing.config.Validator.nonnegative
import com.qualcomm.robotcore.hardware.DcMotorSimple
import dev.nextftc.hardware.lynx.NextLynxModule

/**
 * Drop-in replacement for Pedro's own [com.pedropathing.revhub.drivetrains.MecanumConfig],
 * configured with a Lynx module + port per corner instead of a hardware map name. Each
 * corner's module can differ, so motors split across Control Hub / Expansion Hub work
 * without any special handling.
 */
class NextMecanumConfig(config: Configuration<NextMecanumConfig>) {
  val frontLeftModule: ConfigVar<NextLynxModule> = ConfigVar.required()
  val frontLeftPort: ConfigVar<Int> = ConfigVar.required()
  val frontRightModule: ConfigVar<NextLynxModule> = ConfigVar.required()
  val frontRightPort: ConfigVar<Int> = ConfigVar.required()
  val backLeftModule: ConfigVar<NextLynxModule> = ConfigVar.required()
  val backLeftPort: ConfigVar<Int> = ConfigVar.required()
  val backRightModule: ConfigVar<NextLynxModule> = ConfigVar.required()
  val backRightPort: ConfigVar<Int> = ConfigVar.required()

  val frontLeftDirection: ConfigVar<DcMotorSimple.Direction> = ConfigVar.required()
  val frontRightDirection: ConfigVar<DcMotorSimple.Direction> = ConfigVar.required()
  val backLeftDirection: ConfigVar<DcMotorSimple.Direction> = ConfigVar.required()
  val backRightDirection: ConfigVar<DcMotorSimple.Direction> = ConfigVar.required()

  /** Whether ZeroPowerBrake mode is enabled in manual mode. */
  val manualBrakeMode: ConfigVar<Boolean> = ConfigVar.of(true)

  /** Smallest power change that triggers a hardware write. */
  val powerThreshold: ConfigVar<Double> = ConfigVar.of(0.01, nonnegative())

  init {
    config.configure(this)
  }
}
