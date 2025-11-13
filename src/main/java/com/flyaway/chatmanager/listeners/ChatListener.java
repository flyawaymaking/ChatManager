package com.flyaway.chatmanager.listeners;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
import com.flyaway.chatmanager.managers.MessageManager;
import com.flyaway.chatmanager.managers.ChatMessageRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final ChatMessageRenderer renderer;

    public ChatListener(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.renderer = new ChatMessageRenderer(plugin);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String plainMessage = renderer.extractPlainMessage(event.message());

        boolean isGlobal = plainMessage.startsWith("!");
        String messageText = isGlobal ? plainMessage.substring(1).trim() : plainMessage.trim();

        // Форматируем сообщение
        Component formatted = renderer.renderMessage(sender, messageText, isGlobal);

        // Используем MessageManager для отправки
        if (isGlobal) {
            MessageManager.broadcastMessage(formatted);
        } else {
            int radius = configManager.getLocalChatRadius();
            MessageManager.sendMessageToPlayersInRadius(sender, formatted, radius);
        }
    }
}
