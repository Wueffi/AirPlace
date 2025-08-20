package wueffi.airplace.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;

import static wueffi.airplace.client.PlacementHandler.targetPos;

public class OutlineRenderer {
    public static void render(WorldRenderContext context) {
        if (!AirPlaceConfig.active) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (targetPos == null) return;
        if (!client.world.getBlockState(targetPos).isAir()) return;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        assert matrices != null;
        matrices.push();

        double x = targetPos.getX() - cameraPos.x;
        double y = targetPos.getY() - cameraPos.y;
        double z = targetPos.getZ() - cameraPos.z;

        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        assert interactionManager != null;
        if(interactionManager.getCurrentGameMode() == GameMode.SURVIVAL) {
            return;
        }

        if (AirPlaceConfig.renderMode == AirPlaceClient.RenderMode.LINES) {
            ItemStack stack = client.player.getMainHandStack();
            if (!(stack.getItem() instanceof BlockItem)) return;
            VertexConsumerProvider vertexConsumers = context.consumers();
            assert vertexConsumers != null;
            VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getLines());

            float r = AirPlaceConfig.lineR;
            float g = AirPlaceConfig.lineG;
            float b = AirPlaceConfig.lineB;
            float a = 0.8f;
            drawBoxEdges(matrices, consumer, x, y, z, x + 1, y + 1, z + 1, r, g, b, a);

            matrices.pop();
            return;
        }

        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        BlockState state = block.getDefaultState();

        matrices.translate(
                targetPos.getX() - cameraPos.x,
                targetPos.getY() - cameraPos.y,
                targetPos.getZ() - cameraPos.z
        );

        BlockRenderManager renderManager = client.getBlockRenderManager();
        BakedModel model = renderManager.getModel(state);

        VertexConsumerProvider vertexConsumers = context.consumers();
        BlockRenderView renderWorld = client.world;

        assert vertexConsumers != null;
        VertexConsumer translucentConsumer = vertexConsumers.getBuffer(RenderLayer.getTranslucent());
        int light = 0xFFFFFF;

        renderManager.getModelRenderer().render(
                renderWorld,
                model,
                state,
                BlockPos.ORIGIN,
                matrices,
                translucentConsumer,
                false,
                Random.create(),
                42L,
                light
        );
        matrices.pop();
    }

    private static void drawBoxEdges(MatrixStack matrices, VertexConsumer consumer,
                                     double x1, double y1, double z1, double x2, double y2, double z2,
                                     float r, float g, float b, float a) {
        MatrixStack.Entry entry = matrices.peek();

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

    private static void drawLine(VertexConsumer consumer, MatrixStack.Entry entry,
                                 double x1, double y1, double z1, double x2, double y2, double z2,
                                 float r, float g, float b, float a) {
        consumer.vertex(entry.getPositionMatrix(), (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a)
                .normal(0.0f, 1.0f, 0.0f);
        consumer.vertex(entry.getPositionMatrix(), (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a)
                .normal(0.0f, 1.0f, 0.0f);
    }
}