/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot.coroutine

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.Scheduler
import com.pedropathing.ivy.behaviors.BlockedBehavior
import com.pedropathing.ivy.behaviors.ConflictBehavior
import com.pedropathing.ivy.behaviors.EndCondition
import com.pedropathing.ivy.behaviors.InterruptedBehavior
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * A [Command] whose behavior is written as a sequential coroutine instead of as a
 * start/execute/done state machine.
 *
 * The [body] is advanced one slice per [execute] call and the command is [done] once the body
 * returns. Interrupting the command resumes the body with a [CancellationException], so `finally`
 * blocks run and inline commands started with [CommandScope.await] are ended.
 */
class CoroutineCommand(
  private val requirements: Set<Any> = emptySet(),
  private val priority: Int = 0,
  private val interruptedBehavior: InterruptedBehavior = InterruptedBehavior.END,
  private val conflictBehavior: ConflictBehavior = ConflictBehavior.OVERRIDE,
  private val blockedBehavior: BlockedBehavior = BlockedBehavior.CANCEL,
  private val body: suspend CommandScope.() -> Unit,
) : Command {
  private val forked = mutableListOf<Command>()
  private var next: Continuation<Unit>? = null
  private var finished = false
  private var cancelling = false

  override fun requirements(): Set<Any> = requirements

  override fun priority() = priority

  override fun interruptedBehavior() = interruptedBehavior

  override fun conflictBehavior() = conflictBehavior

  override fun blockedBehavior() = blockedBehavior

  override fun start() {
    finished = false
    cancelling = false
    forked.clear()
    next = body.createCoroutineUnintercepted(Scope(), object : Continuation<Unit> {
      override val context = EmptyCoroutineContext
      override fun resumeWith(result: Result<Unit>) {
        finished = true; next = null
        result.exceptionOrNull()?.let { if (it !is CancellationException) throw it }
      }
    })
  }

  override fun execute() {
    next?.let { c ->
      next = null
      c.resume(Unit)        // runs one slice
    }
    tickForked()
  }

  override fun done() = finished && forked.isEmpty()

  override fun end(endCondition: EndCondition) {
    // A suspended command is resumed later by the scheduler, so keep the continuation around.
    if (endCondition == EndCondition.SUSPENDED) return
    next?.let { c ->
      next = null
      cancelling = true
      try {
        // Unwinds the body: `finally` blocks run and awaited commands are ended.
        c.resumeWithException(CancellationException("$this was ended: $endCondition"))
      } finally {
        cancelling = false
        finished = true
        next = null
      }
    }
    forked.forEach { it.end(endCondition) }
    forked.clear()
  }

  /**
   * Runs one slice of every forked command, ending and dropping the ones that finished. The body
   * is suspended while this runs, so it cannot fork into the list concurrently.
   */
  private fun tickForked() {
    val iterator = forked.iterator()
    while (iterator.hasNext()) {
      val command = iterator.next()
      command.execute()
      if (command.done()) {
        command.end(EndCondition.NATURALLY)
        iterator.remove()
      }
    }
  }

  private inner class Scope : CommandScope {
    override suspend fun yield(): Unit = suspendCoroutineUninterceptedOrReturn { cont ->
      if (cancelling) throw CancellationException("${this@CoroutineCommand} was ended")
      next = cont
      COROUTINE_SUSPENDED
    }

    override suspend fun waitUntil(condition: () -> Boolean) {
      while (!condition()) yield()
    }

    override suspend fun wait(seconds: Double) {
      val deadline = System.nanoTime() + (seconds * 1_000_000_000).toLong()
      waitUntil { System.nanoTime() - deadline >= 0 }
    }

    override suspend fun await(command: Command) {
      command.start()
      try {
        while (!command.done()) {
          yield(); command.execute()
        }
        command.end(EndCondition.NATURALLY)
      } catch (e: Throwable) {
        command.end(EndCondition.INTERRUPTED); throw e
      }
    }

    override suspend fun awaitAll(vararg commands: Command) {
      val running = commands.toMutableList()
      running.removeAll { it.start(); it.endIfDone() }
      try {
        while (running.isNotEmpty()) {
          yield()
          running.removeAll { it.execute(); it.endIfDone() }
        }
      } catch (e: Throwable) {
        running.forEach { it.end(EndCondition.INTERRUPTED) }
        throw e
      }
    }

    override suspend fun awaitAny(vararg commands: Command): Command {
      require(commands.isNotEmpty()) { "awaitAny requires at least one command" }
      commands.forEach { it.start() }
      var winner = commands.firstOrNull { it.done() }
      try {
        while (winner == null) {
          yield()
          for (command in commands) {
            command.execute()
            if (command.done()) {
              winner = command
              break   // the rest are about to lose, so don't tick them again
            }
          }
        }
      } catch (e: Throwable) {
        commands.forEach { it.end(EndCondition.INTERRUPTED) }
        throw e
      }
      commands.forEach {
        it.end(if (it === winner) EndCondition.NATURALLY else EndCondition.INTERRUPTED)
      }
      return winner
    }

    /**
     * Ends this command naturally if it is done, returning whether it was.
     */
    private fun Command.endIfDone(): Boolean {
      if (!done()) return false
      end(EndCondition.NATURALLY)
      return true
    }

    override fun fork(command: Command) {
      command.start()
      if (command.done()) {
        command.end(EndCondition.NATURALLY)
        return
      }
      forked += command
    }
  }
}

