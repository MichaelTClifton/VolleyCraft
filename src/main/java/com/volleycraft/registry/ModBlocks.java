package com.volleycraft.registry;

import com.volleycraft.VolleyCraft;
import com.volleycraft.block.NetPoleBlock;
import com.volleycraft.block.NetSegmentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, VolleyCraft.MOD_ID);

    public static final RegistryObject<Block> NET_POLE = BLOCKS.register("net_pole",
        () -> new NetPoleBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f, 6.0f)
            .sound(SoundType.METAL)
            .noOcclusion()
            .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> NET_SEGMENT = BLOCKS.register("net_segment",
        () -> new NetSegmentBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL)
            .strength(0.1f)
            .sound(SoundType.WOOL)
            .noOcclusion()));
}
