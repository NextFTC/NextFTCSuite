/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.hardware

import android.annotation.SuppressLint
import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.configuration.LynxConstants
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoop
import dev.nextftc.hardware.lynx.NextLynxModule
import dev.nextftc.units.celsius
import dev.nextftc.units.measuretypes.Temperature
import dev.nextftc.units.measuretypes.Voltage
import dev.nextftc.units.volts
import org.firstinspires.ftc.robotcore.external.navigation.TempUnit
import org.firstinspires.ftc.robotcore.external.navigation.VoltageUnit

/**
 * Centralized access to FTC hardware/runtime context and Lynx hub telemetry.
 */
object RobotController : OnCreateEventLoop {
  /**
   * Application context captured when the event loop is created.
   *
   * Used internally when constructing Lynx controller wrappers.
   */
  internal lateinit var appContext: Context

  /**
   * Active FTC event loop backing hardware access.
   *
   * Must be initialized by [onCreateEventLoop] before [hardwareMap] is used.
   */
  @SuppressLint("StaticFieldLeak")
  internal lateinit var eventLoop: FtcEventLoop

  /**
   * The op mode [HardwareMap] from the current event loop.
   */
  @JvmStatic
  @get:JvmName("hardwareMap")
  val hardwareMap: HardwareMap
    get() = eventLoop.opModeManager.hardwareMap

  /**
   * The parent [NextLynxModule], typically the Control Hub.
   *
   * @throws NoSuchElementException when no parent module is available.
   */
  @JvmStatic
  @get:JvmName("controlHub")
  val controlHub: NextLynxModule by lazy {
    NextLynxModule({
      hardwareMap.getAll(LynxModule::class.java).first { it.isParent }
    }, NextLynxModule.Type.CONTROL_HUB)
  }

  /**
   * The first attached Expansion Hub.
   *
   * @throws NoSuchElementException when accessed if an Expansion Hub is not present.
   */
  @JvmStatic
  @get:JvmName("expansionHub")
  val expansionHub: NextLynxModule by lazy {
    NextLynxModule({
      hardwareMap.getAll(LynxModule::class.java).first {
        !it.isParent && it.revProductNumber == LynxConstants.EXPANSION_HUB_PRODUCT_NUMBER
      }
    }, NextLynxModule.Type.EXPANSION_HUB)
  }

  /**
   * All Servo Hubs attached to the robot.
   */
  @JvmStatic
  @get:JvmName("servoHubs")
  val servoHubs: List<NextLynxModule> = object : AbstractList<NextLynxModule>() {
    private val hubs by lazy {
      hardwareMap.getAll(LynxModule::class.java).filter {
        it.revProductNumber == LynxConstants.SERVO_HUB_PRODUCT_NUMBER
      }.map { NextLynxModule({ it }, NextLynxModule.Type.SERVO_HUB) }
    }
    override val size: Int get() = hubs.size
    override fun get(index: Int): NextLynxModule = hubs[index]
  }

  /**
   * Current Control Hub temperature.
   */
  @JvmStatic
  val temperature: Temperature
    get() = controlHub.temperature

  /**
   * Current Control Hub input voltage.
   */
  @JvmStatic
  val inputVoltage: Voltage
    get() = controlHub.inputVoltage

  /**
   * Current Control Hub auxiliary voltage.
   */
  @JvmStatic
  val auxiliaryVoltage: Voltage
    get() = controlHub.auxiliaryVoltage

  /**
   * Blaze has been enabled
   */
  @JvmStatic
  var blazeEnabled: Boolean = false

  /**
   * Lifecycle callback used to initialize runtime context for hardware access.
   *
   * @param context Android context provided by the FTC runtime.
   * @param ftcEventLoop Active FTC event loop.
   */
  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    appContext = context
    eventLoop = ftcEventLoop
  }
}
