/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import dev.frozenmilk.sinister.Scanner
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoop
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoopScanner
import dev.frozenmilk.sinister.sdk.opmodes.SinisterRegisteredOpModes
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.WideSearch
import dev.frozenmilk.sinister.util.log.Logger
import dev.frozenmilk.util.graph.Graph
import dev.frozenmilk.util.graph.rule.AdjacencyRule
import dev.frozenmilk.util.graph.rule.dependsOn
import dev.nextftc.hardware.RobotController
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

/**
 * Scans the user's project for an implementation of [NextRobot] during startup.
 * Maintains a reference to the robot class and instantiation strategy so that
 * [dev.nextftc.robot.opmode.NextFTCOpModeScanner] can properly inject the robot instance into OpModes.
 */
internal object RobotScanner : Scanner {
  /** The class reference of the user's robot. */
  lateinit var robotClass: KClass<*>

  /** The constructor of the robot class */
  lateinit var robotConstructor: () -> NextRobot

  var foundRobot = false
  var foundMultiple = false

  override val loadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> =
    Scanner.INDEPENDENT and dependsOn(OnCreateEventLoopScanner)
  override val unloadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> = Scanner.INDEPENDENT

  override val targets: SearchTarget = WideSearch()

  override fun scan(loader: ClassLoader, cls: Class<*>) {
    val modifiers = cls.modifiers
    if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers)) return

    if (!NextRobot::class.java.isAssignableFrom(cls)) return

    val kcls = cls.kotlin

    if (kcls.hasAnnotation<Disabled>()) {
      Logger.i("NextFTC", "Skipping disabled NextFTC robot class: $kcls")
      return
    }

    Logger.i("NextFTC", "Found NextFTC robot class: $kcls")

    val objectInstance = kcls.objectInstance

    if (objectInstance != null) {
      robotConstructor = { objectInstance as NextRobot }
      robotClass = kcls

      if (foundRobot) {
        foundMultiple = true
      }
      foundRobot = true
      return
    }

    val constructor = kcls.constructors.find { it.parameters.isEmpty() }
    if (constructor != null) {
      robotConstructor = { constructor.call() as NextRobot }
      robotClass = kcls

      if (foundRobot) {
        foundMultiple = true
      }
      foundRobot = true
      return
    }

    Logger.w(
      "NextFTC",
      buildString {
        append("Unable to find appropriate constructor for $cls. ")
        append("Ensure it is either a singleton object or has a public no-argument constructor.")
      },
    )
  }

  override fun afterScan(loader: ClassLoader) {
    if (foundMultiple) {
      Logger.e(
        "NextFTC",
        "Found multiple NextFTC robot classes. Please ensure that there is only one in your project.",
      )
    }

    if (!foundRobot) {
      Logger.e(
        "NextFTC",
        "Unable to find a NextFTC robot class. Please ensure that there is one in your project.",
      )
    } else {
      Logger.i("NextFTC", "Found NextFTC robot class: $robotClass")
    }
  }

  override fun unload(loader: ClassLoader, cls: Class<*>) {}
}

/**
 * Holder for the NextFTC robot class and instance. This is initialized during the [OnCreateEventLoop] phase of the app lifecycle.
 * The robot instance is created using the constructor found by [RobotScanner].
 */
object RobotState : OnCreateEventLoop {
  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    ftcEventLoop.opModeManager.registerListener(DriverStationTelemetry)
  }
}