/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package kernitus.plugin.OldCombatMechanics.module;

import kernitus.plugin.OldCombatMechanics.OCMMain;
import kernitus.plugin.OldCombatMechanics.utilities.reflection.Reflector;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Makes brewing stands not require fuel.
 */
public class ModuleOldBrewingStand extends OCMModule {
    private static final int MAX_FUEL = 20;
    private static final int FUEL_SLOT = 4;
    private static final String BREWING_STAND_TITLE = ChatColor.GOLD + "Brewing Stand";
    private final Method setFuelLevelMethod;
    private final Method setCustomNameMethod;

    public ModuleOldBrewingStand(OCMMain plugin) {
        super(plugin, "old-brewing-stand");
        setFuelLevelMethod = Reflector.getMethod(BrewingStand.class, "setFuelLevel", 1);
        setCustomNameMethod = Reflector.getMethod(BrewingStand.class, "setCustomName", 1);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!isEnabled(event.getPlayer())) {
            return;
        }
        restock(event.getInventory());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isEnabled(event.getPlayer())) {
            return;
        }
        restock(event.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof HumanEntity) || !isEnabled((HumanEntity) event.getWhoClicked())) {
            return;
        }

        final Inventory topInventory = event.getInventory();
        if (!isBrewingInventory(topInventory)) {
            return;
        }

        if (hasFuelSlot(topInventory) && event.getRawSlot() == FUEL_SLOT) {
            event.setCancelled(true);
            restock(topInventory);
            return;
        }

        if (hasFuelSlot(topInventory)
                && event.getClickedInventory() != null
                && Objects.equals(event.getClickedInventory(), event.getWhoClicked().getInventory())
                && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                && isBlazePowder(event.getCurrentItem())) {
            event.setCancelled(true);
            restock(topInventory);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> restock(topInventory));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof HumanEntity) || !isEnabled((HumanEntity) event.getWhoClicked())) {
            return;
        }

        final Inventory topInventory = event.getInventory();
        if (!isBrewingInventory(topInventory)) {
            return;
        }

        if (hasFuelSlot(topInventory) && event.getRawSlots().contains(FUEL_SLOT)) {
            event.setCancelled(true);
            restock(topInventory);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> restock(topInventory));
    }

    @EventHandler
    public void onBrew(BrewEvent event) {
        if (!isEnabled(event.getBlock().getWorld())) {
            return;
        }
        restock(event.getBlock());
    }

    private void restock(Inventory inventory) {
        if (!isBrewingInventory(inventory)) {
            return;
        }

        if (hasFuelSlot(inventory)) {
            inventory.setItem(FUEL_SLOT, new ItemStack(Material.BLAZE_POWDER, 1));
        }

        final Location location = inventory.getLocation();
        if (location == null) {
            return;
        }

        restock(location.getBlock());
    }

    private void restock(Block block) {
        final BlockState blockState = block.getState();
        if (!(blockState instanceof BrewingStand)) {
            return;
        }

        final BrewingStand brewingStand = (BrewingStand) blockState;
        boolean updated = false;
        if (setCustomNameMethod != null) {
            Reflector.invokeMethod(setCustomNameMethod, brewingStand, BREWING_STAND_TITLE);
            updated = true;
        }
        if (setFuelLevelMethod != null) {
            Reflector.invokeMethod(setFuelLevelMethod, brewingStand, MAX_FUEL);
            updated = true;
        }
        if (updated) {
            brewingStand.update();
        }

        final Inventory inventory = brewingStand.getInventory();
        if (hasFuelSlot(inventory)) {
            inventory.setItem(FUEL_SLOT, new ItemStack(Material.BLAZE_POWDER, 1));
        }
    }

    private static boolean isBrewingInventory(Inventory inventory) {
        return inventory != null && inventory.getType() == InventoryType.BREWING;
    }

    private static boolean hasFuelSlot(Inventory inventory) {
        return inventory != null && inventory.getSize() > FUEL_SLOT;
    }

    private static boolean isBlazePowder(ItemStack item) {
        return item != null && item.getType() == Material.BLAZE_POWDER;
    }
}
