package com.volleycraft.client;

import com.volleycraft.entity.VolleyballEntity;
import com.volleycraft.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class VolleyballRenderer extends EntityRenderer<VolleyballEntity> {
    private final ItemRenderer itemRenderer;

    public VolleyballRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(VolleyballEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.1, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 3.0f));
        poseStack.scale(0.5f, 0.5f, 0.5f);
        itemRenderer.renderStatic(
            new ItemStack(ModItems.VOLLEYBALL.get()),
            ItemDisplayContext.GROUND,
            packedLight,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VolleyballEntity entity) {
        return new ResourceLocation("volleycraft", "textures/item/volleyball.png");
    }
}
