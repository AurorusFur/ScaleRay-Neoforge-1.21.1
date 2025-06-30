package net.diavicecat.scaleray.item.custom
import net.diavicecat.scaleray.Events.Keybinds
import net.diavicecat.scaleray.Functions.runCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.level.Level
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent


class ScaleRayItem(properties: Properties) : Item(properties) {



    override fun use(
        level: Level,
        player: Player,
        hand: InteractionHand
    ): InteractionResultHolder<ItemStack> {
if (Keybinds.ShrinkReset.isDown) {
    if (!level.isClientSide) {
        val server = (level as ServerLevel).server
        val source = player.createCommandSourceStack()
        server.commands.performPrefixedCommand(source, "scale set pehkui:base 1 @s")
    }
}
        if (!Keybinds.ShrinkReset.isDown) {

            if (Keybinds.ShrinkMode.isDown) {
                if (!level.isClientSide) {
                    val server = (level as ServerLevel).server
                    val source = player.createCommandSourceStack()
                    server.commands.performPrefixedCommand(source, "scale add pehkui:base 0.1 @s")
                }
            }
            if (!Keybinds.ShrinkMode.isDown) {
                if (!level.isClientSide) {
                    val server = (level as ServerLevel).server
                    val source = player.createCommandSourceStack()
                    server.commands.performPrefixedCommand(source, "scale add pehkui:base -0.1 @s")
                }
            }
        }
        return InteractionResultHolder.success(player.getItemInHand(hand))
    }

}
