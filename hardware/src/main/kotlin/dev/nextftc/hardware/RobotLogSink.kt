package dev.nextftc.hardware

import dev.frozenmilk.sinister.util.log.Logger
import dev.nextftc.util.LogLevel
import dev.nextftc.util.LogSink

/** Sends [dev.nextftc.util.Log] messages to the robot log under the `NextFTC` tag. */
object RobotLogSink : LogSink {
  private const val TAG = "NextFTC"

  override fun log(level: LogLevel, message: String, throwable: Throwable?) {
    when (level) {
      LogLevel.DEBUG -> Logger.d(TAG, message)
      LogLevel.INFO -> Logger.i(TAG, message)
      LogLevel.WARN -> if (throwable != null) Logger.w(TAG, message, throwable) else Logger.w(TAG, message)
      LogLevel.ERROR -> if (throwable != null) Logger.e(TAG, message, throwable) else Logger.e(TAG, message)
    }
  }
}
