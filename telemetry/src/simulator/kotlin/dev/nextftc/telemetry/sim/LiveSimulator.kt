/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.telemetry.sim

import dev.nextftc.telemetry.Telemetry
import dev.nextftc.telemetry.TelemetryLoggable
import dev.nextftc.telemetry.TelemetryRegistry
import dev.nextftc.telemetry.TelemetryTable
import dev.nextftc.telemetry.TelemetryWarningHandler
import dev.nextftc.telemetry.arrayTopic
import dev.nextftc.telemetry.nt.NT4Backend
import dev.nextftc.telemetry.nt.NT4Server
import dev.nextftc.telemetry.struct.StructName
import dev.nextftc.telemetry.topic
import dev.nextftc.telemetry.withUnit
import dev.nextftc.telemetry.wpilog.WPILogBackend
import dev.nextftc.telemetry.wpilog.WPILogWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.exitProcess

/**
 * A fake robot for trying out live telemetry without a Control Hub.
 *
 * It runs a NetworkTables 4 server and plays Auto, a pause, then TeleOp, over and over. Each OpMode
 * gets a new log and a new set of live topics, like the Robot Controller will, and the log is
 * written to `build/sim-logs`. In AdvantageScope, connect to the robot at `localhost`, or open a
 * log from that folder.
 *
 * It wires telemetry the way the robot module will. One server lives for the whole run, and each
 * OpMode registers a fresh [WPILogBackend] and [NT4Backend] on the registry and closes them when it
 * ends, which removes that OpMode's topics. The handles are made once and used by every OpMode. The
 * logs and the server share the registry's clock, so a value has one timestamp everywhere. Only the
 * paths under `/Robot`, `/Drive`, `/Battery`, `/Loop`, `/Status`, `/OpMode` and `/Stress` go to the
 * server. `/Debug` goes to the log file alone.
 *
 * A name keeps its type for the whole run, because AdvantageScope keeps a field's first type and
 * refuses values of another with a "Conflicting types" error. `/Robot/Phase` is a string in both
 * OpModes. With `--conflict true` TeleOp logs it as an integer instead, to show what a mistake like
 * that looks like. The server refuses it and telemetry warns, so AdvantageScope never sees the
 * integers, though the TeleOp log file still has them.
 *
 * Run it with `./gradlew :telemetry:simulateLive`, and pass options with
 * `-PsimArgs="--auto 5 --idle 1 --teleop 5"`. The options are:
 * - `--port`, the port to serve on. 5810 by default, and 0 picks a free one
 * - `--auto`, `--idle` and `--teleop`, how many seconds each lasts
 * - `--hz`, how often the robot loop runs
 * - `--topics`, how many extra topics to update every loop, to see what a lot of traffic does
 * - `--reboot true`, to stop the server and start it again after each TeleOp, as a Robot Controller
 *   restart would, so clients have to reconnect
 * - `--conflict true`, to log `/Robot/Phase` with a different type in TeleOp than in Auto
 * - `--cycles`, how many times to play Auto and TeleOp before exiting. 0 means forever
 * - `--log-dir`, where the logs go
 */
fun main(args: Array<String>) {
  val options =
    try {
      SimulatorOptions.parse(args)
    } catch (e: IllegalArgumentException) {
      System.err.println(e.message)
      System.err.println(USAGE)
      exitProcess(2)
    }
  Simulation(options).run()
}

private const val USAGE =
  "Options: --port N --auto S --idle S --teleop S --hz N --topics N --reboot true " +
    "--conflict true --cycles N --log-dir PATH"

/** What the simulator was asked to do. See `--help` in [main]. */
internal class SimulatorOptions(
  val port: Int = 5810,
  val autoSeconds: Double = 15.0,
  val idleSeconds: Double = 3.0,
  val teleopSeconds: Double = 30.0,
  val hz: Int = 50,
  val extraTopics: Int = 0,
  val reboot: Boolean = false,
  val conflict: Boolean = false,
  val cycles: Int = 0,
  val logDir: File = File("build/sim-logs"),
) {
  init {
    require(port in 0..65535) { "--port must be from 0 to 65535, but was $port" }
    require(hz in 1..1000) { "--hz must be from 1 to 1000, but was $hz" }
    require(autoSeconds >= 0 && idleSeconds >= 0 && teleopSeconds >= 0) {
      "--auto, --idle and --teleop can't be negative"
    }
    require(extraTopics >= 0) { "--topics can't be negative, but was $extraTopics" }
    require(cycles >= 0) { "--cycles can't be negative, but was $cycles" }
  }

  companion object {
    private val KNOWN =
      setOf(
        "port",
        "auto",
        "idle",
        "teleop",
        "hz",
        "topics",
        "reboot",
        "conflict",
        "cycles",
        "log-dir",
      )

    fun parse(args: Array<String>): SimulatorOptions {
      require(args.size % 2 == 0) { "The option ${args.last()} needs a value" }
      val values =
        args.toList().chunked(2).associate { (key, value) ->
          require(key.startsWith("--")) { "Expected an option like --port, but got $key" }
          key.removePrefix("--") to value
        }
      (values.keys - KNOWN).firstOrNull()?.let { throw IllegalArgumentException("Unknown option --$it") }

      val defaults = SimulatorOptions()
      return SimulatorOptions(
        port = values["port"]?.toInt() ?: defaults.port,
        autoSeconds = values["auto"]?.toDouble() ?: defaults.autoSeconds,
        idleSeconds = values["idle"]?.toDouble() ?: defaults.idleSeconds,
        teleopSeconds = values["teleop"]?.toDouble() ?: defaults.teleopSeconds,
        hz = values["hz"]?.toInt() ?: defaults.hz,
        extraTopics = values["topics"]?.toInt() ?: defaults.extraTopics,
        reboot = values["reboot"]?.toBooleanStrict() ?: defaults.reboot,
        conflict = values["conflict"]?.toBooleanStrict() ?: defaults.conflict,
        cycles = values["cycles"]?.toInt() ?: defaults.cycles,
        logDir = values["log-dir"]?.let(::File) ?: defaults.logDir,
      )
    }
  }
}

