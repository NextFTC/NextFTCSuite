package dev.nextftc.robot

import dev.nextftc.util.Log
import com.qualcomm.robotcore.util.RobotLog as SDKRobotLog

/**
 * Messages shown to the driver on the Driver Station, prefixed with `NextFTC:`. Each is also written
 * to the robot log through [Log].
 */
internal object GlobalLog {
  /** Replaces the global error message with [message]. */
  fun setError(message: String) {
    Log.error(message)
    SDKRobotLog.setGlobalErrorMsg("NextFTC: $message")
  }

  /**
   * Replaces the global error message with [message], logging the stack trace of [throwable]
   * alongside it.
   */
  fun setError(message: String, throwable: Throwable) {
    Log.error(message, throwable)
    SDKRobotLog.setGlobalErrorMsg("NextFTC: $message")
  }

  /**
   * Replaces the global error message with [message] and throws [exception] (defaulting to a [NextFTCException] with [message]).
   */
  fun setErrorAndThrow(
    message: String,
    exception: NextFTCException = NextFTCException(message),
  ): Nothing {
    setError(message)
    throw exception
  }

  /** Clears the global error message, if any. */
  fun clearError() {
    SDKRobotLog.clearGlobalErrorMsg()
  }

  /** Adds [message] to the list of global warnings. */
  fun addWarning(message: String) {
    Log.warn(message)
    SDKRobotLog.addGlobalWarningMessage("NextFTC: $message")
  }
}
