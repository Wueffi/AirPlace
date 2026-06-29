package wueffi.airplace.client;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.shapes.Shapes;

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

        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
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

            SubmitNodeCollector submitNodeCollector = context.submitNodeCollector();

            int color = ARGB.colorFromFloat(0.8f, AirPlaceConfig.lineR, AirPlaceConfig.lineG, AirPlaceConfig.lineB);

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            submitNodeCollector.submitShapeOutline(
                    poseStack,
                    Shapes.block(),
                    RenderTypes.lines(),
                    color,
                    2.5f,
                    false
            );

            poseStack.popPose();
            return;
        }

        Level renderWorld = client.level;

        ItemStack stack = client.player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        poseStack.translate(
                targetPos.getX() - cameraPos.x,
                targetPos.getY() - cameraPos.y,
                targetPos.getZ() - cameraPos.z
        );

        SubmitNodeCollector collector = context.submitNodeCollector();
        ModelManager modelManager = client.getModelManager();

        BlockStateModelSet modelSet = modelManager.getBlockStateModelSet();
        BlockStateModel model = modelSet.get(state);

        List<BlockStateModelPart> output = new ArrayList<>();
        model.collectParts(renderWorld.getRandom(), output);

        collector.submitBlockModel(
                poseStack,
                layer -> RenderTypes.translucentMovingBlock(),
                true,
                output,
                null,
                new int[0],
                0xF000F0,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );

        poseStack.popPose();
    }
}