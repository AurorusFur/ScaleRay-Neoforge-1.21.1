package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.item.custom.PowerCellItem
import net.diavicecat.scaleray.menu.ScaleRayMenu
import net.diavicecat.scaleray.network.ScaleRayPayload.Mode
import net.diavicecat.scaleray.network.ScaleRayPayload.Target
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class ScaleRayMenuScreen(menu: ScaleRayMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<ScaleRayMenu>(menu, inv, title) {

    private val minPower = 0.01f
    private val maxPower = 0.3f

    // Controls panel and sidebar geometry (1x GUI pixels; textures are 2x)
    private val ctrlH    = 145  // scale_ray_inventory.png: 290/2
    private val sideW    = 26   // upgrade_slot_inventory.png: 52/2
    private val sideH    = 26
    private val sideRelY = (ctrlH - sideH) / 2  // = 59, centers sidebar in controls panel

    init {
        imageWidth  = 178
        imageHeight = 244  // ctrlH(145) + gap(3) + invH(96)
    }

    override fun init() {
        super.init()
        inventoryLabelX = ScaleRayMenu.INV_X
        inventoryLabelY = ScaleRayMenu.INV_Y - 12
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
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val lx = leftPos
        val ty = topPos
        val ps = guiGraphics.pose()

        // ── Controls panel background (355×290 at 2x → 178×145 at 1x) ──────────
        ps.pushPose()
        ps.translate(lx.toFloat(), ty.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(CONTROLS_BG, 0, 0, 0f, 0f, 355, 290, 355, 290)
        ps.popPose()

        // Labels (rendered at 1x after the panel background)
        guiGraphics.drawString(font, title,    lx + 8, ty + 8,  0x404040, false)
        guiGraphics.drawString(font, "Mode:",   lx + 8, ty + 24, 0x404040, false)
        guiGraphics.drawString(font, "Target:", lx + 8, ty + 68, 0x404040, false)

        // ── Upgrade/battery slot sidebar (52×52 at 2x → 26×26 at 1x) ────────────
        val sideX = lx + imageWidth
        val sideY = ty + sideRelY
        ps.pushPose()
        ps.translate(sideX.toFloat(), sideY.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(UPGRADE_SLOT_BG, 0, 0, 0f, 0f, 52, 52, 52, 52)
        ps.popPose()

        // Battery icon when slot is empty
        if (menu.cellSlot.item.isEmpty) {
            guiGraphics.blit(POWERCELL_SLOT_ICON,
                lx + ScaleRayMenu.CELL_SLOT_X, ty + ScaleRayMenu.CELL_SLOT_Y,
                0f, 0f, 16, 16, 16, 16)
        }

        // ── Inventory panel background (355×192 at 2x → 178×96 at 1x) ───────────
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

    companion object {
        private val CONTROLS_BG      = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/scale_ray_inventory.png")
        private val UPGRADE_SLOT_BG  = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/upgrade_slot_inventory.png")
        private val INVENTORY_BG     = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/player_inventory.png")
        private val POWERCELL_SLOT_ICON = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/powercell_slot.png")
    }
}
