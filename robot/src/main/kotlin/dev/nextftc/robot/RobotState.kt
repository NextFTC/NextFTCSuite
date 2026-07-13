package dev.nextftc.robot

import android.content.Context
import com.qualcomm.ftccommon.FtcEventLoop
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerNotifier
import dev.frozenmilk.sinister.sdk.apphooks.OnCreateEventLoop

object RobotState : OnCreateEventLoop, OpModeManagerNotifier.Notifications {
  lateinit var activeOpMode: OpMode
    private set

  override fun onCreateEventLoop(context: Context, ftcEventLoop: FtcEventLoop) {
    ftcEventLoop.opModeManager.registerListener(this)
  }

  override fun onOpModePreInit(opMode: OpMode?) {
    activeOpMode = opMode!!
    DriverStationTelemetry.telemetry = opMode.telemetry
  }

  override fun onOpModePreStart(opMode: OpMode?) {}

  override fun onOpModePostStop(opMode: OpMode?) {}
}
