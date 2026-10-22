/*
 * Copyright (c) 2026 NextFTC Team
 *
 *  Use of this source code is governed by an BSD-3-clause
 *  license that can be found in the LICENSE.md file at the root of this repository or at
 *  https://opensource.org/license/bsd-3-clause.
 */

package dev.nextftc.v2.pedro

import dev.nextftc.robot.opmode.OpModeHook

object PedroHook : OpModeHook {
    override fun beforePeriodic() {
        Pedro.follower?.update()
    }

    override fun afterEnd() {
        Pedro.follower = null
    }
}