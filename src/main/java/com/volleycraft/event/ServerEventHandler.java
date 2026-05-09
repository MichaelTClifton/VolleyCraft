package com.volleycraft.event;

import com.volleycraft.VolleyCraft;
import com.volleycraft.game.VolleyballGameManager;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VolleyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        VolleyballGameManager.getInstance().reset();
        VolleyCraft.LOGGER.info("VolleyCraft: GameManager reset on server start.");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        VolleyballGameManager.getInstance().reset();
        VolleyCraft.LOGGER.info("VolleyCraft: GameManager reset on server stop.");
    }
}
