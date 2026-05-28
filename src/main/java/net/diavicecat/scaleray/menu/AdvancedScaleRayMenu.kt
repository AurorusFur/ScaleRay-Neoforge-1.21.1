package net.diavicecat.scaleray.menu

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class AdvancedScaleRayMenu(containerId: Int, playerInv: Inventory, rayStack: ItemStack) :
    ScaleRayMenu(containerId, playerInv, rayStack, ModMenuTypes.ADVANCED_SCALE_RAY_MENU.get())
