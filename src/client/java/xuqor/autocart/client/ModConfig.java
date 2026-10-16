package xuqor.autocart.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import xuqor.autocart.Autocart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    public enum Mode {
        INSTA_CART,
        PRE_RAIL
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;

    public Mode mode = Mode.INSTA_CART;
    public int chargeTicks = 5;
    public float pitchOffset = 12.0f;
    public boolean onlyTopFace = true;
    // Легитность: разброс таймингов, плавный прицел, взмах рукой, пауза между запусками.
    public int tickVariance = 1;
    public boolean smoothAim = true;
    public boolean swingHand = true;
    public int cooldownTicks = 6;

    public static synchronized ModConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static synchronized void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("autocart.json");
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                instance = GSON.fromJson(json, ModConfig.class);
                if (instance == null) {
                    instance = new ModConfig();
                }
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                Autocart.LOGGER.warn("Failed to load autocart config, using defaults", e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
        }
        instance.clamp();
    }

    public static synchronized void save() {
        if (instance == null) {
            return;
        }
        instance.clamp();
        Path file = FabricLoader.getInstance().getConfigDir().resolve("autocart.json");
        try {
            Files.writeString(file, GSON.toJson(instance));
        } catch (IOException e) {
            Autocart.LOGGER.warn("Failed to save autocart config", e);
        }
    }

    private void clamp() {
        if (mode == null) {
            mode = Mode.INSTA_CART;
        }
        if (chargeTicks < 3) {
            chargeTicks = 3;
        }
        if (chargeTicks > 10) {
            chargeTicks = 10;
        }
        if (pitchOffset < 0.0f) {
            pitchOffset = 0.0f;
        }
        if (pitchOffset > 30.0f) {
            pitchOffset = 30.0f;
        }
        if (tickVariance < 0) {
            tickVariance = 0;
        }
        if (tickVariance > 2) {
            tickVariance = 2;
        }
        if (cooldownTicks < 0) {
            cooldownTicks = 0;
        }
        if (cooldownTicks > 20) {
            cooldownTicks = 20;
        }
    }
}
