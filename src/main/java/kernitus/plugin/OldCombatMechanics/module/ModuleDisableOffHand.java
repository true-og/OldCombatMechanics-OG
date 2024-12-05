/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.module;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import kernitus.plugin.OldCombatMechanics.utilities.ConfigUtils;
import kernitus.plugin.OldCombatMechanics.utilities.Messenger;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Disables usage of the off-hand.
 */
public class ModuleDisableOffHand extends OCMModule {

    private static final int OFFHAND_SLOT = 40;
    private List<Material> materials;
    private String deniedMessage;
    private BlockType blockType;

    public ModuleDisableOffHand(OCMMain plugin) {
        super(plugin, "disable-offhand");
        reload();
    }

    @Override
    public void reload() {
        blockType = module().getBoolean("whitelist") ? BlockType.WHITELIST : BlockType.BLACKLIST;
        materials = ConfigUtils.loadMaterialList(module(), "items");
        deniedMessage = module().getString("denied-message");
    }

    private void sendDeniedMessage(CommandSender sender) {
        if (!deniedMessage.trim().isEmpty())
            Messenger.send(sender, deniedMessage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent e) {
        final Player player = e.getPlayer();
        if (isEnabled(player) && isItemBlocked(e.getOffHandItem())) {
            e.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        final HumanEntity player = e.getWhoClicked();
        if (!isEnabled(player)) return;
        final ClickType clickType = e.getClick();

        try {
            if (clickType == ClickType.SWAP_OFFHAND) {
                e.setResult(Event.Result.DENY);
                sendDeniedMessage(player);
                return;
            }
        } catch (NoSuchFieldError ignored) {
        } // For versions below 1.16

        final Inventory clickedInventory = e.getClickedInventory();
        if (clickedInventory == null) return;
        final InventoryType inventoryType = clickedInventory.getType();
        // Source inventory must be PLAYER
        if(inventoryType != InventoryType.PLAYER) return;

        final InventoryView view = e.getView();
        // If neither of the inventories is CRAFTING, player cannot be moving stuff to the offhand
        if (view.getBottomInventory().getType() != InventoryType.CRAFTING &&
                view.getTopInventory().getType() != InventoryType.CRAFTING)
            return;

        // Prevent shift-clicking a shield into the offhand item slot
        final ItemStack currentItem = e.getCurrentItem();
        if (currentItem != null
                && currentItem.getType() == Material.SHIELD
                && isItemBlocked(currentItem)
                && e.getSlot() != OFFHAND_SLOT
                && e.isShiftClick()) {
            e.setResult(Event.Result.DENY);
            sendDeniedMessage(player);
        }

        if (e.getSlot() == OFFHAND_SLOT &&
                // Let allowed items be placed into offhand slot with number keys (hotbar swap)
                ((clickType == ClickType.NUMBER_KEY && isItemBlocked(clickedInventory.getItem(e.getHotbarButton())))
                        || isItemBlocked(e.getCursor())) // Deny placing not allowed items into offhand slot
        ) {
            e.setResult(Event.Result.DENY);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        final HumanEntity player = e.getWhoClicked();
        if (!isEnabled(player)
                || e.getInventory().getType() != InventoryType.CRAFTING
                || !e.getInventorySlots().contains(OFFHAND_SLOT)) return;

        if (isItemBlocked(e.getOldCursor())) {
            e.setResult(Event.Result.DENY);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        onModesetChange(e.getPlayer());
    }

    @Override
    public void onModesetChange(Player player) {
        final PlayerInventory inventory = player.getInventory();
        final ItemStack offHandItem = inventory.getItemInOffHand();

        if (isItemBlocked(offHandItem)) {
            sendDeniedMessage(player);
            inventory.setItemInOffHand(new ItemStack(Material.AIR));
            if (!inventory.addItem(offHandItem).isEmpty())
                player.getWorld().dropItemNaturally(player.getLocation(), offHandItem);
        }
    }

    private boolean isItemBlocked(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        return !blockType.isAllowed(materials, item.getType());
    }

    private enum BlockType {
        WHITELIST(Collection::contains),
        BLACKLIST(not(Collection::contains));

        private final BiPredicate<Collection<Material>, Material> filter;

        BlockType(BiPredicate<Collection<Material>, Material> filter) {
            this.filter = filter;
        }

        /**
         * Checks whether the given material is allowed.
         *
         * @param list    the list to use for checking
         * @param toCheck the material to check
         * @return true if the item is allowed, based on the list and the current mode
         */
        boolean isAllowed(Collection<Material> list, Material toCheck) {
            return filter.test(list, toCheck);
        }
    }

    private static <T, U> BiPredicate<T, U> not(BiPredicate<T, U> predicate) {
        return predicate.negate();
    }
}
