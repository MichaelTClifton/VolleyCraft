package com.volleycraft.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Server -> Client: current score and game phase, shown as action bar text. */
public class GameStatePacket {
    private final UUID courtId;
    private final int score0;
    private final int score1;
    private final int phase;
    private final int servingTeam;

    public GameStatePacket(UUID courtId, int score0, int score1, int phase, int servingTeam) {
        this.courtId = courtId;
        this.score0 = score0;
        this.score1 = score1;
        this.phase = phase;
        this.servingTeam = servingTeam;
    }

    public static void encode(GameStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.courtId);
        buf.writeInt(pkt.score0);
        buf.writeInt(pkt.score1);
        buf.writeInt(pkt.phase);
        buf.writeInt(pkt.servingTeam);
    }

    public static GameStatePacket decode(FriendlyByteBuf buf) {
        return new GameStatePacket(
            buf.readUUID(),
            buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()
        );
    }

    public static void handle(GameStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.player.displayClientMessage(
                Component.literal("§e" + pkt.score0 + " §f- §e" + pkt.score1 +
                    "  §7| §fTeam " + (pkt.servingTeam + 1) + " serves"),
                true
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
