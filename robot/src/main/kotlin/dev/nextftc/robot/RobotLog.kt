package dev.nextftc.robot

import dev.frozenmilk.sinister.util.log.Logger
import com.qualcomm.robotcore.util.RobotLog as SDKRobotLog

/**
 * Logs to the robot log under the `NextFTC` tag.
 *
 * Use [Global] for messages that should also surface on the Driver Station.
 */
internal object RobotLog {
  /** Logs [message] at debug level. */
  fun debug(message: String) {
    Logger.d("NextFTC", message)
  }

  /** Logs [message] at info level. */
  fun info(message: String) {
    Logger.i("NextFTC", message)
  }

  /** Logs [message] at warn level. */
  fun warn(message: String) {
    Logger.w("NextFTC", message)
  }

  /** Logs [message] at warn level, along with the stack trace of [throwable]. */
  fun warn(message: String, throwable: Throwable) {
    Logger.w("NextFTC", message, throwable)
  }

  /** Logs [message] at error level. */
  fun error(message: String) {
    Logger.e("NextFTC", message)
  }

  /** Logs [message] at error level, along with the stack trace of [throwable]. */
  fun error(message: String, throwable: Throwable) {
    Logger.e("NextFTC", message, throwable)
  }

  /**
   * Messages shown to the driver on the Driver Station, prefixed with `NextFTC:`.
   */
  object Global {
    /** Replaces the global error message with [message]. */
    fun setError(message: String) {
      this@RobotLog.error(message)
      SDKRobotLog.setGlobalErrorMsg("NextFTC: $message")
    }

    /**
     * Replaces the global error message with [message], logging the stack trace of [throwable]
     * alongside it.
     */
    fun setError(message: String, throwable: Throwable) {
      this@RobotLog.error(message, throwable)
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
      warn(message)
      SDKRobotLog.addGlobalWarningMessage("NextFTC: $message")
    }
  }
}
