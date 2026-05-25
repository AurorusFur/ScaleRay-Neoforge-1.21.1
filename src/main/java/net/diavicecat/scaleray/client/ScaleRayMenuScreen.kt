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
import net.minecraft.world.item.ItemStack

class ScaleRayMenuScreen(menu: ScaleRayMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<ScaleRayMenu>(menu, inv, title) {

    private val minPower = 0.05f
    private val maxPower = 2.0f

    // Controls panel height and sidebar geometry — must match constants in ScaleRayMenu
    private val ctrlH = 156
    private val sideW = 32
    private val sideH = 52
    private val sideRelY = (ctrlH - sideH) / 2 - 20  // = 32, shifted up from center

    init {
        imageWidth = 220
        imageHeight = 258
    }

    override fun init() {
        super.init()
        inventoryLabelX = ScaleRayMenu.INV_X
        inventoryLabelY = ScaleRayMenu.INV_Y - 12
        val lx = leftPos
        val ty = topPos
        val btnW = imageWidth - 16  // full-width buttons

        addRenderableWidget(Button.builder(Component.literal(modeLabel(ScaleRayConfig.mode))) {
            ScaleRayConfig.mode = nextMode(ScaleRayConfig.mode)
            it.message = Component.literal(modeLabel(ScaleRayConfig.mode))
        }.bounds(lx + 8, ty + 36, btnW, 20).build())

        addRenderableWidget(Button.builder(Component.literal(targetLabel(ScaleRayConfig.target))) {
            ScaleRayConfig.target = nextTarget(ScaleRayConfig.target)
            it.message = Component.literal(targetLabel(ScaleRayConfig.target))
        }.bounds(lx + 8, ty + 80, btnW, 20).build())

        val sliderInitial = ((ScaleRayConfig.power - minPower) / (maxPower - minPower)).toDouble()
        addRenderableWidget(object : AbstractSliderButton(lx + 8, ty + 124, btnW, 20, Component.empty(), sliderInitial) {
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

        // ── Controls panel ─────────────────────────────────────────────────
        guiGraphics.fill(lx - 1,             ty - 1,          lx + imageWidth + 1, ty + ctrlH + 1,  0xFF373737.toInt())
        guiGraphics.fill(lx,                  ty,              lx + imageWidth,      ty + ctrlH,      0xFFC6C6C6.toInt())
        guiGraphics.fill(lx,                  ty,              lx + imageWidth - 1,  ty + 1,          0xFFFFFFFF.toInt())
        guiGraphics.fill(lx,                  ty,              lx + 1,               ty + ctrlH - 1, 0xFFFFFFFF.toInt())
        guiGraphics.fill(lx + 1,              ty + ctrlH - 1, lx + imageWidth,      ty + ctrlH,      0xFF555555.toInt())
        guiGraphics.fill(lx + imageWidth - 1, ty + 1,          lx + imageWidth,      ty + ctrlH,      0xFF555555.toInt())

        // Title (white — renderLabels is overridden to skip redrawing it)
        guiGraphics.drawCenteredString(font, title, lx + imageWidth / 2, ty + 8, 0xFFFFFF)
        guiGraphics.drawString(font, "Mode:",   lx + 8, ty + 24, 0x404040, false)
        guiGraphics.drawString(font, "Target:", lx + 8, ty + 68, 0x404040, false)

        // ── Cell sidebar (docked to right of controls panel, right corners rounded) ──
        val sideX = lx + imageWidth
        val sideY = ty + sideRelY
        // Fill: 4-step staircase at right corners
        guiGraphics.fill(sideX,         sideY,       sideX+sideW-2, sideY+sideH,   0xFFC6C6C6.toInt()) // main body
        guiGraphics.fill(sideX+sideW-2, sideY+1,     sideX+sideW-1, sideY+sideH-1, 0xFFC6C6C6.toInt()) // col sw-2
        guiGraphics.fill(sideX+sideW-1, sideY+2,     sideX+sideW,   sideY+sideH-2, 0xFFC6C6C6.toInt()) // col sw-1
        // Border: 4-step staircase corner
        guiGraphics.fill(sideX,         sideY-1,     sideX+sideW-3, sideY,         0xFF373737.toInt()) // top border
        guiGraphics.fill(sideX+sideW,   sideY+3,     sideX+sideW+1, sideY+sideH-3, 0xFF373737.toInt()) // right border
        guiGraphics.fill(sideX,         sideY+sideH, sideX+sideW-3, sideY+sideH+1, 0xFF373737.toInt()) // bottom border
        // Bevels
        guiGraphics.fill(sideX,         sideY,       sideX+sideW-2, sideY+1,       0xFFFFFFFF.toInt()) // top bevel
        guiGraphics.fill(sideX+sideW-1, sideY+2,     sideX+sideW,   sideY+sideH-2, 0xFF555555.toInt()) // right bevel
        guiGraphics.fill(sideX+1,       sideY+sideH-1, sideX+sideW-2, sideY+sideH, 0xFF555555.toInt()) // bottom bevel

        // Rounded slot background (slot bg is 1px outside item position)
        val slotBgX = lx + ScaleRayMenu.CELL_SLOT_X - 1  // = sideX + 6
        val slotBgY = ty + ScaleRayMenu.CELL_SLOT_Y - 1  // = sideY + 13
        drawRoundSlot(guiGraphics, slotBgX, slotBgY)

        // Outline icon when cell slot is empty
        if (menu.cellSlot.item.isEmpty) {
            guiGraphics.blit(POWERCELL_SLOT_ICON,
                lx + ScaleRayMenu.CELL_SLOT_X, ty + ScaleRayMenu.CELL_SLOT_Y,
                0f, 0f, 16, 16, 16, 16)
        }

        // Charge counter below slot
        val cell = menu.cellSlot.item
        if (!cell.isEmpty) {
            val charges = cell.get(ModDataComponents.POWER_CHARGES.get()) ?: 0
            val chargeColor = if (charges > 0) 0x404040 else 0xFF5555
            guiGraphics.drawCenteredString(font, "$charges/${PowerCellItem.MAX_CHARGES}",
                sideX + sideW / 2, sideY + sideH - 14, chargeColor)
        }

        // ── Inventory panel ────────────────────────────────────────────────
        val invPanelY = ty + ctrlH + 3
        val invPanelH = imageHeight - ctrlH - 3
        guiGraphics.fill(lx - 1,             invPanelY - 1,            lx + imageWidth + 1, invPanelY + invPanelH + 1, 0xFF373737.toInt())
        guiGraphics.fill(lx,                  invPanelY,                lx + imageWidth,      invPanelY + invPanelH,     0xFFC6C6C6.toInt())
        guiGraphics.fill(lx,                  invPanelY,                lx + imageWidth - 1,  invPanelY + 1,             0xFFFFFFFF.toInt())
        guiGraphics.fill(lx,                  invPanelY,                lx + 1,               invPanelY + invPanelH - 1, 0xFFFFFFFF.toInt())
        guiGraphics.fill(lx + 1,              invPanelY + invPanelH - 1,lx + imageWidth,      invPanelY + invPanelH,     0xFF555555.toInt())
        guiGraphics.fill(lx + imageWidth - 1, invPanelY + 1,            lx + imageWidth,      invPanelY + invPanelH,     0xFF555555.toInt())

        // ── Player inventory slots — blit vanilla inventory.png for native visuals ──
        // inventory.png is 256×256; player inventory rows start at UV(7,83), hotbar at UV(7,141)
        // blitting 162×76 captures 3 rows (54px) + 4px gap + hotbar (18px)
        guiGraphics.blit(INVENTORY_BG, lx + ScaleRayMenu.INV_X - 1, ty + ScaleRayMenu.INV_Y - 1, 7, 83, 162, 76)
    }

    private fun drawRoundSlot(guiGraphics: GuiGraphics, x: Int, y: Int) {
        val dark  = 0xFF373737.toInt()
        val inner = 0xFF8B8B8B.toInt()
        guiGraphics.fill(x + 1, y,      x + 17, y + 1,  dark)
        guiGraphics.fill(x,     y + 1,  x + 1,  y + 17, dark)
        guiGraphics.fill(x + 1, y + 17, x + 17, y + 18, dark)
        guiGraphics.fill(x + 17,y + 1,  x + 18, y + 17, dark)
        guiGraphics.fill(x + 1, y + 1,  x + 17, y + 17, inner)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Title is drawn white in renderBg; only draw the inventory section label here
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        // No-op — prevent double blur; super.renderBackground() called once in render()
    }

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
        private val INVENTORY_BG = ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png")
        private val POWERCELL_SLOT_ICON = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/powercell_slot.png")
    }
}
