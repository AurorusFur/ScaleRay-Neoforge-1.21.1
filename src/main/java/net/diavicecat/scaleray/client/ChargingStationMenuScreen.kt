package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.menu.ChargingStationMenu
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class ChargingStationMenuScreen(menu: ChargingStationMenu, inv: Inventory, title: Component) :
    AbstractContainerScreen<ChargingStationMenu>(menu, inv, title) {

    // Controls panel and inventory panel heights (1x GUI pixels; textures are 2x)
    private val ctrlH = 78  // 156 / 2

    init {
        imageWidth  = 178
        imageHeight = 177
    }

    override fun init() {
        super.init()
        inventoryLabelX = ChargingStationMenu.INV_X
        inventoryLabelY = ChargingStationMenu.INV_Y - 12
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val lx = leftPos
        val ty = topPos
        val ps = guiGraphics.pose()

        // ── Controls panel background ─────────────────────────────────────────
        // Textures are 2x GUI scale → render at 0.5× so items (always 16 px) fit correctly
        ps.pushPose()
        ps.translate(lx.toFloat(), ty.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)

        guiGraphics.blit(STATION_BG, 0, 0, 0f, 0f, 355, 156, 355, 156)

        // Power bar shell (7×84, 2x coords: x=16, y=36)
        guiGraphics.blit(EMPTY_POWER_BAR, 16, 54, 0f, 0f, 7, 84, 7, 84)

        // Power bar fill — clip from bottom (fill upward)
        val maxH = 80  // powerBarFiller.png height
        val fillH = if (menu.maxPower > 0)
            (menu.powerStored.toDouble() / menu.maxPower * maxH).toInt().coerceIn(0, maxH)
        else 0
        if (fillH > 0) {
            val vOff = (maxH - fillH).toFloat()
            guiGraphics.blit(POWER_BAR_FILLER, 18, 54 + 2 + maxH - fillH, 0f, vOff, 4, fillH, 4, maxH)
        }

        // Thunderbolt frame between slots (36×36, 2x coords: x=160, y=60)
        // Frame 0 is not rendered — inactive bolt is gray ≈ background, which bleeds through as outline
        val isActive = menu.powerStored > 0 && !menu.slots[0].item.isEmpty
        val frame = if (isActive && CHARGE_INTERVAL > 0)
            ((menu.chargeProgress.toLong() * 10 / CHARGE_INTERVAL).toInt() + 1).coerceIn(1, 10)
        else 0
        if (frame > 0) {
            guiGraphics.blit(THUNDERBOLT_FRAMES[frame], 169, 62, 0f, 0f, 15, 24, 15, 24)
        }

        ps.popPose()

        // ── Inventory panel background ────────────────────────────────────────
        val invPanelY = ty + ctrlH + 3
        ps.pushPose()
        ps.translate(lx.toFloat(), invPanelY.toFloat(), 0f)
        ps.scale(0.5f, 0.5f, 1f)
        guiGraphics.blit(PLAYER_INVENTORY_BG, 0, 0, 0f, 0f, 355, 192, 355, 192)
        ps.popPose()

        // Title label (outside the 0.5× scale block — 1x coords)
        guiGraphics.drawString(font, title, lx + 8, ty + 8, 0x404040, false)

        // Outline icons for empty slots (rendered at 1x positions, no scaling needed)
        if (menu.slots[0].item.isEmpty) {
            guiGraphics.blit(POWERCELL_SLOT_ICON,
                lx + ChargingStationMenu.CELL_SLOT_X, ty + ChargingStationMenu.CELL_SLOT_Y,
                0f, 0f, 16, 16, 16, 16)
        }
        if (menu.slots[1].item.isEmpty) {
            guiGraphics.blit(EMERALD_SLOT_ICON,
                lx + ChargingStationMenu.EMERALD_SLOT_X, ty + ChargingStationMenu.EMERALD_SLOT_Y,
                0f, 0f, 16, 16, 16, 16)
        }
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false)
    }

    override fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
        // Power bar tooltip (1x bounds: bar blit at 2x (16,54) → lx+8, ty+27; size 7×84 at 2x → 4×42 at 1x)
        val barX = leftPos + 8
        val barY = topPos + 27
        if (mouseX in barX..(barX + 4) && mouseY in barY..(barY + 42)) {
            guiGraphics.renderTooltip(font, Component.literal("${menu.powerStored} / ${menu.maxPower}"), mouseX, mouseY)
        }
    }

    override fun isPauseScreen() = false

    companion object {
        private const val CHARGE_INTERVAL = 200
        private val STATION_BG           = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/charging_station_inventory.png")
        private val PLAYER_INVENTORY_BG  = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/player_inventory.png")
        private val EMPTY_POWER_BAR      = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/empty_power_bar.png")
        private val POWER_BAR_FILLER     = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/power_bar_filler.png")
        private val POWERCELL_SLOT_ICON  = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/powercell_slot.png")
        private val EMERALD_SLOT_ICON    = ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/emerald_slot.png")
        private val THUNDERBOLT_FRAMES   = Array(11) { i ->
            ResourceLocation.fromNamespaceAndPath("scalerays", "textures/gui/thunderbolt_icon_$i.png")
        }
    }
}
