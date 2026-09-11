/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.robot

/**
 * Base class for all exceptions thrown by NextFTC.
 */
open class NextFTCException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when the [NextRobot] implementation in the user's project could not be found,
 * instantiated, or unambiguously resolved by [RobotScanner].
 */
class RobotScanException(message: String, cause: Throwable? = null) : NextFTCException(message, cause)
