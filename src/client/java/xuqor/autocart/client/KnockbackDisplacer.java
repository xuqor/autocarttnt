package xuqor.autocart.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Knockback displacement (техника marlowww).
 * При спринт-ударе / оружии с Knockback сервер берет направление отброса
 * из look-пакета атакующего, а не из взаимного положения игроков.
 * Поэтому флик головой (yaw +/- angle) в тот же тик, что и удар,
 * кидает жертву вбок. Сторону мод выбирает сам, жать ничего не надо.
 */
public class KnockbackDisplacer {
    private int cooldownLeft = 0;
    private boolean restorePending = false;
    private float restoreYaw = 0.0f;
    private boolean alternateLeft = true;

    public InteractionResult onAttack(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.kbEnabled) {
            return InteractionResult.PASS;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != player) {
            return InteractionResult.PASS;
        }
        if (!(entity instanceof LivingEntity)) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (cooldownLeft > 0) {
            return InteractionResult.PASS;
        }
        // Условие самой механики: нужен спринт-хит или Knockback на предмете.
        boolean sprinting = player.isSprinting();
        boolean kbWeapon = hasKnockback(player.getMainHandItem(), player);
        if (!sprinting && !kbWeapon) {
            return InteractionResult.PASS;
        }
        // Сторона отброса: мод решает сам, ничего жать не надо.
        // LEFT — всегда влево, RIGHT — всегда вправо, ALTERNATE — по очереди,
        // RANDOM — случайно, STRAFE — по зажатым A/D.
        float side;
        switch (cfg.kbDirection) {
            case RIGHT -> side = 1.0f;
            case ALTERNATE -> {
                side = alternateLeft ? -1.0f : 1.0f;
                alternateLeft = !alternateLeft;
            }
            case RANDOM -> side = java.util.concurrent.ThreadLocalRandom.current().nextBoolean() ? -1.0f : 1.0f;
            case STRAFE -> {
                boolean left;
                boolean right;
                try {
                    left = mc.options.keyLeft.isDown();
                    right = mc.options.keyRight.isDown();
                } catch (Exception e) {
                    return InteractionResult.PASS;
                }
                if (left == right) {
                    return InteractionResult.PASS;
                }
                side = left ? -1.0f : 1.0f;
            }
            default -> side = 1.0f;
        }

        float newYaw = player.getYRot() + side * cfg.kbAngle;
        restoreYaw = player.getYRot();
        player.setYRot(newYaw);
        // Шлем Rot-пакет ДО пакета атаки, чтобы сервер применил новый yaw к отбросу.
        try {
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(newYaw, player.getXRot(), player.onGround(), player.horizontalCollision));
            }
        } catch (Exception ignored) {
        }
        restorePending = true;
        cooldownLeft = cfg.kbCooldownTicks;
        return InteractionResult.PASS;
    }

    public void onTick(Minecraft mc) {
        if (cooldownLeft > 0) {
            cooldownLeft--;
        }
        if (restorePending && mc.player != null) {
            try {
                mc.player.setYRot(restoreYaw);
            } catch (Exception ignored) {
            }
            restorePending = false;
        }
    }

    public void toggle(Minecraft mc) {
        ModConfig cfg = ModConfig.get();
        cfg.kbEnabled = !cfg.kbEnabled;
        ModConfig.save();
        try {
            mc.player.displayClientMessage(
                    Component.literal("[KB-Displace] " + (cfg.kbEnabled ? "ON" : "OFF")), false);
        } catch (Exception ignored) {
        }
    }

    private boolean hasKnockback(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return false;
        }
        try {
            Holder<Enchantment> kbHolder = player.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.KNOCKBACK);
            return stack.getEnchantments().getLevel(kbHolder) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
