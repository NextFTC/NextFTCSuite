package dev.nextftc.robot.triggers

import dev.nextftc.robot.coroutine.command
import java.math.BigDecimal

/**
 * A set of selectable options controlled by a [CommandGamepad].
 *
 * The D-pad up and down buttons select which selectable is active. The D-pad left and right
 * buttons change the selected option in the active selectable.
 *
 * @param gamepad The CommandGamepad used to change selections.
 */
class OpModeSelector(
    val gamepad: CommandGamepad
) {
    private val selectables = mutableListOf<GamepadSelectable>()
    private val selectableMap = linkedMapOf<String, GamepadSelectable>()

    private var selectedSelectable = 0

    init {
        gamepad.dpadUp.onTrue(command {
            if (selectables.isNotEmpty()) {
                selectedSelectable = wrap(
                    selectedSelectable - 1,
                    selectables.size
                )
            }
        })

        gamepad.dpadDown.onTrue(command {
            if (selectables.isNotEmpty()) {
                selectedSelectable = wrap(
                    selectedSelectable + 1,
                    selectables.size
                )
            }
        })

        gamepad.dpadLeft.onTrue(command {
            if (selectables.isNotEmpty()) {
                selectables[selectedSelectable].selectPrevious()
            }
        })

        gamepad.dpadRight.onTrue(command {
            if (selectables.isNotEmpty()) {
                selectables[selectedSelectable].selectNext()
            }
        })
    }

    /**
     * Get the gamepad used to change selections.
     *
     * @return the gamepad
     */
    fun getGamepad(): CommandGamepad {
        return gamepad
    }

    /**
     * Adds a selectable with the given options.
     *
     * The first option is selected by default.
     *
     * @param name selectable name
     * @param options available options
     * @return the created selectable
     */
    fun addOptions(name: String, options: List<String>): GamepadSelectable {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(options.isNotEmpty()) { "Options cannot be empty" }
        require(name !in selectableMap) {
            "GamepadSelectable already exists: $name"
        }

        val selectable = GamepadSelectable(name, options.toList())

        selectables.add(selectable)
        selectableMap[name] = selectable

        return selectable
    }

    /**
     * Adds a selectable with the given options.
     *
     * The first option is selected by default.
     *
     * @param name selectable name
     * @param options available options
     * @return the created selectable
     */

    fun addOptions(name: String, vararg options: String) =
        addOptions(name, options.toList())

    /**
     * Adds a selectable with integer options from min to max, inclusive, stepping by
     * delta.
     *
     * The first option is selected by default.
     *
     * @param name selectable name
     * @param min minimum option
     * @param max maximum option
     * @param delta amount between options
     * @return the created selectable
     */
    fun addIntegerOptions(
        name: String,
        min: Int,
        max: Int,
        delta: Int
    ): GamepadSelectable {
        require(min <= max) { "Minimum cannot be greater than maximum" }
        require(delta > 0) { "Delta must be greater than zero" }

        val options = mutableListOf<String>()
        var value = min.toLong()

        while (value <= max) {
            options.add(value.toString())
            value += delta
        }

        if (options.last() != max.toString()) {
            options.add(max.toString())
        }

        return addOptions(name, options)
    }


    /**
     * Adds a selectable with double options from min to max, inclusive, stepping by
     * delta.
     *
     * The first option is selected by default.
     *
     * @param name selectable name
     * @param min minimum option
     * @param max maximum option
     * @param delta amount between options
     * @return the created selectable
     */
    fun addDoubleOptions(
        name: String,
        min: Double,
        max: Double,
        delta: Double
    ): GamepadSelectable {
        require(min.isFinite() && max.isFinite() && delta.isFinite()) {
            "Values must be finite"
        }
        require(min <= max) { "Minimum cannot be greater than maximum" }
        require(delta > 0.0) { "Delta must be greater than zero" }

        val options = mutableListOf<String>()

        val maximum = BigDecimal.valueOf(max)
        val increment = BigDecimal.valueOf(delta)
        var value = BigDecimal.valueOf(min)

        while (value <= maximum) {
            options.add(value.toPlainString())
            value = value.add(increment)
        }

        if (BigDecimal(options.last()).compareTo(maximum) != 0) {
            options.add(maximum.toPlainString())
        }

        return addOptions(name, options)
    }

    /**
     * Gets the currently selected option for a selectable.
     *
     * @param name selectable name
     * @return the currently selected option
     */
    fun getSelected(name: String): String =
        getSelectable(name).selected

    /**
     * Gets the currently selected integer option for a selectable.
     *
     * @param name selectable name
     * @return the currently selected option
     */
    fun getSelectedInteger(name: String): Int =
        getSelected(name).toInt()



    /**
     * Gets the currently selected double option for a selectable.
     *
     * @param name selectable name
     * @return the currently selected option
     */
    fun getSelectedDouble(name: String): Double =
        getSelected(name).toDouble()

    /**
     * Gets the currently selected option index for a selectable.
     *
     * @param name selectable name
     * @return the currently selected option index
     */
    fun getSelectedIndex(name: String): Int =
        getSelectable(name).selectedIndex

    /**
     * Gets the names of all selectables.
     *
     * @return selectable names
     */
    fun getSelectableNames(): List<String> =
        selectableMap.keys.toList()

    /**
     * Gets the selectable currently controlled by D-pad left and right.
     *
     * @return the selected selectable
     */
    fun getSelectedSelectable(): GamepadSelectable? =
        selectables.getOrNull(selectedSelectable)


    /**
     * Gets the line to display in telemetry.
     *
     * For example, you can run `chooser.getDisplayLines().forEach { telemetry.addLine(it) }`
     *
     */
    fun getDisplayLines(): List<String> =
        selectables.mapIndexed { index, selectable ->
            if (index == selectedSelectable) {
                "> ${selectable.name} : ${selectable.selected} <"
            } else {
                "  ${selectable.name} : ${selectable.selected}"
            }
        }

    private fun getSelectable(name: String): GamepadSelectable =
        selectableMap[name]
            ?: throw IllegalArgumentException("Unknown selectable: $name")

    private fun wrap(value: Int, size: Int): Int =
        Math.floorMod(value, size)

    class GamepadSelectable internal constructor(
        val name: String,
        val options: List<String>
    ) {
        var selectedIndex = 0
            private set

        val selected: String
            get() = options[selectedIndex]

        internal fun selectPrevious() {
            selectedIndex = Math.floorMod(
                selectedIndex - 1,
                options.size
            )
        }

        internal fun selectNext() {
            selectedIndex = Math.floorMod(
                selectedIndex + 1,
                options.size
            )
        }
    }
}
