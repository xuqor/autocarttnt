package xuqor.autocart.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AutocartConfigScreen extends Screen {
    private final Screen parent;

    public AutocartConfigScreen(Screen parent) {
        super(Component.translatable("autocart.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 100;
        int y = 60;

        addRenderableWidget(Button.builder(modeText(), btn -> {
            ModConfig cfg = ModConfig.get();
            cfg.mode = cfg.mode == ModConfig.Mode.INSTA_CART ? ModConfig.Mode.PRE_RAIL : ModConfig.Mode.INSTA_CART;
            ModConfig.save();
            btn.setMessage(modeText());
        }).bounds(cx, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(chargeText(), btn -> {
            ModConfig cfg = ModConfig.get();
            cfg.chargeTicks++;
            if (cfg.chargeTicks > 10) {
                cfg.chargeTicks = 3;
            }
            ModConfig.save();
            btn.setMessage(chargeText());
        }).bounds(cx, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(pitchText(), btn -> {
            ModConfig cfg = ModConfig.get();
            cfg.pitchOffset += 1.0f;
            if (cfg.pitchOffset > 30.0f) {
                cfg.pitchOffset = 0.0f;
            }
            ModConfig.save();
            btn.setMessage(pitchText());
        }).bounds(cx, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(topFaceText(), btn -> {
            ModConfig cfg = ModConfig.get();
            cfg.onlyTopFace = !cfg.onlyTopFace;
            ModConfig.save();
            btn.setMessage(topFaceText());
        }).bounds(cx, y, 200, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("autocart.screen.done"), btn -> onClose())
                .bounds(cx, y, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("autocart.screen.hint"), this.width / 2, 45, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        ModConfig.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    private Component modeText() {
        boolean insta = ModConfig.get().mode == ModConfig.Mode.INSTA_CART;
        return Component.translatable(insta ? "autocart.screen.mode_insta" : "autocart.screen.mode_prerail");
    }

    private Component chargeText() {
        return Component.translatable("autocart.screen.charge", ModConfig.get().chargeTicks);
    }

    private Component pitchText() {
        return Component.translatable("autocart.screen.pitch", String.format("%.0f", ModConfig.get().pitchOffset));
    }

    private Component topFaceText() {
        return Component.translatable(ModConfig.get().onlyTopFace ? "autocart.screen.topface_on" : "autocart.screen.topface_off");
    }
}
