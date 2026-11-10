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
import android.content.SharedPreferences
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.firstinspires.ftc.robotcore.internal.system.AppUtil
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Settings for [FtcTelemetry]. Change them before an OpMode is initialized; they are read each time
 * one starts.
 */
class FtcTelemetryConfig {
  /** Whether to write a WPILOG for each OpMode. */
  @JvmField @Volatile
  var logToFile: Boolean = true

  @Volatile
  private var customLogDirectory: File? = null

  private val defaultLogDirectory: File by lazy {
    try {
      File(AppUtil.FIRST_FOLDER, "nextftc/logs")
    } catch (_: Throwable) {
      File("nextftc/logs")
    }
  }

  /** Where logs are written. */
  var logDirectory: File
    get() = customLogDirectory ?: defaultLogDirectory
    set(value) {
      customLogDirectory = value
    }

  /**
   * How often, in milliseconds, [FtcTelemetry.update] flushes the log to disk, so a crash or power
   * loss only costs the last interval. Zero or less turns it off, and the log is only flushed when
   * `flush()  is called, its buffer fills or the OpMode stops.
   */
  @JvmField @Volatile
  var logFlushIntervalMillis: Long = 250

  /** Whether to serve NetworkTables 4 for dashboards such as AdvantageScope. */
  @JvmField @Volatile
  var liveServer: Boolean = true

  /** The port the NetworkTables 4 server listens on. Read when the Robot Controller app starts. */
  @JvmField @Volatile
  var serverPort: Int = NT4Server.DEFAULT_PORT

  /** Whether to show logged values on the Driver Station with a [DriverStationBackend]. */
  @JvmField @Volatile
  var driverStation: Boolean = true

  /**
   * If not empty, the Driver Station only shows values on these paths or under them, such as
   * `/Drive`. The log and the server always get everything.
   */
  @JvmField @Volatile
  var driverStationPrefixes: List<String> = emptyList()
}

/**
 * Connects [Telemetry] to the FTC OpMode lifecycle, with no other part of NextFTC involved.
 *
 * Sloth finds this object and calls [onCreateEventLoop] when the Robot Controller creates its event
 * loop. From then on every OpMode gets a [WPILogBackend] (if [FtcTelemetryConfig.logToFile]), a
 * [NT4Backend] (if [FtcTelemetryConfig.liveServer]) and a [DriverStationBackend] (if
 * [FtcTelemetryConfig.driverStation]) registered on [Telemetry.registry] before its `init`, and
 * closed after it stops. The server also needs [isServerEnabled], which the "Enable/Disable NextFTC
 * Telemetry Server" OpModes switch, so it can be turned off for competition.
 * The server outlives the OpModes, so a dashboard stays connected between them, and is stopped when
 * the Robot Controller app is destroyed.
 */
object FtcTelemetry : OnCreateEventLoop, OpModeManagerNotifier.Notifications {
  private const val TAG = "NextFTC-Telemetry"
  private const val PREFS_NAME = "nextftc_telemetry"
  private const val KEY_SERVER_ENABLED = "server_enabled"

  /** The settings used for each OpMode. */
  @JvmStatic val config: FtcTelemetryConfig = FtcTelemetryConfig()

  @SuppressLint("StaticFieldLeak")
  private var eventLoop: FtcEventLoop? = null
  private var server: NT4Server? = null
  private var log: WPILogBackend? = null

  /** The file [log] writes to, which the download page won't serve until it is closed. */
  @Volatile
  private var activeLogFile: File? = null

  /** Serves `/download`, `/logs` and `/logs/<name>` on the server's port. */
  private val logDownloads =
    LogDownloads(directory = { config.logDirectory }, activeFile = { activeLogFile })
  private var live: NT4Backend? = null
  private var driverStation: DriverStationBackend? = null
  private var prefs: SharedPreferences? = null
  private var lastFlush: TimeMark = TimeSource.Monotonic.markNow()

  /**
   * Whether the NetworkTables server may run, saved on the Robot Controller so it holds across app
   * restarts. The server runs when this and [FtcTelemetryConfig.liveServer] are both true. Change it
   * with [setServerEnabled], or the "Enable/Disable NextFTC Telemetry Server" OpModes.
   */
  @JvmStatic
  @Volatile
  var isServerEnabled: Boolean = true
    private set