// These match WPILib's Pose2d, so AdvantageScope draws them: the field is in meters and the angle is
// in radians.
@StructName("Translation2d")
private data class Translation(val x: Double, val y: Double)

@StructName("Rotation2d")
private data class Rotation(val value: Double)

@StructName("Pose2d")
private data class SimPose(val translation: Translation, val rotation: Rotation)

private class Intake(val position: Double, val jammed: Boolean) : TelemetryLoggable {
  override fun logTo(table: TelemetryTable) {
    table.log("Position", position)
    table.log("Jammed", jammed)
  }
}

private enum class Alliance { RED, BLUE }

/** The middle of an FTC field, in meters. */
private const val FIELD_CENTER = 1.83

/** A closed curve with the given radii that takes [period] seconds to go around once. */
private fun pose(t: Double, rx: Double, ry: Double, period: Double): SimPose {
  val angle = 2 * PI * t / period
  val dx = -rx * sin(angle)
  val dy = ry * cos(angle)
  return SimPose(
    Translation(FIELD_CENTER + rx * cos(angle), FIELD_CENTER + ry * sin(angle)),
    Rotation(atan2(dy, dx)),
  )
}

private fun pause(seconds: Double) = Thread.sleep((seconds * 1000).toLong())

/**
 * The simulated robot: a server that lasts the whole run, and the OpModes that log through it.
 *
 * @param registry Where the robot logs. It is the shared one by default, and a test passes its own.
 * @param onWarning Where telemetry's warnings go. There should be none.
 */
