package dev.nextftc.robot.coroutine

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.CommandBuilder
import com.pedropathing.ivy.Scheduler
import com.pedropathing.ivy.behaviors.EndCondition
import com.pedropathing.ivy.behaviors.EndCondition.INTERRUPTED
import com.pedropathing.ivy.behaviors.EndCondition.NATURALLY
import kotlin.coroutines.RestrictsSuspension

/**
 * The receiver of a [CoroutineCommand] body.
 *
 * Every suspension point corresponds to one loop iteration: the body runs a slice of work per
 * [Command.execute] call and suspends until the next one. Because the scope is
 * [RestrictsSuspension], only the functions declared here may be suspended on, which keeps the
 * body single threaded and on the OpMode loop.
 */
@RestrictsSuspension
interface CommandScope {
  /**
   * Suspends until the next loop iteration.
   */
  suspend fun yield()

  /**
   * Suspends until [condition] returns true, checking once per loop iteration.
   */
  suspend fun waitUntil(condition: () -> Boolean)

  /**
   * Suspends for at least [seconds].
   */
  suspend fun wait(seconds: Double)

  /**
   * Runs [command] inline, suspending until it is done.
   *
   * The command is driven by this coroutine rather than the [Scheduler], so its requirements are
   * not checked for conflicts: they should be part of the enclosing [CoroutineCommand]'s
   * requirements. If the enclosing command is interrupted while [command] is running, the command
   * is ended with [EndCondition.INTERRUPTED].
   */
  suspend fun await(command: Command)

  /**
   * Runs all [commands] inline, suspending until they are all done.
   *
   * Each command is ended with [EndCondition.NATURALLY] as soon as it finishes, and the ones still
   * running are ended with [EndCondition.INTERRUPTED] if the enclosing command is interrupted.
   */
  suspend fun awaitAll(vararg commands: Command)

  /**
   * Runs all [commands] inline, suspending until they are all done.
   */
  suspend fun awaitAll(commands: Collection<Command>) = awaitAll(*commands.toTypedArray())

  /**
   * Runs all [commands] inline, suspending until any of them is done. Returns the first one that completes.
   *
   * The winner is ended with [EndCondition.NATURALLY] and every other command with
   * [EndCondition.INTERRUPTED].
   *
   * @throws IllegalArgumentException if [commands] is empty, since there would be nothing to return
   */
  suspend fun awaitAny(vararg commands: Command): Command

  /**
   * Runs all [commands] inline, suspending until any of them is done. Returns the first one that completes.
   *
   * @throws IllegalArgumentException if [commands] is empty, since there would be nothing to return
   */
  suspend fun awaitAny(commands: Collection<Command>): Command = awaitAny(*commands.toTypedArray())

  /**
   * Starts [command] and returns immediately, without waiting for it to finish.
   *
   * Like [await], the command runs inline — it is ticked by the enclosing [CoroutineCommand] and
   * never reaches the [Scheduler], so its requirements are not checked for conflicts. The
   * enclosing command is not [Command.done] until every forked command has finished, and ending
   * the enclosing command ends any that are still running.
   *
   * To hand a command off to the [Scheduler] instead, call [Command.schedule] on it directly.
   */
  fun fork(command: Command)
}

/**
 * Creates a [CoroutineCommandBuilder] with the given body.
 */
fun command(body: suspend CommandScope.() -> Unit): CoroutineCommandBuilder =
  CoroutineCommandBuilder().setBody(body)

val command1 = CommandBuilder()
val command2 = CommandBuilder()
val command3 = CommandBuilder()

val example = command {
  // Example usage of the command builder
  awaitAll(command1, command2)
  wait(1.0)
  fork(command3)
}
