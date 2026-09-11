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
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.WideSearch
import dev.frozenmilk.util.graph.Graph
import dev.frozenmilk.util.graph.rule.AdjacencyRule
import dev.frozenmilk.util.graph.rule.dependsOn
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation

/**
 * Scans the user's project for an implementation of [NextRobot] during startup.
 * Maintains a reference to the robot class and instantiation strategy so that
 * [dev.nextftc.robot.opmode.NextFTCOpModeScanner] can properly inject the robot instance into OpModes.
 */
internal object RobotScanner : Scanner {
  internal var allRobotClasses: MutableList<KClass<out NextRobot>> = mutableListOf()
  private var robotConstructor: (() -> NextRobot)? = null
  private var robotLoader: ClassLoader? = null

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
      RobotLog.info("Skipping disabled NextFTC robot class: $kcls")
      return
    }

    RobotLog.info("Found NextFTC robot class: $kcls")
    allRobotClasses.add(cls.asSubclass(NextRobot::class.java).kotlin)
    // Recorded for every candidate, not just usable ones, so that [beforeUnload] always clears the
    // state this scan added.
    robotLoader = loader

    val objectInstance = kcls.objectInstance

    if (objectInstance != null) {
      robotConstructor = { objectInstance as NextRobot }
      return
    }

    val constructor = kcls.constructors.find {
      it.parameters.isEmpty() && it.visibility == KVisibility.PUBLIC
    }
    if (constructor != null) {
      robotConstructor = { constructor.call() as NextRobot }
      return
    }

    val message = "Unable to find appropriate constructor for $cls. " +
      "Ensure it is either a singleton object or has a public no-argument constructor."
    RobotLog.Global.addWarning(message)
  }

  override fun afterScan(loader: ClassLoader) {
    if (allRobotClasses.isEmpty()) {
      RobotLog.Global.setError(
        "No NextFTC robot class found. Ensure you have a class that implements NextRobot.",
      )
      return
    }

    if (allRobotClasses.size > 1) {
      RobotLog.Global.addWarning(
        "Multiple NextFTC robot classes found: $allRobotClasses. " +
          "Ensure you have only one class that implements NextRobot.",
      )
      return
    }

    val robotClass = allRobotClasses[0]
    val constructor = robotConstructor

    if (constructor == null) {
      RobotLog.Global.setError(
        "No usable constructor was found for $robotClass, so it could not be created. " +
          "Ensure it is either a singleton object or has a public no-argument constructor.",
      )
      return
    }

    RobotLog.info("Using NextFTC robot class: $robotClass")
    RobotState.robotClass = robotClass

    RobotState.robotInstance = try {
      constructor()
    } catch (throwable: Throwable) {
      RobotLog.Global.setError(
        "Failed to create an instance of $robotClass. " +
          "Check its constructor and property initializers for code that throws.",
        throwable,
      )
      RobotState.robotClass = null
      return
    }
  }

  override fun beforeUnload(loader: ClassLoader) {
    if (loader == robotLoader) {
      robotConstructor = null
      robotLoader = null
      allRobotClasses.clear()
      RobotState.robotClass = null
      RobotState.robotInstance = null
    }
  }

  override fun unload(loader: ClassLoader, cls: Class<*>) {}
}

/**
 * Holder for the NextFTC robot class and instance. This is initialized during the [OnCreateEventLoop] phase of the app lifecycle.
 * The robot instance is created using the constructor found by [RobotScanner].
 */
object RobotState : OnCreateEventLoop {
  internal var robotInstance: NextRobot? = null
  internal var robotClass: KClass<out NextRobot>? = null
  val robot: NextRobot
    get() = robotInstance ?: RobotLog.Global.setErrorAndThrow(
      "Robot instance not initialized",
      RobotScanException("Robot instance not initialized"),
    )

  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    ftcEventLoop.opModeManager.registerListener(DriverStationTelemetry)
  }
}
