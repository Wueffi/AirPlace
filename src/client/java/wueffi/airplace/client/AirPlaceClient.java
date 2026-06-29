package wueffi.airplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static wueffi.airplace.AirPlaceMain.LOGGER;
import static wueffi.airplace.client.AirPlaceConfig.active;


public class AirPlaceClient implements ClientModInitializer {
    private static KeyMapping toggleKey;
    public enum RenderMode { BLOCK, LINES}

    @Override
    public void onInitializeClient() {
        AirPlaceConfig.load();
        LOGGER.info("AirPlace Config Loaded!");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AirPlaceCommand.register(dispatcher);
        });

        LOGGER.info("AirPlace commands registered!");
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.airplace.toggle_airplace",
                InputConstants.KEY_F8,
                KeyMapping.Category.register(Identifier.parse("category.airplace"))
        ));
        LOGGER.info("AirPlace Keybinds registered!");

        UpdateHandler.initialize();
        LOGGER.info("Update Handler initialized!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                active = !active;
                AirPlaceConfig.setActive(active);
                AirPlaceConfig.save();
                if (active) {
                    assert client.level != null;
                    PlacementHandler.lastPlaceTick = client.level.getGameTime();
                }
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("AirPlace: " + (active ? "ON" : "OFF")));
                    LOGGER.info("AirPlace toggled {}", active ? "ON" : "OFF");
                }
            }
        });


        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(OutlineRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(PlacementHandler::tick);
    }
}