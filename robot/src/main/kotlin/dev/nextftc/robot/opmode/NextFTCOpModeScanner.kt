package dev.nextftc.robot.opmode

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import dev.frozenmilk.sinister.sdk.opmodes.OpModeScanner
import dev.frozenmilk.sinister.sdk.opmodes.TeleopAutonomousOpModeScanner
import dev.frozenmilk.sinister.targeting.SearchTarget
import dev.frozenmilk.sinister.targeting.WideSearch
import dev.frozenmilk.sinister.util.log.Logger
import dev.frozenmilk.util.graph.rule.dependsOn
import dev.nextftc.robot.RobotScanner
import org.firstinspires.ftc.robotcore.internal.opmode.OpModeMeta
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSuperclassOf

/**
 * Scans the user's project for OpModes that take a [dev.nextftc.robot.NextRobot] instance in their constructor.
 *
 * Automatically handles registering these OpModes with the FTC dashboard while intercepting
 * their instantiation to inject the [dev.nextftc.robot.RobotScanner.robot] instance.
 */
object NextFTCOpModeScanner : OpModeScanner() {
  override val loadAdjacencyRule = super.loadAdjacencyRule and dependsOn(
    RobotScanner,
  ) and dependsOn(TeleopAutonomousOpModeScanner)
  override val unloadAdjacencyRule = super.unloadAdjacencyRule and dependsOn(RobotScanner)

  override val targets: SearchTarget = WideSearch()

  @Suppress("UNCHECKED_CAST")
  override fun scan(loader: ClassLoader, cls: Class<*>, registrationHelper: RegistrationHelper) {
    val modifiers = cls.modifiers
    if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers)) return

    if (!NextOpMode::class.java.isAssignableFrom(cls)) return

    val kcls = cls.kotlin as KClass<NextOpMode>

    if (kcls.hasAnnotation<Disabled>()) {
      Logger.i("NextFTC", "Skipping disabled NextFTC OpMode class: $kcls")
      return
    }

    when (val metaResult = opModeMetaFromClass(kcls)) {
      is OpModeMetaCheckResult.FoundAnnotation -> {
        when (val constructorResult = opModeConstructorFromClass(kcls)) {
          is OpModeConstructorCheckResult.FoundConstructor -> {
            Logger.i("NextFTC", "Found NextFTC OpMode class: $cls")
            registrationHelper.register(metaResult.meta) { BoundNextOpMode(constructorResult.constructor) }
          }
          is OpModeConstructorCheckResult.NoConstructorFound -> {
            Logger.w("NextFTC", "No valid constructor found for NextFTC OpMode class: $cls")
          }
        }
      }
      is OpModeMetaCheckResult.NoAnnotationPresent -> {
        Logger.w(
          "NextFTC",
          "No @NextAutonomous or @NextTeleop annotation found for NextFTC OpMode class: $cls",
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

internal fun opModeMetaFromClass(cls: KClass<*>): OpModeMetaCheckResult {
  val autonomous = cls.findAnnotation<NextAutonomous>()
  if (autonomous != null) {
    return OpModeMetaCheckResult.FoundAnnotation(
      OpModeMeta.Builder().setFlavor(OpModeMeta.Flavor.AUTONOMOUS)
        .setName(autonomous.name.ifEmpty { cls.simpleName!! })
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
        .setName(teleop.name.ifEmpty { cls.simpleName!! })
        .setGroup(teleop.group.ifEmpty { "NextFTC Teleop" })
        .setSource(OpModeMeta.Source.ANDROID_STUDIO)
        .build(),
    )
  }

  return OpModeMetaCheckResult.NoAnnotationPresent(cls.simpleName!!)
}

internal fun opModeConstructorFromClass(cls: KClass<out NextOpMode>): OpModeConstructorCheckResult {
  if (RobotScanner.foundRobot) {
    val constructor = cls.constructors.find { it.parameters.size == 1 }
    if (constructor != null) {
      val paramType = constructor.parameters[0].type.classifier as KClass<*>
      if (paramType.isSuperclassOf(RobotScanner.robotClass)) {
        return OpModeConstructorCheckResult.FoundConstructor { constructor.call(RobotScanner.robot) }
      }
    }
  }

  val constructor = cls.constructors.find { it.parameters.isEmpty() }
  if (constructor != null) {
    return OpModeConstructorCheckResult.FoundConstructor { constructor.call() }
  }

  return OpModeConstructorCheckResult.NoConstructorFound(cls.simpleName!!)
}
