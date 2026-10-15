package xuqor.autocart.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static int findRail(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.RAIL)) {
                return i;
            }
        }
        return -1;
    }

    public static int findTntMinecart(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.is(Items.TNT_MINECART)) {
                return i;
            }
        }
        return -1;
    }

    public static int findFlameBow(LocalPlayer player) {
        Inventory inv = player.getInventory();
        Holder<Enchantment> flameHolder = null;
        try {
            var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            flameHolder = lookup.getOrThrow(Enchantments.FLAME);
        } catch (Exception e) {
            flameHolder = null;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(Items.BOW)) {
                continue;
            }
            if (flameHolder == null) {
                return i;
            }
            try {
                int level = stack.getEnchantments().getLevel(flameHolder);
                if (level > 0) {
                    return i;
                }
            } catch (Exception e) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hasArrow(Inventory inv) {
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) {
                return true;
            }
        }
        return false;
    }

    public static void selectSlot(LocalPlayer player, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) {
            return;
        }
        player.getInventory().setSelectedSlot(hotbarSlot);
    }
}
