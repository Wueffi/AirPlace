package wueffi.airplace.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class PlacementHandler {

    private static final KeyMapping placeKey = Minecraft.getInstance().options.keyUse;
    public static BlockPos targetPos = new BlockPos(0,0,0);
    public static long lastPlaceTick = -20;

    public static void tick(Minecraft client) {
        if (!AirPlaceConfig.active || client == null || client.player == null || client.level == null) return;

        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        assert gameMode != null;
        if (gameMode.getPlayerMode() == GameType.SURVIVAL) {
            return;
        }

        long currentTick = client.level.getGameTime();

        Vec3 eyePos = client.player.getEyePosition(1.0F);
        Vec3 lookVec = client.player.getViewVector(1.0F);
        Vec3 end = eyePos.add(lookVec.scale(5.0D));
        BlockHitResult hitResult = client.level.clip(
                new ClipContext(
                        eyePos, end,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        client.player
                )
        );

        Direction face;

        if (hitResult.getType() == BlockHitResult.Type.BLOCK) {
            face = hitResult.getDirection();
            targetPos = hitResult.getBlockPos().relative(face);
        } else {
            targetPos = BlockPos.containing(end);
            face = Direction.getNearest((int)lookVec.x, (int)lookVec.y, (int)lookVec.z, Direction.NORTH);
        }


        if (placeKey.consumeClick()  && currentTick > lastPlaceTick + AirPlaceConfig.getSpeed()) {
            lastPlaceTick = client.level.getGameTime();
            if (!client.level.getBlockState(targetPos).isAir()) return;

            ItemStack stack = client.player.getMainHandItem();
            if (!(stack.getItem() instanceof BlockItem)) return;

            BlockHitResult placeHit = new BlockHitResult(
                    Vec3.atCenterOf(targetPos),
                    face,
                    targetPos,
                    false
            );

            gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, placeHit);
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}