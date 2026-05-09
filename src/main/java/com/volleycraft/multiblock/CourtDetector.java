package com.volleycraft.multiblock;

import com.volleycraft.blockentity.NetPoleBlockEntity;
import com.volleycraft.game.VolleyballGameManager;
import com.volleycraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.UUID;

public class CourtDetector {
    private static final int MIN_DIST = 3;
    private static final int MAX_DIST = 14;
    private static final int NET_HEIGHT = 4;

    public static void tryFormCourt(Level level, BlockPos newPolePos) {
        BlockState poleState = level.getBlockState(newPolePos);
        if (!poleState.is(ModBlocks.NET_POLE.get())) return;

        Direction poleFacing = poleState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        // Search perpendicular to the pole's facing (along the net axis)
        Direction searchDir = poleFacing.getClockWise();

        BlockPos partner = findPartner(level, newPolePos, searchDir, poleFacing);
        if (partner == null) partner = findPartner(level, newPolePos, searchDir.getOpposite(), poleFacing);
        if (partner == null) return;

        NetPoleBlockEntity newBe = getBlockEntity(level, newPolePos);
        NetPoleBlockEntity partnerBe = getBlockEntity(level, partner);
        if (newBe == null || partnerBe == null) return;
        if (newBe.isPartOfCourt() || partnerBe.isPartOfCourt()) return;

        UUID courtId = VolleyballGameManager.getInstance().createCourt(newPolePos, partner);
        newBe.setCourtData(courtId, partner, true);
        partnerBe.setCourtData(courtId, newPolePos, false);

        placeNet(level, newPolePos, partner, poleFacing);

        level.players().forEach(p -> p.sendSystemMessage(
            Component.literal("[VolleyCraft] Court formed! Right-click a net pole to join the game.")));
    }

    private static BlockPos findPartner(Level level, BlockPos start, Direction dir, Direction facing) {
        for (int i = 1; i <= MAX_DIST; i++) {
            BlockPos check = start.relative(dir, i);
            BlockState state = level.getBlockState(check);
            if (state.is(ModBlocks.NET_POLE.get())) {
                Direction f = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                if ((f == facing || f == facing.getOpposite()) && i >= MIN_DIST) return check;
                return null;
            }
            if (!state.isAir() && !state.is(ModBlocks.NET_SEGMENT.get())) return null;
        }
        return null;
    }

    public static void dismantleCourt(Level level, BlockPos removedPole) {
        VolleyballGameManager.getInstance().removeCourt(removedPole);
        // Remove all net segments near the removed pole
        BlockPos.betweenClosed(
            removedPole.offset(-MAX_DIST, 0, -MAX_DIST),
            removedPole.offset(MAX_DIST, NET_HEIGHT, MAX_DIST)
        ).forEach(p -> {
            if (level.getBlockState(p).is(ModBlocks.NET_SEGMENT.get())) {
                level.removeBlock(new BlockPos(p), false);
            }
        });
    }

    private static void placeNet(Level level, BlockPos pole1, BlockPos pole2, Direction facing) {
        BlockState netState = ModBlocks.NET_SEGMENT.get().defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing);

        int baseY = Math.max(pole1.getY(), pole2.getY());
        int x1 = pole1.getX(), z1 = pole1.getZ();
        int x2 = pole2.getX(), z2 = pole2.getZ();

        if (x1 == x2) {
            // Poles aligned on X — net runs along Z
            int minZ = Math.min(z1, z2) + 1;
            int maxZ = Math.max(z1, z2) - 1;
            for (int z = minZ; z <= maxZ; z++) {
                for (int dy = 1; dy <= NET_HEIGHT; dy++) {
                    BlockPos np = new BlockPos(x1, baseY + dy, z);
                    if (level.getBlockState(np).isAir()) level.setBlock(np, netState, 3);
                }
            }
        } else {
            // Poles aligned on Z — net runs along X
            int minX = Math.min(x1, x2) + 1;
            int maxX = Math.max(x1, x2) - 1;
            for (int x = minX; x <= maxX; x++) {
                for (int dy = 1; dy <= NET_HEIGHT; dy++) {
                    BlockPos np = new BlockPos(x, baseY + dy, z1);
                    if (level.getBlockState(np).isAir()) level.setBlock(np, netState, 3);
                }
            }
        }
    }

    private static NetPoleBlockEntity getBlockEntity(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof NetPoleBlockEntity be ? be : null;
    }
}
