package dev.nextftc.robot.opmode

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import dev.frozenmilk.sinister.sdk.opmodes.AnnotatedOpModeScanner
import dev.frozenmilk.sinister.sdk.opmodes.OpModeScanner
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.WideSearch
import dev.frozenmilk.util.graph.rule.dependsOn
import dev.nextftc.robot.RobotLog
import dev.nextftc.robot.RobotScanner
import dev.nextftc.robot.RobotState
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf

/**
 * Scans the user's project for OpModes that take a [dev.nextftc.robot.NextRobot] instance in their constructor.
 *
 * Automatically handles registering these OpModes with the FTC dashboard while intercepting
 * their instantiation to inject the [dev.nextftc.robot.RobotState.robot] instance.
 */
object NextFTCOpModeScanner : OpModeScanner() {
  override val loadAdjacencyRule = super.loadAdjacencyRule and dependsOn(
    RobotScanner,
  ) and dependsOn(AnnotatedOpModeScanner)
  override val unloadAdjacencyRule = super.unloadAdjacencyRule and dependsOn(RobotScanner)

  override val targets: SearchTarget = WideSearch()

  @Suppress("UNCHECKED_CAST")
  override fun scan(loader: ClassLoader, cls: Class<*>, registrationHelper: RegistrationHelper) {
    val modifiers = cls.modifiers
    if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers)) return

    if (!NextOpMode::class.java.isAssignableFrom(cls)) return

    val kcls = cls.kotlin as KClass<NextOpMode>

    if (kcls.hasAnnotation<Disabled>()) {
      RobotLog.info("Skipping disabled NextFTC OpMode class: $kcls")
      return
    }

    when (val metaResult = opModeMetaFromClass(kcls)) {
      is OpModeMetaCheckResult.FoundAnnotation -> {
        when (val constructorResult = opModeConstructorFromClass(kcls)) {
          is OpModeConstructorCheckResult.FoundConstructor -> {
            RobotLog.info("Found NextFTC OpMode class: $cls")
            registrationHelper.register(metaResult.meta) { BoundNextOpMode(constructorResult.constructor) }
          }
          is OpModeConstructorCheckResult.NoConstructorFound -> {
            RobotLog.Global.addWarning(
              "No valid constructor found for NextFTC OpMode class $cls, so it was not " +
                "registered. Ensure it has a public constructor that takes either no arguments " +
                "or your NextRobot type.",
            )
          }
        }
      }
      is OpModeMetaCheckResult.NoAnnotationPresent -> {
        RobotLog.Global.addWarning(
          "No @NextAutonomous, @NextTeleop, or @NextUtility annotation found for NextFTC OpMode " +
            "class $cls, so it was not registered.",
        )
      }
    }
  }
}

sealed interface OpModeMetaCheckResult {
  data class FoundAnnotation(val meta: OpModeMeta) : OpModeMetaCheckResult

  data class NoAnnotationPresent(val className: String) : OpModeMetaCheckResult
}

sealed interface OpModeConstructorCheckResult {
  data class FoundConstructor(val constructor: () -> NextOpMode) : OpModeConstructorCheckResult

  data class NoConstructorFound(val opModeName: String) : OpModeConstructorCheckResult
}

/**
 * The name an OpMode or robot class is displayed under when no name is configured. [KClass.simpleName]
 * is null for classes that have no source-level name, so fall back to the binary name.
 */
internal val KClass<*>.displayName: String
  get() = simpleName ?: java.name

internal fun opModeMetaFromClass(cls: KClass<*>): OpModeMetaCheckResult {
  val autonomous = cls.findAnnotation<NextAutonomous>()
  if (autonomous != null) {
    return OpModeMetaCheckResult.FoundAnnotation(
      OpModeMeta.Builder().setFlavor(OpModeMeta.Flavor.AUTONOMOUS)
        .setName(autonomous.name.ifEmpty { cls.displayName })
        .setGroup(autonomous.group.ifEmpty { "NextFTC Auto" })
        .setTransitionTarget(autonomous.preselectTeleop)
        .setSource(OpModeMeta.Source.ANDROID_STUDIO)
        .build(),
    )
  }

  val teleop = cls.findAnnotation<NextTeleop>()
  if (teleop != null) {
    return OpModeMetaCheckResult.FoundAnnotation(
      OpModeMeta.Builder().setFlavor(OpModeMeta.Flavor.TELEOP)
        .setName(teleop.name.ifEmpty { cls.displayName })
        .setGroup(teleop.group.ifEmpty { "NextFTC Teleop" })
        .setSource(OpModeMeta.Source.ANDROID_STUDIO)
        .build(),
    )
  }

  val utility = cls.findAnnotation<NextUtility>()
  if (utility != null) {
    return OpModeMetaCheckResult.FoundAnnotation(
      OpModeMeta.Builder().setFlavor(OpModeMeta.Flavor.UTILITY)
        .setName(utility.name.ifEmpty { cls.displayName })
        .setDescription(utility.description.ifEmpty { null })
        .setSource(OpModeMeta.Source.ANDROID_STUDIO)
        .build(),
    )
  }

  return OpModeMetaCheckResult.NoAnnotationPresent(cls.displayName)
}

internal fun opModeConstructorFromClass(cls: KClass<out NextOpMode>): OpModeConstructorCheckResult {
  val robotClass = RobotState.robotClass

  if (robotClass != null) {
    val oneArg = cls.constructors.find {
      it.parameters.size == 1 && it.visibility == KVisibility.PUBLIC
    }
    val paramType = oneArg?.parameters?.single()?.type?.classifier as? KClass<*>
    if (oneArg != null && paramType != null && paramType.isSuperclassOf(robotClass)) {
      return OpModeConstructorCheckResult.FoundConstructor { oneArg.call(RobotState.robot) }
    }
  }

  val noArg = cls.constructors.find {
    it.parameters.isEmpty() && it.visibility == KVisibility.PUBLIC
  }

  if (noArg != null) {
    return OpModeConstructorCheckResult.FoundConstructor { noArg.call() }
  }

  return OpModeConstructorCheckResult.NoConstructorFound(cls.displayName)
}
