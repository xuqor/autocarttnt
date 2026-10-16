package xuqor.autocart.client;

import java.util.concurrent.ThreadLocalRandom;

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
        INSTA_WAIT,
        INSTA_DRAW,
        INSTA_CHARGE,
        INSTA_RAIL,
        INSTA_CART,
        PRE_WAIT,
        PRE_RAIL,
        PRE_DRAW,
        PRE_CHARGE,
        PRE_CART,
        DONE
    }

    private State state = State.IDLE;
    private int timer = 0;
    private int cooldownLeft = 0;

    private BlockHitResult targetHit;
    private BlockPos railPos;
    private int railSlot = -1;
    private int cartSlot = -1;
    private int bowSlot = -1;
    private int originalSlot = 0;
    private float originalPitch = 0.0f;
    private float originalYaw = 0.0f;
    private float pitchTarget = 0.0f;
    private float yawTarget = 0.0f;
    private int plannedStart = 0;
    private int plannedCharge = 5;
    private int plannedRail = 1;
    private int plannedCart = 1;
    private int plannedRestore = 1;
    private boolean bowDown = false;

    public boolean isIdle() {
        return state == State.IDLE;
    }

    public boolean trigger(Minecraft mc) {
        if (state != State.IDLE) {
            return false;
        }
        if (cooldownLeft > 0) {
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

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int variance = cfg.tickVariance;
        float jitter = variance > 0 ? 1.5f : 0.0f;
        float yawWobble = variance > 0 ? 0.5f : 0.0f;

        targetHit = blockHit;
        railPos = placePos;
        originalSlot = inv.getSelectedSlot();
        originalPitch = mc.player.getXRot();
        originalYaw = mc.player.getYRot();
        pitchTarget = originalPitch - cfg.pitchOffset + (jitter > 0.0f ? rnd.nextFloat(-jitter, jitter) : 0.0f);
        yawTarget = originalYaw + (yawWobble > 0.0f ? rnd.nextFloat(-yawWobble, yawWobble) : 0.0f);
        plannedStart = variance > 0 ? rnd.nextInt(0, variance + 1) : 0;
        plannedCharge = clamp(cfg.chargeTicks + (variance > 0 ? rnd.nextInt(-variance, variance + 1) : 0), 3, 12);
        plannedRail = 1 + (variance > 0 ? rnd.nextInt(0, variance + 1) : 0);
        plannedCart = 1 + (variance > 0 ? rnd.nextInt(0, variance + 1) : 0);
        plannedRestore = 1 + (variance > 0 ? rnd.nextInt(0, variance + 1) : 0);
        timer = 0;
        bowDown = false;

        if (cfg.mode == ModConfig.Mode.PRE_RAIL) {
            state = State.PRE_WAIT;
        } else {
            state = State.INSTA_WAIT;
        }
        return true;
    }

    public void onTick(Minecraft mc) {
        if (cooldownLeft > 0) {
            cooldownLeft--;
        }
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
                case INSTA_WAIT -> {
                    if (timer > plannedStart) {
                        state = State.INSTA_DRAW;
                        timer = 0;
                    }
                }
                case INSTA_DRAW -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, bowSlot);
                    } else if (timer >= 2) {
                        mc.options.keyUse.setDown(true);
                        bowDown = true;
                        state = State.INSTA_CHARGE;
                        timer = 0;
                    }
                }
                case INSTA_CHARGE -> chargeTick(mc, State.INSTA_RAIL);
                case INSTA_RAIL -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, railSlot);
                    } else if (timer > plannedRail) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, targetHit);
                        swing(mc);
                        state = State.INSTA_CART;
                        timer = 0;
                    }
                }
                case INSTA_CART -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, cartSlot);
                    } else if (timer > plannedCart) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, cartHit());
                        swing(mc);
                        state = State.DONE;
                        timer = 0;
                    }
                }
                case PRE_WAIT -> {
                    if (timer > plannedStart) {
                        state = State.PRE_RAIL;
                        timer = 0;
                    }
                }
                case PRE_RAIL -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, railSlot);
                    } else if (timer > plannedRail) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, targetHit);
                        swing(mc);
                        state = State.PRE_DRAW;
                        timer = 0;
                    }
                }
                case PRE_DRAW -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, bowSlot);
                    } else if (timer >= 2) {
                        mc.options.keyUse.setDown(true);
                        bowDown = true;
                        state = State.PRE_CHARGE;
                        timer = 0;
                    }
                }
                case PRE_CHARGE -> chargeTick(mc, State.PRE_CART);
                case PRE_CART -> {
                    if (timer == 1) {
                        InventoryUtil.selectSlot(mc.player, cartSlot);
                    } else if (timer > plannedCart) {
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, cartHit());
                        swing(mc);
                        state = State.DONE;
                        timer = 0;
                    }
                }
                case DONE -> {
                    if (timer >= plannedRestore) {
                        InventoryUtil.selectSlot(mc.player, originalSlot);
                        int cooldown = ModConfig.get().cooldownTicks;
                        if (cooldown > 0) {
                            cooldownLeft = cooldown + ThreadLocalRandom.current().nextInt(0, ModConfig.get().tickVariance + 1);
                        }
                        reset();
                    }
                }
                default -> reset();
            }
        } catch (Exception e) {
            forceReset(mc);
        }
    }

    private void chargeTick(Minecraft mc, State next) {
        ModConfig cfg = ModConfig.get();
        if (cfg.smoothAim) {
            if (timer < plannedCharge - 1) {
                return;
            }
            if (timer <= plannedCharge) {
                glide(mc, pitchTarget, yawTarget, 0.5f);
                return;
            }
            if (timer == plannedCharge + 1) {
                mc.player.setXRot(pitchTarget);
                mc.player.setYRot(yawTarget);
                mc.options.keyUse.setDown(false);
                bowDown = false;
                return;
            }
            if (timer <= plannedCharge + 3) {
                glide(mc, originalPitch, originalYaw, 0.5f);
                return;
            }
            mc.player.setXRot(originalPitch);
            mc.player.setYRot(originalYaw);
            state = next;
            timer = 0;
        } else {
            if (timer == plannedCharge) {
                mc.player.setXRot(pitchTarget);
                mc.player.setYRot(yawTarget);
            } else if (timer == plannedCharge + 1) {
                mc.options.keyUse.setDown(false);
                bowDown = false;
            } else if (timer >= plannedCharge + 2) {
                mc.player.setXRot(originalPitch);
                mc.player.setYRot(originalYaw);
                state = next;
                timer = 0;
            }
        }
    }

    private void glide(Minecraft mc, float targetPitch, float targetYaw, float factor) {
        float curP = mc.player.getXRot();
        float curY = mc.player.getYRot();
        mc.player.setXRot(curP + (targetPitch - curP) * factor);
        mc.player.setYRot(curY + (targetYaw - curY) * factor);
    }

    private void swing(Minecraft mc) {
        try {
            if (ModConfig.get().swingHand && mc.player != null) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } catch (Exception ignored) {
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

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private void tell(Minecraft mc, String text) {
        try {
            mc.player.displayClientMessage(Component.literal("[AutoCart] " + text), false);
        } catch (Exception ignored) {
        }
    }
}
