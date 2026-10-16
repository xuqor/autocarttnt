package xuqor.autocart.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutocartClient implements ClientModInitializer {
    private static KeyMapping cartKey;
    private static KeyMapping displaceKey;
    private static final CartController CONTROLLER = new CartController();
    private static final KnockbackDisplacer DISPLACER = new KnockbackDisplacer();

    public static CartController controller() {
        return CONTROLLER;
    }

    public static KnockbackDisplacer displacer() {
        return DISPLACER;
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

        displaceKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.autocart.displace",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_Z,
                        category));

        AttackEntityCallback.EVENT.register(DISPLACER::onAttack);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (cartKey.consumeClick()) {
                CONTROLLER.trigger(client);
            }
            while (displaceKey.consumeClick()) {
                DISPLACER.toggle(client);
            }
            CONTROLLER.onTick(client);
            DISPLACER.onTick(client);
        });
    }
}
