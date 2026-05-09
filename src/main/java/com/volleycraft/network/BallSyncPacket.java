package com.volleycraft.network;

import com.volleycraft.entity.VolleyballEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: authoritative ball position + velocity every 3 ticks.
 * Client smoothly interpolates toward this position to hide latency.
 */
public class BallSyncPacket {
    private final int entityId;
    private final double x, y, z;
    private final double vx, vy, vz;

    public BallSyncPacket(int entityId, double x, double y, double z,
                          double vx, double vy, double vz) {
        this.entityId = entityId;
        this.x = x; this.y = y; this.z = z;
        this.vx = vx; this.vy = vy; this.vz = vz;
    }

    public static void encode(BallSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        buf.writeDouble(pkt.x);
        buf.writeDouble(pkt.y);
        buf.writeDouble(pkt.z);
        buf.writeDouble(pkt.vx);
        buf.writeDouble(pkt.vy);
        buf.writeDouble(pkt.vz);
    }

    public static BallSyncPacket decode(FriendlyByteBuf buf) {
        return new BallSyncPacket(
            buf.readInt(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readDouble(), buf.readDouble(), buf.readDouble()
        );
    }

    public static void handle(BallSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(pkt.entityId);
            if (entity instanceof VolleyballEntity ball) {
                ball.lerpTo(pkt.x, pkt.y, pkt.z, pkt.vx, pkt.vy, pkt.vz);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