internal class Simulation(
  private val options: SimulatorOptions,
  private val registry: TelemetryRegistry = Telemetry.registry,
  onWarning: (String) -> Unit = { println("WARNING: $it") },
) : AutoCloseable {
  /** The server dashboards connect to. It outlives every OpMode, like the robot's will. */
  val server = NT4Server(options.port, timeSource = registry.timeSource, origin = registry.origin)

  // The handles are made once, here, and used by every OpMode. They keep working as each OpMode's
  // backends come and go.
  private val event = registry.table("OpMode").stringTopic("Event")
  private val loopCount = registry.root.intTopic("Loop/Count")
  private val battery = registry.root.doubleTopic("Battery/Voltage").withUnit("V")
  private val status = registry.root.stringTopic("Status")

  private val drive = registry.table("Drive")
  private val leftVelocity = drive.doubleTopic("Left/Velocity").withUnit("m/s")
  private val rightVelocity = drive.doubleTopic("Right/Velocity").withUnit("m/s")
  private val enabled = drive.booleanTopic("Enabled")
  private val motorPowers = drive.doubleArrayTopic("MotorPowers")

  private val robot = registry.table("Robot")
  private val pose = robot.topic<SimPose>("Pose")
  private val targets = robot.arrayTopic<SimPose>("Targets")
  private val distances = robot.doubleArrayTopic("Distances").withUnit("m")
  private val phase = robot.stringTopic("Phase")

  /** Only used with `--conflict`, to log the same path as a different type on purpose. */
  private val phaseNumber = robot.intTopic("Phase")

  /** How long the logging calls took each loop, which goes to the log file and not the server. */
  private val logNanos = registry.root.longTopic("Debug/LogNanos")

  private val stress = List(options.extraTopics) { registry.table("Stress").doubleTopic("t$it") }

  init {
    registry.warningHandler = TelemetryWarningHandler { onWarning(it) }
  }

  /** Plays Auto and TeleOp for [SimulatorOptions.cycles] rounds, or forever, then stops the server. */
  fun run() {
    server.start()
    println(
      "Simulated robot streaming NetworkTables 4 on port ${server.boundPort} " +
        "(Auto ${options.autoSeconds}s, TeleOp ${options.teleopSeconds}s, ${options.hz} Hz)",
    )
    println("Connect AdvantageScope to the robot at localhost. /Debug is logged to the file only.")
    println("Logs are written to ${options.logDir.absolutePath}")

    try {
      var cycle = 0
      while (options.cycles == 0 || cycle < options.cycles) {
        runOpMode("Auto", options.autoSeconds) { t -> pose(t, 1.2, 0.9, 8.0) }
        pause(options.idleSeconds)
        runOpMode("TeleOp", options.teleopSeconds) { t -> pose(t, 1.5, 0.6, 6.0) }
        cycle++
        if (options.reboot) {
          println("Rebooting: stopping the server and starting it again")
          server.stop()
          server.start()
          println("Streaming again on port ${server.boundPort}")
        }
        pause(options.idleSeconds)
      }
    } finally {
      server.stop()
    }
  }

  override fun close() = server.stop()

  /**
   * Runs one OpMode: opens a log, registers a log backend and a live backend on the registry, logs
   * for [seconds], then unregisters and closes them. This is what the robot module does around a
   * real OpMode.
   */
  private fun runOpMode(name: String, seconds: Double, path: (Double) -> SimPose) {
    options.logDir.mkdirs()
    val stamp = SimpleDateFormat("yyyy_MM_dd__HH_mm_ss_SSS").format(Date())
    val file = File(options.logDir, "${stamp}__$name.wpilog")

    val log =
      WPILogBackend(
        WPILogWriter(
          FileOutputStream(file),
          extraHeader = """{"source":"NextFTC telemetry simulator","opMode":"$name"}""",
          timeSource = registry.timeSource,
          origin = registry.origin,
        ),
      )
    val live = NT4Backend(server)
    registry.register(log)
    registry.register(
      live,
      "/OpMode",
      "/Robot",
      "/Drive",
      "/Battery",
      "/Loop",
      "/Status",
      "/Stress",
    )

    println("$name: PRE_INIT (${file.name})")
    event.set("PRE_INIT")
    pause(0.5)
    println("$name: PRE_START")
    event.set("PRE_START")

    val period = 1_000_000_000L / options.hz
    val start = System.nanoTime()
    var loop = 0
    var nextTick = start
    var nextReport = start + 1_000_000_000L
    var spentSinceReport = 0L
    var loopsSinceReport = 0

    while (true) {
      val now = System.nanoTime()
      val t = (now - start) / 1e9
      if (t >= seconds) break

      val p = path(t)
      val logStart = System.nanoTime()
      loopCount.set(loop)
      battery.set(12.6 - 0.01 * t + 0.04 * sin(t * 3))
      status.set(if (t < seconds / 2) "SEARCHING" else "TRACKING")

      leftVelocity.set(30 * sin(t))
      rightVelocity.set(30 * cos(t))
      enabled.set((t.toInt() / 5) % 2 == 0)
      motorPowers.set(doubleArrayOf(sin(t), cos(t), -sin(t), -cos(t)))

      pose.set(p)
      targets.set(
        listOf(
          SimPose(Translation(0.6, 3.0), Rotation(0.0)),
          SimPose(Translation(3.0, 3.0), Rotation(PI / 2)),
          SimPose(Translation(FIELD_CENTER + 0.2 * sin(t), 0.6), Rotation(PI)),
        ),
      )
      distances.set(doubleArrayOf(0.1 + 0.05 * sin(t), 0.2 + 0.05 * cos(t), 0.3))
      robot.log("Alliance", if (name == "Auto") Alliance.RED.name else Alliance.BLUE.name)
      robot.log(
        "Intake",
        Intake(position = (t / seconds).coerceIn(0.0, 1.0), jammed = (t.toInt() % 7) == 6),
      )
      if (name == "TeleOp" && options.conflict) phaseNumber.set(2) else phase.set(name.uppercase())
      for ((i, topic) in stress.withIndex()) topic.set(sin(t + i))

      val spent = System.nanoTime() - logStart
      logNanos.set(spent)
      spentSinceReport += spent
      loopsSinceReport++

      loop++
      if (now >= nextReport) {
        println(
          "$name: ${"%.0f".format(t)}s, $loop loops, ${server.clientCount} client(s), " +
            "logging took ${spentSinceReport / loopsSinceReport / 1000} µs a loop",
        )
        nextReport += 1_000_000_000L
        spentSinceReport = 0
        loopsSinceReport = 0
      }

      nextTick += period
      val sleepNanos = nextTick - System.nanoTime()
      if (sleepNanos > 0) Thread.sleep(sleepNanos / 1_000_000, (sleepNanos % 1_000_000).toInt())
    }

    event.set("POST_STOP")
    registry.unregister(log)
    registry.unregister(live)
    log.close()
    live.close()
    println("$name: POST_STOP after $loop loops")
  }
}
