package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.network.ScaleRayPayload
import java.io.File

object ScaleRayConfig {
    var mode: ScaleRayPayload.Mode     = ScaleRayPayload.Mode.SHRINK
    var target: ScaleRayPayload.Target = ScaleRayPayload.Target.OBSERVED
    var power: Float                   = 0.1f

    val palette = intArrayOf(
        0x00E63C, 0xFF3030, 0x3080FF, 0xC040FF,
        0x00D8D8, 0xFFCC00, 0xFFFFFF, 0xFF8800
    )

    fun loadPalette(gameDir: File) {
        val file = File(gameDir, "config/scalerays_palette.txt")
        if (!file.exists()) return
        file.readLines().forEachIndexed { i, line ->
            if (i < 8) line.trim().toLongOrNull(16)?.toInt()?.and(0xFFFFFF)?.let { palette[i] = it }
        }
    }

    fun savePalette(gameDir: File) {
        val file = File(gameDir, "config/scalerays_palette.txt")
        file.parentFile?.mkdirs()
        file.writeText(palette.joinToString("\n") { "%06X".format(it) })
    }
}
