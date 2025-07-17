package wueffi.airplace.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Properties;

public class AirPlaceConfig {
    private static final String FILE_NAME = "airplace.properties";
    private static final Properties props = new Properties();

    public static boolean active = false;
    public static AirPlaceClient.RenderMode renderMode = AirPlaceClient.RenderMode.BLOCK;
    public static float lineR = 0.0f, lineG = 0.8f, lineB = 1.0f;

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        File file = configDir.resolve(FILE_NAME).toFile();
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                active = Boolean.parseBoolean(props.getProperty("active", "false"));
                renderMode = AirPlaceClient.RenderMode.valueOf(props.getProperty("renderMode", "BLOCK"));
                lineR = Float.parseFloat(props.getProperty("lineR", "0.0"));
                lineG = Float.parseFloat(props.getProperty("lineG", "0.8"));
                lineB = Float.parseFloat(props.getProperty("lineB", "1.0"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        props.setProperty("active", String.valueOf(active));
        props.setProperty("renderMode", renderMode.name());
        props.setProperty("lineR", String.valueOf(lineR));
        props.setProperty("lineG", String.valueOf(lineG));
        props.setProperty("lineB", String.valueOf(lineB));

        Path configDir = FabricLoader.getInstance().getConfigDir();
        File file = configDir.resolve(FILE_NAME).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "AirPlace Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
        load();
    }

    public static void saveDefault() {
        props.setProperty("active", "false");
        props.setProperty("renderMode", "BLOCK");
        props.setProperty("lineR", "1.0");
        props.setProperty("lineG", "0.0");
        props.setProperty("lineB", "0.0");

        Path configDir = FabricLoader.getInstance().getConfigDir();
        File file = configDir.resolve(FILE_NAME).toFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "AirPlace Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
        load();
    }

    public static void setColor(float r, float g, float b) {
        lineR = r;
        lineG = g;
        lineB = b;
        save();
    }

    public static void setActive(boolean val) {
        active = val;
        save();
    }

    public static void setRenderMode(AirPlaceClient.RenderMode mode) {
        renderMode = mode;
        save();
    }

    public static boolean isActive() {
        return active;
    }

    public static AirPlaceClient.RenderMode getRenderMode() {
        return renderMode;
    }
}