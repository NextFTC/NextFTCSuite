package dev.nextftc.util

/** How serious a log message is. */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/** Where [Log] sends its messages. */
fun interface LogSink {
  /** Records [message] at [level], with the stack trace of [throwable] if there is one. */
  fun log(level: LogLevel, message: String, throwable: Throwable?)
}

/**
 * A logging facade for code that can't depend on the FTC SDK.
 *
 * Messages go to [sink]. The default prints warnings and errors to standard error, which is what you
 * want in unit tests; `hardware`'s `RobotController` installs one that writes to the Robot Controller
 * log.
 */
object Log {
  /** The destination for every message. Replace it to redirect logging. */
  @Volatile
  @JvmStatic
  var sink: LogSink = StderrSink

  /** Logs [message] at debug level. */
  @JvmStatic
  fun debug(message: String) = sink.log(LogLevel.DEBUG, message, null)

  /** Logs [message] at info level. */
  @JvmStatic
  fun info(message: String) = sink.log(LogLevel.INFO, message, null)

  /** Logs [message] at warn level, with the stack trace of [throwable] if given. */
  @JvmStatic
  @JvmOverloads
  fun warn(message: String, throwable: Throwable? = null) = sink.log(LogLevel.WARN, message, throwable)

  /** Logs [message] at error level, with the stack trace of [throwable] if given. */
  @JvmStatic
  @JvmOverloads
  fun error(message: String, throwable: Throwable? = null) = sink.log(LogLevel.ERROR, message, throwable)

  private object StderrSink : LogSink {
    override fun log(level: LogLevel, message: String, throwable: Throwable?) {
      if (level < LogLevel.WARN) return
      System.err.println("NextFTC [$level] $message")
      throwable?.printStackTrace()
    }
  }
}
