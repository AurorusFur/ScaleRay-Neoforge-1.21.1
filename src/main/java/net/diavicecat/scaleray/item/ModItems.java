package net.diavicecat.scaleray.item;

import net.diavicecat.scaleray.ScaleRay;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ScaleRay.MOD_ID);

    public static final DeferredItem<Item> SCALERAY = ITEMS.register("scaleray",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SCALINGCORE = ITEMS.register("scalingcore",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> SCALETECHCASING = ITEMS.register("scaletechcasing",
            () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
