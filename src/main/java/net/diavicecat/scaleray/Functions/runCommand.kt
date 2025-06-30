package net.diavicecat.scaleray.Functions

import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer

class runCommand(server1: MinecraftServer?, createCommandSourceStack: CommandSourceStack, string: String) {
    fun runCommand(server: MinecraftServer, source: CommandSourceStack, command: String) {
        server.commands.performPrefixedCommand(source, command)
    }
}