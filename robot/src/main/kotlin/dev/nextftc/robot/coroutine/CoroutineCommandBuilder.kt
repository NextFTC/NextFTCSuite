package dev.nextftc.robot.coroutine

import com.pedropathing.ivy.Command
import com.pedropathing.ivy.behaviors.BlockedBehavior
import com.pedropathing.ivy.behaviors.ConflictBehavior
import com.pedropathing.ivy.behaviors.EndCondition
import com.pedropathing.ivy.behaviors.InterruptedBehavior

/**
 * Builds a [CoroutineCommand], in the same fluent style as [com.pedropathing.ivy.CommandBuilder].
 *
 * The builder is itself a [com.pedropathing.ivy.Command], so it can be scheduled or composed with the usual Ivy
 * operators without calling [build]:
 *
 * ```kotlin
 * coroutineCommand {
 *   await(arm.toHigh())
 *   wait(0.5)
 *   fork(claw.open())
 * }.requiring(arm).setPriority(1).schedule()
 * ```
 *
 * A fresh [CoroutineCommand] is created from the current configuration every time the builder is
 * started, so one builder can be scheduled repeatedly.
 */
class CoroutineCommandBuilder : Command {
  private var requirements: Set<Any> = emptySet()
  private var priority = 0
  private var interruptedBehavior = InterruptedBehavior.END
  private var conflictBehavior = ConflictBehavior.OVERRIDE
  private var blockedBehavior = BlockedBehavior.CANCEL
  private var body: suspend CommandScope.() -> Unit = {}
  private var running: CoroutineCommand? = null

  /**
   * Sets the body of the command. See [CommandScope] for what it can suspend on.
   */
  fun setBody(body: suspend CommandScope.() -> Unit) = apply { this.body = body }

  /**
   * Sets the requirements of the command.
   */
  fun requiring(requirements: Set<Any>) = apply { this.requirements = requirements }

  /**
   * Sets the requirements of the command.
   */
  fun requiring(vararg requirements: Any) = requiring(requirements.toSet())

  /**
   * Sets the priority the command has over commands with conflicting requirements.
   */
  fun setPriority(priority: Int) = apply { this.priority = priority }

  /**
   * Sets what the command does when interrupted.
   */
  fun setInterruptedBehavior(interruptedBehavior: InterruptedBehavior) =
    apply { this.interruptedBehavior = interruptedBehavior }

  /**
   * Sets what the command does when a conflicting command with an equal priority is running.
   */
  fun setConflictBehavior(conflictBehavior: ConflictBehavior) =
    apply { this.conflictBehavior = conflictBehavior }

  /**
   * Sets what the command does when blocked by a command with a higher priority.
   */
  fun setBlockedBehavior(blockedBehavior: BlockedBehavior) =
    apply { this.blockedBehavior = blockedBehavior }

  /**
   * Creates a [CoroutineCommand] from the current configuration. Later changes to the builder do
   * not affect commands it has already built.
   */
  fun build() = CoroutineCommand(
    requirements,
    priority,
    interruptedBehavior,
    conflictBehavior,
    blockedBehavior,
    body,
  )

  override fun requirements() = requirements

  override fun priority() = priority

  override fun interruptedBehavior() = interruptedBehavior

  override fun conflictBehavior() = conflictBehavior

  override fun blockedBehavior() = blockedBehavior

  override fun start() {
    running = build().also { it.start() }
  }

  override fun execute() {
    running?.execute()
  }

  override fun done() = running?.done() ?: false

  override fun end(endCondition: EndCondition) {
    val command = running ?: return
    command.end(endCondition)
    // A suspended command is resumed later, so it has to survive until it really ends.
    if (endCondition != EndCondition.SUSPENDED) running = null
  }
}
