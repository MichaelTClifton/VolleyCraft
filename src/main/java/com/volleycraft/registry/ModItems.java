package com.volleycraft.registry;

import com.volleycraft.VolleyCraft;
import com.volleycraft.item.VolleyballItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, VolleyCraft.MOD_ID);

    public static final RegistryObject<Item> VOLLEYBALL = ITEMS.register("volleyball",
        () -> new VolleyballItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> NET_POLE = ITEMS.register("net_pole",
        () -> new BlockItem(ModBlocks.NET_POLE.get(), new Item.Properties()));
}
