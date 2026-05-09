package com.volleycraft.registry;

import com.volleycraft.VolleyCraft;
import com.volleycraft.blockentity.NetPoleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, VolleyCraft.MOD_ID);

    public static final RegistryObject<BlockEntityType<NetPoleBlockEntity>> NET_POLE =
        BLOCK_ENTITY_TYPES.register("net_pole",
            () -> BlockEntityType.Builder.of(NetPoleBlockEntity::new,
                ModBlocks.NET_POLE.get()).build(null));
}
