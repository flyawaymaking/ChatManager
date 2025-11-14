package com.flyaway.chatmanager.listeners;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.PlaceholderProcessor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class InventoryProtectListener implements Listener {

    private final PlaceholderProcessor placeholderProcessor;

    public InventoryProtectListener(ChatManagerPlugin plugin) {
        this.placeholderProcessor = plugin.getPlaceholderProcessor();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();

        if (placeholderProcessor.isTempInventory(inv)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();

        if (placeholderProcessor.isTempInventory(inv)) {
            event.setCancelled(true);
        }
    }
}