  /**
   * Turns the NetworkTables server on or off, and saves the choice so it holds across app restarts.
   *
   * Turning it off stops the server now, and disconnects dashboards. Turning it on takes effect when
   * the next OpMode is initialized.
   */
  @JvmStatic
  @Synchronized
  fun setServerEnabled(enabled: Boolean) {
    isServerEnabled = enabled
    try {
      prefs?.edit()?.putBoolean(KEY_SERVER_ENABLED, enabled)?.apply()
    } catch (e: Exception) {
      RobotLog.ee(TAG, e, "Couldn't save the telemetry server setting")
    }
    if (enabled) return

    live?.let {
      Telemetry.registry.unregister(it)
      it.close()
      live = null
    }
    try {
      server?.stop()
    } catch (e: Exception) {
      RobotLog.ee(TAG, e, "Couldn't stop the telemetry server")
    }
    server = null
  }

  @Synchronized
  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    isServerEnabled = prefs?.getBoolean(KEY_SERVER_ENABLED, true) ?: true
    eventLoop = ftcEventLoop
    Telemetry.registry.warningHandler = TelemetryWarningHandler { RobotLog.ww(TAG, it) }
    ftcEventLoop.opModeManager.registerListener(this)
  }

  @Synchronized
  override fun onOpModePreInit(opMode: OpMode?) {
    closeBackends()
    val registry = Telemetry.registry
    val name = eventLoop?.opModeManager?.activeOpModeName ?: opMode?.javaClass?.simpleName ?: "OpMode"
    // The server switches aren't worth a log of their own
    if (name == OpModeManagerImpl.DEFAULT_OP_MODE_NAME || opMode is ServerToggleOpMode) return

    if (config.logToFile) {
      try {
        config.logDirectory.mkdirs()
        val stamp = SimpleDateFormat("yyyy_MM_dd__HH_mm_ss", Locale.US).format(Date())
        val file =
          File(config.logDirectory, "${stamp}__${name.replace(Regex("[^A-Za-z0-9_.-]"), "_")}.wpilog")
        val writer = WPILogWriter(
          FileOutputStream(file),
          extraHeader = buildJsonObject {
            put("source", "NextFTC")
            put("opMode", name)
          }.toString(),
          timeSource = registry.timeSource,
          origin = registry.origin,
        )
        WPILogBackend(writer).also {
          log = it
          activeLogFile = file
          lastFlush = registry.timeSource.markNow()
          registry.register(it)
        }
      } catch (e: Exception) {
        RobotLog.ee(TAG, e, "Couldn't open a telemetry log")
      }
    }

    if (config.liveServer && isServerEnabled) {
      try {
        val server = server ?: NT4Server(
          config.serverPort,
          timeSource = registry.timeSource,
          origin = registry.origin,
          routes = { logDownloads.install(this) },
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

    val sdkTelemetry = opMode?.telemetry
    if (config.driverStation && sdkTelemetry != null) {
      DriverStationBackend(sdkTelemetry).also {
        driverStation = it
        registry.register(it, *config.driverStationPrefixes.toTypedArray())
      }
    }
  }

  /**
   * Sends the values logged since the last update to the Driver Station, like `telemetry.update()`
   * but only for the lines this library added. A `LinearOpMode` has to call it (or
   * `telemetry.update()`) every loop, because the SDK only updates after each loop of an iterative
   * `OpMode`. It does nothing when [FtcTelemetryConfig.driverStation] is off.
   *
   * It also flushes the log once every [FtcTelemetryConfig.logFlushIntervalMillis], so call it every
   * loop even with the Driver Station off.
   */
  @JvmStatic
  @Synchronized
  fun update() {
    driverStation?.update()
    flushLogIfDue()
  }

  private fun flushLogIfDue() {
    val log = log ?: return
    val interval = config.logFlushIntervalMillis
    if (interval <= 0) return
    if (lastFlush.elapsedNow() < interval.milliseconds) return
    lastFlush = Telemetry.registry.timeSource.markNow()
    try {
      log.writer.flush()
    } catch (e: Exception) {
      RobotLog.ee(TAG, e, "Couldn't flush the telemetry log")
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
    for (backend in listOfNotNull(log, live, driverStation)) {
      registry.unregister(backend)
      try {
        backend.close()
      } catch (e: Exception) {
        RobotLog.ee(TAG, e, "Couldn't close a telemetry backend")
      }
    }
    log = null
    activeLogFile = null
    live = null
    driverStation = null
  }
}

/**
 * Stops [FtcTelemetry]'s server when the Robot Controller app is destroyed. It's a separate object
 * because [OnCreateEventLoop] and [OnDestroy] declare incompatible `adjacencyRule` properties.
 */
internal object FtcTelemetryShutdown : OnDestroy {
  override fun onDestroy(context: Context) = FtcTelemetry.shutdown()
}
