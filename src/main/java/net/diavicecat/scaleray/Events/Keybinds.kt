package net.diavicecat.scaleray.Events
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

object Keybinds {
    val ShrinkMode = KeyMapping(
        "key.scalerays.shrinkmode",
        GLFW.GLFW_KEY_LEFT_SHIFT,
        "key.categories.misc"
    )
    val ShrinkReset = KeyMapping(
        "key.scalerays.shrinkmode",
        GLFW.GLFW_KEY_LEFT_CONTROL,
        "key.categories.misc"
    )
}