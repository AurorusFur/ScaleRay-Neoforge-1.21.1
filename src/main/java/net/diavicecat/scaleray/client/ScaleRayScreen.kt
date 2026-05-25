package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.network.ScaleRayPayload.Mode
import net.diavicecat.scaleray.network.ScaleRayPayload.Target
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class ScaleRayScreen : Screen(Component.literal("Scale Ray")) {

    private val panelW = 220
    private val panelH = 160

    // Power range: 0.05 – 2.0
    private val minPower = 0.05f
    private val maxPower = 2.0f

    override fun init() {
        val lx = (width  - panelW) / 2
        val ty = (height - panelH) / 2

        // --- Mode cycling button ---
        addRenderableWidget(Button.builder(Component.literal(modeLabel(ScaleRayConfig.mode))) {
            ScaleRayConfig.mode = nextMode(ScaleRayConfig.mode)
            it.message = Component.literal(modeLabel(ScaleRayConfig.mode))
        }.bounds(lx + 8, ty + 36, panelW - 16, 20).build())

        // --- Target cycling button ---
        addRenderableWidget(Button.builder(Component.literal(targetLabel(ScaleRayConfig.target))) {
            ScaleRayConfig.target = nextTarget(ScaleRayConfig.target)
            it.message = Component.literal(targetLabel(ScaleRayConfig.target))
        }.bounds(lx + 8, ty + 80, panelW - 16, 20).build())

        // --- Power slider ---
        val sliderInitial = ((ScaleRayConfig.power - minPower) / (maxPower - minPower)).toDouble()
        addRenderableWidget(object : AbstractSliderButton(lx + 8, ty + 124, panelW - 16, 20,
            Component.empty(), sliderInitial) {
            init { updateMessage() }
            override fun updateMessage() {
                val p = (value * (maxPower - minPower) + minPower).toFloat()
                setMessage(Component.literal("Power: ${"%.2f".format(p)}"))
            }
            override fun applyValue() {
                ScaleRayConfig.power = (value * (maxPower - minPower) + minPower).toFloat()
            }
        })
    }

    private fun modeLabel(mode: Mode) = when (mode) {
        Mode.SHRINK -> "Shrink"
        Mode.GROW   -> "Grow"
        Mode.RESET  -> "Reset"
    }

    private fun nextMode(mode: Mode): Mode {
        val values = Mode.entries
        return values[(values.indexOf(mode) + 1) % values.size]
    }

    private fun targetLabel(target: Target) = when (target) {
        Target.OBSERVED -> "Observed"
        Target.SELF     -> "Self"
    }

    private fun nextTarget(target: Target): Target {
        val values = Target.entries
        return values[(values.indexOf(target) + 1) % values.size]
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Intentionally empty — we invoke super.renderBackground() directly in render()
        // so it runs exactly once, before our custom draws, not again inside super.render().
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)

        val lx = (width  - panelW) / 2
        val ty = (height - panelH) / 2

        // Outer dark border
        guiGraphics.fill(lx - 1, ty - 1, lx + panelW + 1, ty + panelH + 1, 0xFF373737.toInt())
        // Gray panel fill
        guiGraphics.fill(lx, ty, lx + panelW, ty + panelH, 0xFFC6C6C6.toInt())
        // Bevel — bright top/left edge
        guiGraphics.fill(lx,           ty,            lx + panelW - 1, ty + 1,        0xFFFFFFFF.toInt())
        guiGraphics.fill(lx,           ty,            lx + 1,          ty + panelH - 1, 0xFFFFFFFF.toInt())
        // Bevel — dark bottom/right edge
        guiGraphics.fill(lx + 1,      ty + panelH - 1, lx + panelW, ty + panelH, 0xFF555555.toInt())
        guiGraphics.fill(lx + panelW - 1, ty + 1,      lx + panelW, ty + panelH, 0xFF555555.toInt())

        // Title
        guiGraphics.drawCenteredString(font, title, width / 2, ty + 8, 0xFFFFFF)

        // Section labels
        guiGraphics.drawString(font, "Mode:",   lx + 8, ty + 24, 0x404040, false)
        guiGraphics.drawString(font, "Target:", lx + 8, ty + 68, 0x404040, false)

        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (minecraft?.options?.keyInventory?.matches(keyCode, scanCode) == true) {
            onClose()
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun isPauseScreen() = false
}
