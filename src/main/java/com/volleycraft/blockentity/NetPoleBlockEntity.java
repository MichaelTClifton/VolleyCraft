package com.volleycraft.blockentity;

import com.volleycraft.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class NetPoleBlockEntity extends BlockEntity {
    @Nullable
    private UUID courtId = null;
    @Nullable
    private BlockPos partnerPos = null;
    private boolean isPrimary = false;

    public NetPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.NET_POLE.get(), pos, state);
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState state, NetPoleBlockEntity be) {
        // Tick is reserved for future animations or state updates
    }

    public void setCourtData(UUID courtId, BlockPos partnerPos, boolean isPrimary) {
        this.courtId = courtId;
        this.partnerPos = partnerPos;
        this.isPrimary = isPrimary;
        setChanged();
    }

    public void clearCourtData() {
        this.courtId = null;
        this.partnerPos = null;
        this.isPrimary = false;
        setChanged();
    }

    public boolean isPartOfCourt() {
        return courtId != null;
    }

    @Nullable
    public UUID getCourtId() {
        return courtId;
    }

    @Nullable
    public BlockPos getPartnerPos() {
        return partnerPos;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (courtId != null) {
            tag.putUUID("CourtId", courtId);
        }
        if (partnerPos != null) {
            tag.putInt("PartnerX", partnerPos.getX());
            tag.putInt("PartnerY", partnerPos.getY());
            tag.putInt("PartnerZ", partnerPos.getZ());
        }
        tag.putBoolean("IsPrimary", isPrimary);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("CourtId")) {
            courtId = tag.getUUID("CourtId");
        }
        if (tag.contains("PartnerX")) {
            partnerPos = new BlockPos(
                tag.getInt("PartnerX"),
                tag.getInt("PartnerY"),
                tag.getInt("PartnerZ")
            );
        }
        isPrimary = tag.getBoolean("IsPrimary");
    }
}
