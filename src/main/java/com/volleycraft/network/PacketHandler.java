package com.volleycraft.network;

import com.volleycraft.VolleyCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(VolleyCraft.MOD_ID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, BallSyncPacket.class,
            BallSyncPacket::encode, BallSyncPacket::decode, BallSyncPacket::handle);
        CHANNEL.registerMessage(nextId++, GameStatePacket.class,
            GameStatePacket::encode, GameStatePacket::decode, GameStatePacket::handle);
        CHANNEL.registerMessage(nextId++, PlayerHitPacket.class,
            PlayerHitPacket::encode, PlayerHitPacket::decode, PlayerHitPacket::handle);
    }
}
