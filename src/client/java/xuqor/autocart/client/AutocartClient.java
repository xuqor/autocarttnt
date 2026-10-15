package xuqor.autocart.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutocartClient implements ClientModInitializer {
    private static KeyMapping cartKey;
    private static final CartController CONTROLLER = new CartController();

    public static CartController controller() {
        return CONTROLLER;
    }

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("autocart", "main"));

        cartKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.autocart.cart",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_C,
                        category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (cartKey.consumeClick()) {
                CONTROLLER.trigger(client);
            }
            CONTROLLER.onTick(client);
        });
    }
}
