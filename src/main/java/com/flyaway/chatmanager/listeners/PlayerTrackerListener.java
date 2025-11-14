package com.flyaway.chatmanager.listeners;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.PlayerTracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerTrackerListener implements Listener {

    private final PlayerTracker tracker;

    public PlayerTrackerListener(ChatManagerPlugin plugin) {
        this.tracker = plugin.getPlayerTracker();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        tracker.add(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracker.remove(event.getPlayer());
    }
}
