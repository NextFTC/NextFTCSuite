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
import dev.frozenmilk.sinister.loaders.SlothClassLoader
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoop
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoopScanner
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.TeamCodeSearch
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
  // every robot candidate found in the teamcode loader
  private val candidates = mutableListOf<RobotCandidate>()

  private var reportedError = false

  override val loadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> =
    Scanner.INDEPENDENT and dependsOn(OnCreateEventLoopScanner)
  override val unloadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> = Scanner.INDEPENDENT

  override val targets: SearchTarget = TeamCodeSearch()

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
    candidates.add(RobotCandidate(cls.asSubclass(NextRobot::class.java).kotlin, constructorFor(kcls)))
  }

  /**
   * Returns a factory for [kcls], or null, and adds a warning to the log if no usable constructor was found.
   */
  private fun constructorFor(kcls: KClass<*>): (() -> NextRobot)? {
    val objectInstance = kcls.objectInstance
    if (objectInstance != null) return { objectInstance as NextRobot }

    val constructor = kcls.constructors.find {
      it.parameters.isEmpty() && it.visibility == KVisibility.PUBLIC
    }
    if (constructor != null) return { constructor.call() as NextRobot }

    RobotLog.Global.addWarning(
      "Unable to find appropriate constructor for $kcls. " +
        "Ensure it is either a singleton object or has a public no-argument constructor.",
    )
    return null
  }

  override fun afterScan(loader: ClassLoader) {
    // required because Sloth constructs a new one every load
    // unfortunately this only works bc Sloth only uses SlothClassLoader for teamcode
    // so if that changes it might break
    if (loader !is SlothClassLoader) {
      return
    }

    clearReportedError()
    RobotState.robotClass = null
    RobotState.robotInstance = null

    if (candidates.isEmpty()) {
      reportError("No NextFTC robot class found. Ensure you have a class that implements NextRobot.")
      return
    }

    if (candidates.size > 1) {
      RobotLog.Global.addWarning(
        "Multiple NextFTC robot classes found: ${candidates.map { it.kClass }}. " +
          "Ensure you have only one class that implements NextRobot.",
      )
      return
    }

    val (kClass, constructor) = candidates.single()

    if (constructor == null) {
      reportError(
        "No usable constructor was found for $kClass, so it could not be created. " +
          "Ensure it is either a singleton object or has a public no-argument constructor.",
      )
      return
    }

    val instance = try {
      constructor()
    } catch (throwable: Throwable) {
      reportError(
        "Failed to create an instance of $kClass. " +
          "Check its constructor and property initializers for code that throws.",
        throwable,
      )
      return
    }

    RobotLog.info("Using NextFTC robot class: $kClass")
    RobotState.robotClass = kClass
    RobotState.robotInstance = instance
  }

  override fun beforeUnload(loader: ClassLoader) {
    candidates.clear()
    RobotState.robotClass = null
    RobotState.robotInstance = null
  }

  override fun unload(loader: ClassLoader, cls: Class<*>) {}

  private fun reportError(message: String, throwable: Throwable? = null) {
    reportedError = true
    if (throwable == null) {
      RobotLog.Global.setError(message)
    } else {
      RobotLog.Global.setError(message, throwable)
    }
  }

  /** Clears the global error only if this scanner is the one that set it. */
  private fun clearReportedError() {
    if (!reportedError) return
    reportedError = false
    RobotLog.Global.clearError()
  }

  private data class RobotCandidate(val kClass: KClass<out NextRobot>, val constructor: (() -> NextRobot)?)
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
