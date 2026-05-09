package com.volleycraft.registry;

import com.volleycraft.VolleyCraft;
import com.volleycraft.entity.VolleyballEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, VolleyCraft.MOD_ID);

    public static final RegistryObject<EntityType<VolleyballEntity>> VOLLEYBALL =
        ENTITY_TYPES.register("volleyball",
            () -> EntityType.Builder.<VolleyballEntity>of(VolleyballEntity::new, MobCategory.MISC)
                .sized(0.4f, 0.4f)
                .clientTrackingRange(64)
                .updateInterval(3)
                .build("volleyball"));
}
