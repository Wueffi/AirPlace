package wueffi.airplace.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class AirPlaceCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("airplace")
                .then(ClientCommandManager.literal("on")
                        .executes(ctx -> {
                            AirPlaceConfig.setActive(true);
                            ctx.getSource().sendFeedback(Text.literal("AirPlace: ON"));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("off")
                        .executes(ctx -> {
                            AirPlaceConfig.setActive(false);
                            ctx.getSource().sendFeedback(Text.literal("AirPlace: OFF"));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("toggle")
                        .executes(ctx -> {
                            boolean newState = !AirPlaceConfig.active;
                            AirPlaceConfig.setActive(newState);
                            ctx.getSource().sendFeedback(Text.literal("AirPlace: " + (newState ? "ON" : "OFF")));
                            return 1;
                        }))
                .then(ClientCommandManager.literal("renderMode")
                        .then(ClientCommandManager.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> {
                                    b.suggest("lines");
                                    b.suggest("block");
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    String mode = StringArgumentType.getString(ctx, "mode");
                                    try {
                                        AirPlaceClient.RenderMode rm =
                                                mode.equalsIgnoreCase("lines") ? AirPlaceClient.RenderMode.LINES : AirPlaceClient.RenderMode.BLOCK;
                                        AirPlaceConfig.setRenderMode(rm);
                                        ctx.getSource().sendFeedback(Text.literal("AirPlace: Render mode set to " + mode));
                                    } catch (Exception e) {
                                        ctx.getSource().sendFeedback(Text.literal("AirPlace: Invalid render mode! Valid: lines, block"));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(ClientCommandManager.literal("config")
                    .then(ClientCommandManager.literal("setcolor")
                            .then(ClientCommandManager.argument("rgb", StringArgumentType.string())
                                    .executes(ctx -> {
                                        String rgb = StringArgumentType.getString(ctx, "rgb");
                                        String[] parts = rgb.split(" ");
                                        if (parts.length != 3) {
                                            ctx.getSource().sendFeedback(Text.literal("AirPlace: Usage /airplace setcolor \"<R> <G> <B>\" (0-255)"));
                                            return 0;
                                        }
                                        try {
                                            float r = (float) Integer.parseInt(parts[0]) / 255;
                                            float g = (float) Integer.parseInt(parts[1]) / 255;
                                            float b = (float) Integer.parseInt(parts[2]) / 255;
                                            AirPlaceConfig.setColor(r, g, b);
                                            ctx.getSource().sendFeedback(Text.literal(
                                                    String.format("AirPlace: Set line color to R=%.2f G=%.2f B=%.2f", r, g, b)));
                                        } catch (Exception e) {
                                            ctx.getSource().sendFeedback(Text.literal("AirPlace: Invalid numbers!"));
                                            return 0;
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("reload")
                            .executes(ctx -> {
                                try {
                                    AirPlaceConfig.load();
                                    ctx.getSource().sendFeedback(Text.literal(String.format("AirPlace: Config Reloaded!")));
                                } catch (Exception e) {
                                    ctx.getSource().sendFeedback(Text.literal("AirPlace: Failed to reload Config!"));
                                    return 0;
                                }
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("reset")
                            .executes(ctx -> {
                                try {
                                    AirPlaceConfig.saveDefault();
                                    AirPlaceConfig.load();
                                    ctx.getSource().sendFeedback(Text.literal(String.format("AirPlace: Config reset!")));
                                } catch (Exception e) {
                                    ctx.getSource().sendFeedback(Text.literal("AirPlace: Failed to reset Config!"));
                                    return 0;
                                }
                                return 1;
                            })
                    )
                )
        );
    }
}