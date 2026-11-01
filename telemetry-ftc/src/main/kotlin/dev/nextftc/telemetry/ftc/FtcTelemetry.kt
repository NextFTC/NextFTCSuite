/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.ftc

import android.annotation.SuppressLint
import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier
import com.qualcomm.robotcore.util.RobotLog
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoop
import dev.frozenmilk.sinister.sdk.apphooks.OnDestroy
import dev.nextftc.telemetry.Telemetry
import dev.nextftc.telemetry.TelemetryWarningHandler
import dev.nextftc.telemetry.nt.NT4Backend
import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.wpilog.WPILogBackend
import dev.nextftc.telemetry.wpilog.WPILogWriter
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Settings for [FtcTelemetry]. Change them before an OpMode is initialized; they are read each time
 * one starts.
 */
class FtcTelemetryConfig {
  /** Whether to write a WPILOG for each OpMode. */
  @JvmField @Volatile
  var logToFile: Boolean = true

  /** Where logs are written. */
  @JvmField @Volatile
  var logDirectory: File = File(AppUtil.FIRST_FOLDER, "nextftc/logs")

  /** Whether to serve NetworkTables 4 for dashboards such as AdvantageScope. */
  @JvmField @Volatile
  var liveServer: Boolean = true

  /** The port the NetworkTables 4 server listens on. Read when the Robot Controller app starts. */
  @JvmField @Volatile
  var serverPort: Int = NT4Server.DEFAULT_PORT
}

/**
 * Connects [Telemetry] to the FTC OpMode lifecycle, with no other part of NextFTC involved.
 *
 * Sloth finds this object and calls [onCreateEventLoop] when the Robot Controller creates its event
 * loop. From then on every OpMode gets a [WPILogBackend] (if [FtcTelemetryConfig.logToFile]) and a
 * [NT4Backend] registered on [Telemetry.registry] before its `init`, and closed after it stops.
 * The server outlives the OpModes, so a dashboard stays connected between them, and is stopped when
 * the Robot Controller app is destroyed.
 */
object FtcTelemetry : OnCreateEventLoop, OpModeManagerNotifier.Notifications {
  private const val TAG = "NextFTC-Telemetry"

  /** The settings used for each OpMode. */
  @JvmStatic val config: FtcTelemetryConfig = FtcTelemetryConfig()

  @SuppressLint("StaticFieldLeak")
  private var eventLoop: FtcEventLoop? = null
  private var server: NT4Server? = null
  private var log: WPILogBackend? = null
  private var live: NT4Backend? = null

  @Synchronized
  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    eventLoop = ftcEventLoop
    Telemetry.registry.warningHandler = TelemetryWarningHandler { RobotLog.ww(TAG, it) }
    ftcEventLoop.opModeManager.registerListener(this)
  }

  @Synchronized
  override fun onOpModePreInit(opMode: OpMode?) {
    closeBackends()
    val registry = Telemetry.registry
    val name = eventLoop?.opModeManager?.activeOpModeName ?: opMode?.javaClass?.simpleName ?: "OpMode"
    if (name == OpModeManagerImpl.DEFAULT_OP_MODE_NAME) return

    if (config.logToFile) {
      try {
        config.logDirectory.mkdirs()
        val stamp = SimpleDateFormat("yyyy_MM_dd__HH_mm_ss", Locale.US).format(Date())
        val file =
          File(config.logDirectory, "${stamp}__${name.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.wpilog")
        val writer = WPILogWriter(
          FileOutputStream(file),
          extraHeader = """{"source":"NextFTC","opMode":"${name.replace("\"", "'")}"}""",
          timeSource = registry.timeSource,
          origin = registry.origin,
        )
        WPILogBackend(writer).also {
          log = it
          registry.register(it)
        }
      } catch (e: Exception) {
        RobotLog.ee(TAG, e, "Couldn't open a telemetry log")
      }
    }

    if (config.liveServer) {
      try {
        val server = server ?: NT4Server(
          config.serverPort,
          timeSource = registry.timeSource,
          origin = registry.origin,
        ).also {
          it.start()
          server = it
        }
        NT4Backend(server).also {
          live = it
          registry.register(it)
        }
      } catch (e: Exception) {
        RobotLog.ee(TAG, e, "Couldn't start the telemetry server")
      }
    }
  }

  @Synchronized
  internal fun shutdown() {
    closeBackends()
    eventLoop = null
    try {
      server?.stop()
    } catch (e: Exception) {
      RobotLog.ee(TAG, e, "Couldn't stop the telemetry server")
    }
    server = null
  }

  override fun onOpModePreStart(opMode: OpMode?) {}

  @Synchronized
  override fun onOpModePostStop(opMode: OpMode?) {
    closeBackends()
  }

  private fun closeBackends() {
    val registry = Telemetry.registry
    for (backend in listOfNotNull(log, live)) {
      registry.unregister(backend)
      try {
        backend.close()
      } catch (e: Exception) {
        RobotLog.ee(TAG, e, "Couldn't close a telemetry backend")
      }
    }
    log = null
    live = null
  }
}

/**
 * Stops [FtcTelemetry]'s server when the Robot Controller app is destroyed. It's a separate object
 * because [OnCreateEventLoop] and [OnDestroy] declare incompatible `adjacencyRule` properties.
 */
internal object FtcTelemetryShutdown : OnDestroy {
  override fun onDestroy(context: Context) = FtcTelemetry.shutdown()
}
