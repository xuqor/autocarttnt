package xuqor.autocart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CartController {
    private enum State {
        IDLE,
        INSTA_DRAW,
        INSTA_CHARGE,
        INSTA_RAIL,
        INSTA_CART,
        PRE_RAIL,
        PRE_DRAW,
        PRE_CHARGE,
        PRE_CART,
        DONE
    }

    private State state = State.IDLE;
    private int timer = 0;

    private BlockHitResult targetHit;
    private BlockPos railPos;
    private int railSlot = -1;
    private int cartSlot = -1;
    private int bowSlot = -1;
    private int originalSlot = 0;
    private float originalPitch = 0.0f;
    private float originalYaw = 0.0f;
    private boolean bowDown = false;

    public boolean isIdle() {
        return state == State.IDLE;
    }

    public boolean trigger(Minecraft mc) {
        if (state != State.IDLE) {
            return false;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return false;
        }
        if (mc.screen != null) {
            return false;
        }
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            tell(mc, "Смотри на блок!");
            return false;
        }

        ModConfig cfg = ModConfig.get();
        if (cfg.onlyTopFace && blockHit.getDirection() != Direction.UP) {
            tell(mc, "Целься в верхнюю грань блока!");
            return false;
        }

        Inventory inv = mc.player.getInventory();
        railSlot = InventoryUtil.findRail(inv);
        cartSlot = InventoryUtil.findTntMinecart(inv);
        bowSlot = InventoryUtil.findFlameBow(mc.player);
        boolean arrow = InventoryUtil.hasArrow(inv);

        if (railSlot == -1 || cartSlot == -1 || bowSlot == -1 || !arrow) {
            StringBuilder missing = new StringBuilder("Нет: ");
            if (railSlot == -1) {
                missing.append("рельсы ");
            }
            if (cartSlot == -1) {
                missing.append("ТНТ-вагонетки ");
            }
            if (bowSlot == -1) {
                missing.append("лука с Flame ");
            }
            if (!arrow) {
                missing.append("стрел");
            }
            tell(mc, missing.toString());
            return false;
        }

        BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!mc.level.getBlockState(placePos).isAir()) {
            tell(mc, "Нет места для рельсы!");
            return false;
        }

        targetHit = blockHit;
        railPos = placePos;
        originalSlot = inv.getSelectedSlot();
        originalPitch = mc.player.getXRot();
        originalYaw = mc.player.getYRot();
        timer = 0;
        bowDown = false;

        if (cfg.mode == ModConfig.Mode.PRE_RAIL) {
            state = State.PRE_RAIL;
        } else {
            state = State.INSTA_DRAW;
        }
        return true;
    }

    public void onTick(Minecraft mc) {
        if (state == State.IDLE) {
            return;
        }
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            forceReset(mc);
            return;
        }
        timer++;

        try {
            switch (state) {
                case INSTA_DRAW -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, bowSlot);
                        mc.options.keyUse.setDown(true);
                        bowDown = true;
                        state = State.INSTA_CHARGE;
                        timer = 0;
                    }
                }
                case INSTA_CHARGE -> {
                    int charge = ModConfig.get().chargeTicks;
                    if (timer == charge) {
                        mc.player.setXRot(originalPitch - ModConfig.get().pitchOffset);
                    } else if (timer == charge + 1) {
                        mc.options.keyUse.setDown(false);
                        bowDown = false;
                    } else if (timer >= charge + 2) {
                        mc.player.setXRot(originalPitch);
                        mc.player.setYRot(originalYaw);
                        state = State.INSTA_RAIL;
                        timer = 0;
                    }
                }
                case INSTA_RAIL -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, railSlot);
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, targetHit);
                        state = State.INSTA_CART;
                        timer = 0;
                    }
                }
                case INSTA_CART -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, cartSlot);
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, cartHit());
                        state = State.DONE;
                        timer = 0;
                    }
                }
                case PRE_RAIL -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, railSlot);
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, targetHit);
                        state = State.PRE_DRAW;
                        timer = 0;
                    }
                }
                case PRE_DRAW -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, bowSlot);
                        mc.options.keyUse.setDown(true);
                        bowDown = true;
                        state = State.PRE_CHARGE;
                        timer = 0;
                    }
                }
                case PRE_CHARGE -> {
                    int charge = ModConfig.get().chargeTicks;
                    if (timer == charge) {
                        mc.player.setXRot(originalPitch - ModConfig.get().pitchOffset);
                    } else if (timer == charge + 1) {
                        mc.options.keyUse.setDown(false);
                        bowDown = false;
                    } else if (timer >= charge + 2) {
                        mc.player.setXRot(originalPitch);
                        mc.player.setYRot(originalYaw);
                        state = State.PRE_CART;
                        timer = 0;
                    }
                }
                case PRE_CART -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, cartSlot);
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, cartHit());
                        state = State.DONE;
                        timer = 0;
                    }
                }
                case DONE -> {
                    if (timer >= 1) {
                        InventoryUtil.selectSlot(mc.player, originalSlot);
                        reset();
                    }
                }
                default -> reset();
            }
        } catch (Exception e) {
            forceReset(mc);
        }
    }

    public void forceReset(Minecraft mc) {
        try {
            if (mc != null && mc.options != null && bowDown) {
                mc.options.keyUse.setDown(false);
            }
            if (mc != null && mc.player != null) {
                mc.player.setXRot(originalPitch);
                mc.player.setYRot(originalYaw);
            }
        } catch (Exception ignored) {
        }
        bowDown = false;
        reset();
    }

    private void reset() {
        state = State.IDLE;
        timer = 0;
        targetHit = null;
        railPos = null;
        bowDown = false;
    }

    private BlockHitResult cartHit() {
        Vec3 center = Vec3.atCenterOf(railPos);
        return new BlockHitResult(center, Direction.UP, railPos, false);
    }

    private void tell(Minecraft mc, String text) {
        try {
            mc.player.displayClientMessage(Component.literal("[AutoCart] " + text), false);
        } catch (Exception ignored) {
        }
    }
}
