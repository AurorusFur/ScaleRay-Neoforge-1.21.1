package net.diavicecat.scaleray.client

import net.diavicecat.scaleray.component.ModDataComponents
import net.diavicecat.scaleray.menu.AdvancedScaleRayMenu
import net.diavicecat.scaleray.network.BeamColorPayload
import net.diavicecat.scaleray.network.ScaleRayPayload.Mode
import net.diavicecat.scaleray.network.ScaleRayPayload.Target
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
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

    private var selectedColor = 0x00E63C
    private var showColorPicker = false

    // HSV state
    private var currentHue = 0f
    private var currentSat = 1f
    private var currentVal = 1f
    private var isDraggingSV = false
    private var isDraggingHue = false

    private lateinit var hexInput: EditBox

    // Picker dimensions (fixed)
    private val svSize = 96
    private val hueBarW = 12
    private val pad = 4
    private val hexRowH = 16
    private val swatchSize = 12
    private val swatchGap = 2
    private val paletteRowH = swatchSize
    private val pickerW = pad + svSize + pad + hueBarW + pad                          // 120
    private val pickerH = pad + svSize + pad + hexRowH + pad + paletteRowH + pad      // 138

    // Picker origin — set in init()
    private var pickerX = 0
    private var pickerY = 0

    init {
        imageWidth  = 178
        imageHeight = 244
    }

    override fun init() {
        super.init()
        inventoryLabelX = ScaleRayMenu_INV_X
        inventoryLabelY = ScaleRayMenu_INV_Y - 12

        ScaleRayConfig.loadPalette(Minecraft.getInstance().gameDirectory)
        selectedColor = menu.rayStack.get(ModDataComponents.BEAM_COLOR.get()) ?: 0x00E63C
        val (h, s, v) = rgbToHsv(selectedColor)
        currentHue = h; currentSat = s; currentVal = v

        val lx = leftPos
        val ty = topPos
        val btnW = imageWidth - 16

        // Position picker to the left of the sidebar, centered on the two slot buttons
        val slotsCenterY = ty + sideRelY + sideH  // center between battery slot top and color slot bottom
        pickerX = lx + imageWidth - pickerW - pad
        pickerY = (slotsCenterY - pickerH / 2).coerceIn(ty, ty + ctrlH - pickerH)

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

        // Hex input — registered as child only; rendered manually inside the picker overlay
        val hexRowY = pickerY + pad + svSize + pad
        hexInput = EditBox(font, pickerX + pad + 10, hexRowY, 72, hexRowH, Component.literal("Hex"))
        hexInput.setMaxLength(6)
        hexInput.value = colorToHex(selectedColor)
        hexInput.visible = false
        hexInput.setResponder { hex ->
            val parsed = parseHex(hex)
            if (parsed != null) {
                val (h2, s2, v2) = rgbToHsv(parsed)
                currentHue = h2; currentSat = s2; currentVal = v2
                selectedColor = parsed
            }
        }
        addWidget(hexInput)

        // Color button in sidebar (below battery slot)
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
                guiGraphics.fill(x + 5, y + 5, x + 21, y + 21, selectedColor or (0xFF shl 24))
                if (isHovered) guiGraphics.fill(x + 5, y + 5, x + 21, y + 21, 0x33FFFFFF)
            }
            override fun updateWidgetNarration(output: NarrationElementOutput) {
                defaultButtonNarrationText(output)
            }
            override fun onClick(mouseX: Double, mouseY: Double) {
                if (showColorPicker) closePicker() else openPicker()
            }
        })
    }

    // ── rendering ──────────────────────────────────────────────────────────────

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        if (showColorPicker) renderColorPicker(guiGraphics, mouseX, mouseY)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    private fun renderColorPicker(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        // Elevate Z so depth-buffer entries overwrite button text entries when MC flushes the font batch
        val ps = guiGraphics.pose()
        ps.pushPose()
        ps.translate(0f, 0f, 300f)

        val svX  = pickerX + pad
        val svY  = pickerY + pad
        val hueX = svX + svSize + pad
        val hueY = svY
        val hexRowY = svY + svSize + pad

        // Panel background + border (fully opaque so nothing bleeds through)
        guiGraphics.fill(pickerX, pickerY, pickerX + pickerW, pickerY + pickerH, 0xFF181818.toInt())
        guiGraphics.renderOutline(pickerX, pickerY, pickerW, pickerH, 0xFF888888.toInt())

        // SV square — one vertical gradient per column so both sat and val vary
        for (col in 0 until svSize) {
            val s = col.toFloat() / (svSize - 1).toFloat()
            val topColor = hsvToArgb(currentHue, s, 1f)
            guiGraphics.fillGradient(svX + col, svY, svX + col + 1, svY + svSize, topColor, 0xFF000000.toInt())
        }
        guiGraphics.renderOutline(svX - 1, svY - 1, svSize + 2, svSize + 2, 0xFF555555.toInt())

        // SV crosshair cursor
        val cx = svX + (currentSat * (svSize - 1)).toInt()
        val cy = svY + ((1f - currentVal) * (svSize - 1)).toInt()
        guiGraphics.fill(cx - 3, cy,     cx + 4, cy + 1, -1)
        guiGraphics.fill(cx,     cy - 3, cx + 1, cy + 4, -1)
        guiGraphics.fill(cx - 2, cy,     cx + 3, cy + 1, 0xFF000000.toInt())
        guiGraphics.fill(cx,     cy - 2, cx + 1, cy + 3, 0xFF000000.toInt())

        // Hue bar — one fill per row
        for (row in 0 until svSize) {
            val h = row.toFloat() / (svSize - 1).toFloat()
            guiGraphics.fill(hueX, hueY + row, hueX + hueBarW, hueY + row + 1, hsvToArgb(h, 1f, 1f))
        }
        guiGraphics.renderOutline(hueX - 1, hueY - 1, hueBarW + 2, svSize + 2, 0xFF555555.toInt())

        // Hue cursor: white bar with black outline
        val hcy = hueY + (currentHue * (svSize - 1)).toInt()
        guiGraphics.fill(hueX - 2, hcy - 1, hueX + hueBarW + 2, hcy + 2, -1)
        guiGraphics.fill(hueX - 1, hcy,     hueX + hueBarW + 1, hcy + 1, 0xFF222222.toInt())

        // Hex row: "#" label, EditBox (rendered manually), preview swatch
        guiGraphics.drawString(font, "#", pickerX + pad, hexRowY + 4, 0xAAAAAA, false)
        hexInput.render(guiGraphics, mouseX, mouseY, 0f)

        val previewX = hexInput.x + hexInput.width + pad
        guiGraphics.fill(previewX, hexRowY, previewX + hexRowH, hexRowY + hexRowH, selectedColor or (0xFF shl 24))
        guiGraphics.renderOutline(previewX - 1, hexRowY - 1, hexRowH + 2, hexRowH + 2, 0xFF666666.toInt())

        // Palette row: 8 swatches, left-click = load, right-click = save
        val paletteY = hexRowY + hexRowH + pad
        for (i in 0 until 8) {
            val sx = pickerX + pad + i * (swatchSize + swatchGap)
            val color = ScaleRayConfig.palette[i]
            guiGraphics.fill(sx, paletteY, sx + swatchSize, paletteY + swatchSize, color or (0xFF shl 24))
            val hovered = mouseX >= sx && mouseX < sx + swatchSize && mouseY >= paletteY && mouseY < paletteY + swatchSize
            if (hovered) guiGraphics.fill(sx, paletteY, sx + swatchSize, paletteY + swatchSize, 0x55FFFFFF)
            guiGraphics.renderOutline(sx - 1, paletteY - 1, swatchSize + 2, swatchSize + 2,
                if (color == selectedColor) 0xFFFFFFFF.toInt() else 0xFF555555.toInt())
        }

        ps.popPose()
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

    // ── input handling ─────────────────────────────────────────────────────────

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (showColorPicker) {
            val svX  = pickerX + pad
            val svY  = pickerY + pad
            val hueX = svX + svSize + pad
            val hueY = svY

            if (mouseX >= svX && mouseX < svX + svSize && mouseY >= svY && mouseY < svY + svSize) {
                isDraggingSV = true
                updateSV(mouseX.toFloat(), mouseY.toFloat())
                return true
            }
            if (mouseX >= hueX && mouseX < hueX + hueBarW && mouseY >= hueY && mouseY < hueY + svSize) {
                isDraggingHue = true
                updateHue(mouseY.toFloat())
                return true
            }
            if (hexInput.isMouseOver(mouseX, mouseY)) {
                hexInput.mouseClicked(mouseX, mouseY, button)
                setFocused(hexInput)
                return true
            }
            // Palette row
            val paletteY = pickerY + pad + svSize + pad + hexRowH + pad
            if (mouseY >= paletteY && mouseY < paletteY + swatchSize) {
                for (i in 0 until 8) {
                    val sx = pickerX + pad + i * (swatchSize + swatchGap)
                    if (mouseX >= sx && mouseX < sx + swatchSize) {
                        if (button == 1) {
                            // Right-click: save current color to this slot
                            ScaleRayConfig.palette[i] = selectedColor
                            ScaleRayConfig.savePalette(Minecraft.getInstance().gameDirectory)
                        } else {
                            // Left-click: load color from this slot
                            val loaded = ScaleRayConfig.palette[i]
                            val (h, s, v) = rgbToHsv(loaded)
                            currentHue = h; currentSat = s; currentVal = v
                            selectedColor = loaded
                            hexInput.value = colorToHex(selectedColor)
                        }
                        return true
                    }
                }
            }
            // Click outside panel → confirm and close
            if (mouseX < pickerX || mouseX > pickerX + pickerW || mouseY < pickerY || mouseY > pickerY + pickerH) {
                closePicker()
                return true
            }
            return true  // swallow clicks inside panel
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (isDraggingSV) { updateSV(mouseX.toFloat(), mouseY.toFloat()); return true }
        if (isDraggingHue) { updateHue(mouseY.toFloat()); return true }
        val f = getFocused()
        if (f != null && f.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isDraggingSV || isDraggingHue) {
            isDraggingSV = false
            isDraggingHue = false
            return true
        }
        val result = super.mouseReleased(mouseX, mouseY, button)
        if (button == 0 && !showColorPicker) setFocused(null)
        return result
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (showColorPicker) {
            if (hexInput.isFocused) {
                when (keyCode) {
                    257, 335 -> { closePicker(); return true }   // Enter / numpad Enter
                    256      -> { closePicker(); return true }   // Escape
                    else     -> if (hexInput.keyPressed(keyCode, scanCode, modifiers)) return true
                }
            } else if (keyCode == 256) {
                closePicker(); return true
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        if (showColorPicker && hexInput.isFocused) return hexInput.charTyped(codePoint, modifiers)
        return super.charTyped(codePoint, modifiers)
    }

    override fun isPauseScreen() = false

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun updateSV(mouseX: Float, mouseY: Float) {
        val svX = pickerX + pad
        val svY = pickerY + pad
        currentSat = ((mouseX - svX) / (svSize - 1).toFloat()).coerceIn(0f, 1f)
        currentVal = 1f - ((mouseY - svY) / (svSize - 1).toFloat()).coerceIn(0f, 1f)
        selectedColor = hsvToArgb(currentHue, currentSat, currentVal) and 0xFFFFFF
        hexInput.value = colorToHex(selectedColor)
    }

    private fun updateHue(mouseY: Float) {
        val hueY = pickerY + pad
        currentHue = ((mouseY - hueY) / (svSize - 1).toFloat()).coerceIn(0f, 1f)
        selectedColor = hsvToArgb(currentHue, currentSat, currentVal) and 0xFFFFFF
        hexInput.value = colorToHex(selectedColor)
    }

    private fun openPicker() {
        showColorPicker = true
        hexInput.visible = true
        hexInput.value = colorToHex(selectedColor)
    }

    private fun closePicker() {
        PacketDistributor.sendToServer(BeamColorPayload(selectedColor))
        showColorPicker = false
        hexInput.visible = false
        hexInput.setFocused(false)
    }

    private fun hsvToArgb(h: Float, s: Float, v: Float): Int {
        if (s == 0f) {
            val c = (v * 255).toInt()
            return (0xFF shl 24) or (c shl 16) or (c shl 8) or c
        }
        val sector = h * 6f
        val i = sector.toInt() % 6
        val f = sector - i.toFloat()
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))
        val (r, g, b) = when (i) {
            0 -> Triple(v, t, p); 1 -> Triple(q, v, p); 2 -> Triple(p, v, t)
            3 -> Triple(p, q, v); 4 -> Triple(t, p, v); else -> Triple(v, p, q)
        }
        return (0xFF shl 24) or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    private fun rgbToHsv(rgb: Int): Triple<Float, Float, Float> {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8)  and 0xFF) / 255f
        val b = (rgb and 0xFF)           / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val v = max
        val s = if (max == 0f) 0f else delta / max
        val h = when {
            delta == 0f -> 0f
            max == r    -> ((g - b) / delta + (if (g < b) 6f else 0f)) / 6f
            max == g    -> ((b - r) / delta + 2f) / 6f
            else        -> ((r - g) / delta + 4f) / 6f
        }
        return Triple(h, s, v)
    }

    private fun colorToHex(color: Int) = "%06X".format(color and 0xFFFFFF)

    private fun parseHex(hex: String): Int? {
        val clean = hex.trimStart('#').trim()
        return if (clean.length == 6) clean.toLongOrNull(16)?.toInt()?.and(0xFFFFFF) else null
    }

    private fun modeLabel(mode: Mode) = when (mode) {
        Mode.SHRINK -> "Shrink"; Mode.GROW -> "Grow"; Mode.RESET -> "Reset"
    }
    private fun nextMode(mode: Mode): Mode {
        val v = Mode.entries; return v[(v.indexOf(mode) + 1) % v.size]
    }
    private fun targetLabel(target: Target) = when (target) {
        Target.OBSERVED -> "Observed"; Target.SELF -> "Self"
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
            0x00E63C to "Green", 0xFF3030 to "Red",    0x3080FF to "Blue",   0xC040FF to "Purple",
            0x00D8D8 to "Cyan",  0xFFCC00 to "Yellow", 0xFFFFFF to "White",  0xFF8800 to "Orange"
        )
    }
}
