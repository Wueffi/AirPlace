package wueffi.airplace.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static wueffi.airplace.AirPlaceMain.LOGGER;

/**
 * Compatibility handler for MrCrayfish's Controllable mod.
 * Uses reflection and MethodHandles to avoid a hard compile-time / runtime dependency.
 */
public class ControllableCompat {

    private static boolean initialized = false;
    private static boolean available = false;

    private static Object useItemBinding;
    private static MethodHandle isButtonDownHandle;

    private static MethodHandle getControllerHandle;
    private static MethodHandle isButtonPressedHandle;
    private static MethodHandle getLTriggerValueHandle;
    private static int leftTriggerButtonIndex = 11; // Controllable default for Buttons.LEFT_TRIGGER

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        if (!FabricLoader.getInstance().isModLoaded("controllable")) {
            return;
        }

        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            // 1. Resolve ButtonBindings.USE_ITEM and ButtonBinding#isButtonDown
            try {
                Class<?> buttonBindingsClass = Class.forName("com.mrcrayfish.controllable.client.binding.ButtonBindings");
                Field useItemField = buttonBindingsClass.getField("USE_ITEM");
                useItemBinding = useItemField.get(null);

                Class<?> buttonBindingClass = Class.forName("com.mrcrayfish.controllable.client.binding.ButtonBinding");
                Method isButtonDownMethod = buttonBindingClass.getMethod("isButtonDown");
                isButtonDownHandle = lookup.unreflect(isButtonDownMethod);
            } catch (Throwable t) {
                LOGGER.debug("[AirPlace] Could not bind Controllable ButtonBindings.USE_ITEM: {}", t.getMessage());
            }

            // 2. Resolve Controllable.getController()
            try {
                Class<?> controllableClass = Class.forName("com.mrcrayfish.controllable.Controllable");
                Method getControllerMethod = controllableClass.getMethod("getController");
                getControllerHandle = lookup.unreflect(getControllerMethod);
            } catch (Throwable t) {
                LOGGER.debug("[AirPlace] Could not bind Controllable.getController: {}", t.getMessage());
            }

            // 3. Resolve Controller#isButtonPressed(int) and Controller#getLTriggerValue()
            try {
                Class<?> controllerClass = Class.forName("com.mrcrayfish.controllable.client.input.Controller");
                Method isButtonPressedMethod = controllerClass.getMethod("isButtonPressed", int.class);
                isButtonPressedHandle = lookup.unreflect(isButtonPressedMethod);

                try {
                    Method getLTriggerValueMethod = controllerClass.getMethod("getLTriggerValue");
                    getLTriggerValueHandle = lookup.unreflect(getLTriggerValueMethod);
                } catch (NoSuchMethodException ignored) {
                }
            } catch (Throwable t) {
                LOGGER.debug("[AirPlace] Could not bind Controllable Controller methods: {}", t.getMessage());
            }

            // 4. Resolve Buttons.LEFT_TRIGGER
            try {
                Class<?> buttonsClass = Class.forName("com.mrcrayfish.controllable.client.input.Buttons");
                Field leftTriggerField = buttonsClass.getField("LEFT_TRIGGER");
                leftTriggerButtonIndex = leftTriggerField.getInt(null);
            } catch (Throwable ignored) {
            }

            available = (useItemBinding != null && isButtonDownHandle != null) || (getControllerHandle != null && isButtonPressedHandle != null);
            if (available) {
                LOGGER.info("[AirPlace] Controllable controller compatibility successfully initialized!");
            }
        } catch (Throwable t) {
            LOGGER.warn("[AirPlace] Failed to initialize Controllable compatibility: {}", t.getMessage());
            available = false;
        }
    }

    /**
     * Checks whether the Controllable mod's LT button or USE_ITEM binding is currently held down.
     *
     * @return true if LT trigger or USE_ITEM action is pressed
     */
    public static boolean isPlacePressed() {
        if (!initialized) {
            init();
        }
        if (!available) {
            return false;
        }

        try {
            // Check 1: ButtonBindings.USE_ITEM.isButtonDown()
            if (useItemBinding != null && isButtonDownHandle != null) {
                boolean isDown = (boolean) isButtonDownHandle.invoke(useItemBinding);
                if (isDown) {
                    return true;
                }
            }

            // Check 2: Raw Controller LT button / analog trigger
            if (getControllerHandle != null) {
                Object controller = getControllerHandle.invoke();
                if (controller != null) {
                    if (isButtonPressedHandle != null) {
                        boolean pressed = (boolean) isButtonPressedHandle.invoke(controller, leftTriggerButtonIndex);
                        if (pressed) {
                            return true;
                        }
                    }
                    if (getLTriggerValueHandle != null) {
                        float triggerValue = (float) getLTriggerValueHandle.invoke(controller);
                        if (triggerValue > 0.5f) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }
}
