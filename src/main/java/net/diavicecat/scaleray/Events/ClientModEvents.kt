package net.diavicecat.scaleray.Events

import net.diavicecat.scaleray.ScaleRay
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

@EventBusSubscriber(modid = ScaleRay.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClientModEvents {
    @JvmStatic
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        // ...
    }

}
