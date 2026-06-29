package wueffi.airplace.client;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

import static wueffi.airplace.client.PlacementHandler.targetPos;

public class OutlineRenderer {
    public static void render(LevelRenderContext context) {
        if (!AirPlaceConfig.active) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (targetPos == null) return;
        if (!client.level.getBlockState(targetPos).isAir()) return;

        Vec3 cameraPos = context.gameRenderer().getMainCamera().position();
        PoseStack poseStack = context.poseStack();

        poseStack.pushPose();

        double x = targetPos.getX() - cameraPos.x;
        double y = targetPos.getY() - cameraPos.y;
        double z = targetPos.getZ() - cameraPos.z;

        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        assert gameMode != null;
        if (gameMode.getPlayerMode() == GameType.SURVIVAL) {
            return;
        }

        if (AirPlaceConfig.renderMode == AirPlaceClient.RenderMode.LINES) {
            ItemStack stack = client.player.getMainHandItem();
            if (!(stack.getItem() instanceof BlockItem)) return;
            MultiBufferSource bufferSource = context.bufferSource();
            
            VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.LINES);

            float r = AirPlaceConfig.lineR;
            float g = AirPlaceConfig.lineG;
            float b = AirPlaceConfig.lineB;
            float a = 0.8f;
            drawBoxEdges(poseStack, consumer, x, y, z, x + 1, y + 1, z + 1, r, g, b, a);

            poseStack.popPose();
            return;
        }

        ItemStack stack = client.player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        poseStack.translate(
                targetPos.getX() - cameraPos.x,
                targetPos.getY() - cameraPos.y,
                targetPos.getZ() - cameraPos.z
        );

        ModelManager modelManager = client.getModelManager();
        BlockStateModelSet modelSet = modelManager.getBlockStateModelSet();

        BlockStateModel model = modelSet.get(state);

        MultiBufferSource bufferSource = context.bufferSource();
        Level renderWorld = client.level;

        PoseStack.Pose entry = poseStack.last();

        List<BlockStateModelPart> output = new ArrayList<>();
        model.collectParts(renderWorld.getRandom(), output);

        QuadInstance instance = new QuadInstance();
        instance.setColor(ARGB.color(255, 255, 255, 255));
        instance.setLightCoords(0xF000F0);
        instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);

        instance.scaleColor(0.6f);

        VertexConsumer translucentConsumer = getTranslucentConsumer(bufferSource, model);

        for (Direction dir : Direction.values()) {
            for (BlockStateModelPart part : output) {
                for (BakedQuad quad : part.getQuads(dir)) {
                    translucentConsumer.putBakedQuad(entry, quad, instance);
                }
            }
        }

        for (BlockStateModelPart part : output) {
            for (BakedQuad quad : part.getQuads(null)) {
                translucentConsumer.putBakedQuad(entry, quad, instance);
            }
        }

        poseStack.popPose();
    }

    private static void drawBoxEdges(PoseStack poseStack, VertexConsumer consumer,
                                     double x1, double y1, double z1, double x2, double y2, double z2,
                                     float r, float g, float b, float a) {
        PoseStack.Pose entry = poseStack.last();

        drawLine(consumer, entry, x1, y1, z1, x2, y1, z1, r, g, b, a);
        drawLine(consumer, entry, x2, y1, z1, x2, y1, z2, r, g, b, a);
        drawLine(consumer, entry, x2, y1, z2, x1, y1, z2, r, g, b, a);
        drawLine(consumer, entry, x1, y1, z2, x1, y1, z1, r, g, b, a);

        drawLine(consumer, entry, x1, y2, z1, x2, y2, z1, r, g, b, a);
        drawLine(consumer, entry, x2, y2, z1, x2, y2, z2, r, g, b, a);
        drawLine(consumer, entry, x2, y2, z2, x1, y2, z2, r, g, b, a);
        drawLine(consumer, entry, x1, y2, z2, x1, y2, z1, r, g, b, a);

        drawLine(consumer, entry, x1, y1, z1, x1, y2, z1, r, g, b, a);
        drawLine(consumer, entry, x2, y1, z1, x2, y2, z1, r, g, b, a);
        drawLine(consumer, entry, x2, y1, z2, x2, y2, z2, r, g, b, a);
        drawLine(consumer, entry, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void drawLine(VertexConsumer consumer, PoseStack.Pose entry,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        consumer.addVertex(entry.pose(), (float)x1, (float)y1, (float)z1)
                .setColor(r, g, b, a)
                .setNormal(entry, 0.0f, 1.0f, 0.0f)
                .setLineWidth(2.5f);
        consumer.addVertex(entry.pose(), (float)x2, (float)y2, (float)z2)
                .setColor(r, g, b, a)
                .setNormal(entry, 0.0f, 1.0f, 0.0f)
                .setLineWidth(2.5f);
    }

    private static VertexConsumer getTranslucentConsumer(MultiBufferSource bufferSource, BlockStateModel model) {
        return bufferSource.getBuffer(RenderTypes.entityTranslucent(model.particleMaterial().sprite().atlasLocation()));
    }
}