package com.volleycraft.event;

import com.volleycraft.VolleyCraft;
import com.volleycraft.entity.VolleyballEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VolleyCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AttackEventHandler {

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof VolleyballEntity ball)) return;

        event.setCanceled(true);

        // Scale power by charge (0.3 min, 1.5 max) so fully-charged hits spike harder
        float power = Math.min(player.getAttackStrengthScale(0.5f) * 1.2f + 0.3f, 1.5f);
        ball.applyPlayerHit(player, power);
    }
}
