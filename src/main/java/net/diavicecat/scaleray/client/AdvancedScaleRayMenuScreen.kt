package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.menu.AdvancedScaleRayMenu
import net.diavicecat.scaleray.network.BeamColorPayload
import net.diavicecat.scaleray.network.ScaleRayPayload.Mode
import net.diavicecat.scaleray.network.ScaleRayPayload.Target
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.neoforged.neoforge.network.PacketDistributor

class AdvancedScaleRayMenuScreen(menu: AdvancedScaleRayMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<AdvancedScaleRayMenu>(menu, inv, title) {

    private val minPower = 0.01f
    private val maxPower = 10f
    private val ctrlH = 145
    private val sideW = 26
    private val sideH = 26
    private val sideRelY = (ctrlH - sideH) / 2

    private var colorIndex = 0

    init {
        imageWidth  = 178
        imageHeight = 244
    }

    override fun init() {
        super.init()
        inventoryLabelX = ScaleRayMenu_INV_X
        inventoryLabelY = ScaleRayMenu_INV_Y - 12

        val currentColor = menu.rayStack.get(ModDataComponents.BEAM_COLOR.get()) ?: BEAM_COLORS[0].first
        colorIndex = BEAM_COLORS.indexOfFirst { it.first == currentColor }.takeIf { it >= 0 } ?: 0

        val lx   = leftPos
        val ty   = topPos
        val btnW = imageWidth - 16

        addRenderableWidget(Button.builder(Component.literal(modeLabel(ScaleRayConfig.mode))) {
            ScaleRayConfig.mode = nextMode(ScaleRayConfig.mode)
            it.message = Component.literal(modeLabel(ScaleRayConfig.mode))
        }.bounds(lx + 8, ty + 36, btnW, 20).build())

        addRenderableWidget(Button.builder(Component.literal(targetLabel(ScaleRayConfig.target))) {
            ScaleRayConfig.target = nextTarget(ScaleRayConfig.target)
            it.message = Component.literal(targetLabel(ScaleRayConfig.target))
        }.bounds(lx + 8, ty + 80, btnW, 20).build())

        val sliderInitial = ((ScaleRayConfig.power - minPower) / (maxPower - minPower)).toDouble()
        addRenderableWidget(object : AbstractSliderButton(lx + 8, ty + 112, btnW, 20, Component.empty(), sliderInitial) {
            init { updateMessage() }
            override fun updateMessage() {
                val p = (value * (maxPower - minPower) + minPower).toFloat()
                setMessage(Component.literal("Power: ${"%.2f".format(p)}"))
            }
            override fun applyValue() {
                ScaleRayConfig.power = (value * (maxPower - minPower) + minPower).toFloat()
            }
        })

        val colorSlotX = lx + imageWidth
        val colorSlotY = ty + sideRelY + sideH
        addRenderableWidget(object : AbstractWidget(colorSlotX, colorSlotY, sideW, sideH, Component.literal("Beam Color")) {
            override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                val ps = guiGraphics.pose()
                ps.pushPose()
                ps.translate(x.toFloat(), y.toFloat(), 0f)
                ps.scale(0.5f, 0.5f, 1f)
                guiGraphics.blit(UPGRADE_SLOT_BG, 0, 0, 0f, 0f, 52, 52, 52, 52)
                ps.popPose()
                val argb = BEAM_COLORS[colorIndex].first or (0xFF shl 24)
                guiGraphics.fill(x + 5, y + 5, x + 21, y + 21, argb)
                if (isHovered) guiGraphics.fill(x + 5, y + 5, x + 21, y + 21, 0x33FFFFFF)
            }
            override fun updateWidgetNarration(output: NarrationElementOutput) {
                defaultButtonNarrationText(output)
            }
            override fun onClick(mouseX: Double, mouseY: Double) {
                colorIndex = (colorIndex + 1) % BEAM_COLORS.size
                PacketDistributor.sendToServer(BeamColorPayload(BEAM_COLORS[colorIndex].first))
            }
        })
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val lx = leftPos
        val ty = topPos
        val ps = guiGraphics.pose()

        ps.pushPose()
        ps.translate(lx.toFloat(), ty.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(CONTROLS_BG, 0, 0, 0f, 0f, 355, 290, 355, 290)
        ps.popPose()

        guiGraphics.drawString(font, title,    lx + 8, ty + 8,  0x404040, false)
        guiGraphics.drawString(font, "Mode:",   lx + 8, ty + 24, 0x404040, false)
        guiGraphics.drawString(font, "Target:", lx + 8, ty + 68, 0x404040, false)

        val sideX = lx + imageWidth
        val sideY = ty + sideRelY
        ps.pushPose()
        ps.translate(sideX.toFloat(), sideY.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(UPGRADE_SLOT_BG, 0, 0, 0f, 0f, 52, 52, 52, 52)
        ps.popPose()

        if (menu.cellSlot.item.isEmpty) {
            guiGraphics.blit(POWERCELL_SLOT_ICON,
                lx + CELL_SLOT_X, ty + CELL_SLOT_Y,
                0f, 0f, 16, 16, 16, 16)
        }

        val invPanelY = ty + ctrlH + 3
        ps.pushPose()
        ps.translate(lx.toFloat(), invPanelY.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(INVENTORY_BG, 0, 0, 0f, 0f, 355, 192, 355, 192)
        ps.popPose()
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        val f = getFocused()
        if (f != null && f.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val result = super.mouseReleased(mouseX, mouseY, button)
        if (button == 0) setFocused(null)
        return result
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun isPauseScreen() = false

    private fun modeLabel(mode: Mode) = when (mode) {
        Mode.SHRINK -> "Shrink"
        Mode.GROW   -> "Grow"
        Mode.RESET  -> "Reset"
    }
    private fun nextMode(mode: Mode): Mode {
        val v = Mode.entries; return v[(v.indexOf(mode) + 1) % v.size]
    }
    private fun targetLabel(target: Target) = when (target) {
        Target.OBSERVED -> "Observed"
        Target.SELF     -> "Self"
    }
    private fun nextTarget(target: Target): Target {
        val v = Target.entries; return v[(v.indexOf(target) + 1) % v.size]
    }

    companion object {
        private const val CELL_SLOT_X = 183
        private const val CELL_SLOT_Y = 64
        private const val ScaleRayMenu_INV_X = 8
        private const val ScaleRayMenu_INV_Y = 164

        private val CONTROLS_BG        = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/scale_ray_inventory.png")
        private val UPGRADE_SLOT_BG    = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/upgrade_slot_inventory.png")
        private val INVENTORY_BG       = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/player_inventory.png")
        private val POWERCELL_SLOT_ICON = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/powercell_slot.png")

        val BEAM_COLORS = listOf(
            0x00E63C to "Green",
            0xFF3030 to "Red",
            0x3080FF to "Blue",
            0xC040FF to "Purple",
            0x00D8D8 to "Cyan",
            0xFFCC00 to "Yellow",
            0xFFFFFF to "White",
            0xFF8800 to "Orange"
        )
    }
}
