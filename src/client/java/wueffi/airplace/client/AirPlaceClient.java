package wueffi.airplace.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import static wueffi.airplace.AirPlaceMain.LOGGER;
import static wueffi.airplace.client.AirPlaceConfig.active;


public class AirPlaceClient implements ClientModInitializer {
    private static KeyBinding toggleKey;
    public enum RenderMode { BLOCK, LINES}

    @Override
    public void onInitializeClient() {
        AirPlaceConfig.load();
        LOGGER.info("AirPlace Config Loaded!");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AirPlaceCommand.register(dispatcher);
        });

        LOGGER.info("AirPlace commands registered!");
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.airplace.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                "category.airplace"
        ));

        LOGGER.info("AirPlace Keybinds registered!");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                active = !active;
                AirPlaceConfig.setActive(active);
                AirPlaceConfig.save();
                if (active) {
                    assert client.world != null;
                    PlacementHandler.lastPlaceTick = client.world.getTime();
                }
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("AirPlace: " + (active ? "ON" : "OFF")), true);
                    LOGGER.info("AirPlace toggled {}", active ? "ON" : "OFF");
                }
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(OutlineRenderer::render);
        ClientTickEvents.END_CLIENT_TICK.register(PlacementHandler::tick);
    }
}