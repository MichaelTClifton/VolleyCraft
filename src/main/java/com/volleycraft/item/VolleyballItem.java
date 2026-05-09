package com.volleycraft.item;

import com.volleycraft.entity.VolleyballEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VolleyballItem extends Item {

    public VolleyballItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            VolleyballEntity ball = new VolleyballEntity(level,
                player.getX(), player.getEyeY() - 0.1, player.getZ());

            // Throw upward and slightly forward
            double power = 0.7;
            ball.setDeltaMovement(
                player.getLookAngle().x * power,
                0.5 + player.getLookAngle().y * 0.3,
                player.getLookAngle().z * power
            );
            level.addFreshEntity(ball);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
