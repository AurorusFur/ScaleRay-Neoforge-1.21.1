package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.network.ScaleRayPayload

object ScaleRayConfig {
    var mode: ScaleRayPayload.Mode     = ScaleRayPayload.Mode.SHRINK
    var target: ScaleRayPayload.Target = ScaleRayPayload.Target.OBSERVED
    var power: Float                   = 0.1f
}
