/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.RobotLog
import dev.frozenmilk.sinister.Scanner
import dev.frozenmilk.sinister.sdk.opmodes.OpModeScanner
import dev.frozenmilk.sinister.sdk.opmodes.SinisterRegisteredOpModes
import dev.frozenmilk.sinister.sdk.opmodes.TeleopAutonomousOpModeScanner
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.WideSearch
import dev.frozenmilk.sinister.util.log.Logger
import dev.frozenmilk.util.graph.Graph
import dev.frozenmilk.util.graph.rule.AdjacencyRule
import dev.frozenmilk.util.graph.rule.dependsOn
import java.lang.reflect.Modifier

/**
 * Scans the user's project for an implementation of [NextRobot] during startup.
 * Maintains a reference to the robot class and instantiation strategy so that
 * [RobotOpModeScanner] can properly inject the robot instance into OpModes.
 */
object RobotScanner : Scanner {
  /** The singleton or freshly constructed instance of the user's robot. */
  lateinit var robot: NextRobot

  /** The class reference of the user's robot. */
  lateinit var robotClass: Class<*>

  override val loadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> = Scanner.INDEPENDENT
  override val unloadAdjacencyRule: AdjacencyRule<Scanner, Graph<Scanner>> = Scanner.INDEPENDENT

  override val targets: SearchTarget = WideSearch()

  override fun scan(loader: ClassLoader, cls: Class<*>) {
    if (NextRobot::class.java.isAssignableFrom(cls) && !cls.isInterface &&
      !Modifier.isAbstract(cls.modifiers)
    ) {
      robotClass = cls
      val instanceField = try {
        cls.getDeclaredField("INSTANCE")
      } catch (e: NoSuchFieldException) {
        null
      }

      robot = if (instanceField != null) {
        instanceField.isAccessible = true
        instanceField.get(null) as NextRobot
      } else {
        cls.getDeclaredConstructor().newInstance() as NextRobot
      }
    }
  }

  override fun unload(loader: ClassLoader, cls: Class<*>) {}
}

/**
 * Scans the user's project for OpModes that take a [NextRobot] instance in their constructor.
 *
 * Automatically handles registering these OpModes with the FTC dashboard while intercepting
 * their instantiation to inject the [RobotScanner.robot] instance.
 */
object RobotOpModeScanner : OpModeScanner() {
  override val loadAdjacencyRule = super.loadAdjacencyRule and dependsOn(
    RobotScanner,
  ) and dependsOn(TeleopAutonomousOpModeScanner)
  override val unloadAdjacencyRule = super.unloadAdjacencyRule and dependsOn(RobotScanner)

  override val targets: SearchTarget = WideSearch()

  override fun scan(loader: ClassLoader, cls: Class<*>, registrationHelper: RegistrationHelper) {
    if (OpMode::class.java.isAssignableFrom(cls) && !Modifier.isAbstract(cls.modifiers)) {
      val (meta, error) = TeleopAutonomousOpModeScanner.metaForClass(cls)
      if (error != null) {
        Logger.e("NextFTC", "Error scanning class ${cls.name}: $error")
        RobotLog.setGlobalErrorMsg(error)
        return
      }
      if (meta == null) return

      val robotConstructor = cls.constructors.find {
        it.parameterTypes.size == 1 && it.parameterTypes[0].isAssignableFrom(RobotScanner.robotClass)
      }

      if (robotConstructor != null) {
        registrationHelper.unregister(meta)
        registrationHelper.register(meta) { robotConstructor.newInstance(RobotScanner.robot) as OpMode }
      }
    }
  }
}
