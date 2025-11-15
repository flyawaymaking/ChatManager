package com.flyaway.chatmanager.listeners;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
import com.flyaway.chatmanager.managers.MessageManager;
import com.flyaway.chatmanager.managers.ChatMessageRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ChatListener implements Listener {

    private final ConfigManager configManager;
    private final ChatMessageRenderer renderer;
    private final MessageManager messageManager;

    public ChatListener(ChatManagerPlugin plugin) {
        this.configManager = plugin.getConfigManager();
        this.renderer = plugin.getChatMessageRenderer();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String plainMessage = renderer.extractPlainMessage(event.message());

        boolean isGlobal = plainMessage.startsWith("!");
        String messageText = isGlobal ? plainMessage.substring(1).trim() : plainMessage.trim();

        // Форматируем сообщение
        Component formatted = renderer.renderLocaleMessage(player, messageText, isGlobal);

        // Используем MessageManager для отправки
        if (isGlobal) {
            messageManager.broadcastMessage(formatted);
        } else {
            int radius = configManager.getLocalChatRadius();
            messageManager.sendMessageToPlayersInRadius(player, formatted, radius);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage(); // полный ввод, например "/msg target hello"
        String lower = message.toLowerCase();

        if (lower.startsWith("/msg ") || lower.startsWith("/m ") ||
                lower.startsWith("/tell ") || lower.startsWith("/w ")) {
            event.setCancelled(true); // отменяем стандартное выполнение

            String[] parts = message.split(" ", 3);
            if (parts.length < 3) return; // некорректный вызов

            handleMessageCommand(event.getPlayer(), parts[1], parts[2], parts[0]);
        }

        // Обработка команды /bc от игрока
        else if (lower.startsWith("/bc ")) {
            event.setCancelled(true);
            handleBroadcast(event.getPlayer(), message.substring(4).trim());
        }
    }

    private void handleMessageCommand(Player sender, String targetName, String text, String commandUsed) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messageManager.sendMessage(sender, configManager.getMessage("player-not-found"));
            return;
        }

        Component formattedText = renderer.renderMessage(sender, renderer.applyColorPermissions(sender, text));

        // Формат сообщения для получателя
        String messageForSender = configManager.getMessage("to-player").replace("<reset>", "").replace("{target}", target.getName());
        messageManager.sendMessage(sender, messageManager.formatMessage(messageForSender + "<reset> ").append(formattedText));

        // Формат сообщения для отправителя
        String hoverText = configManager.getMessage("reply-hover-text");
        String messageForTarget = configManager.getMessage("from-player").replace("<reset>", "").replace("{sender}", sender.getName());
        messageForTarget = "<hover:show_text:'" + hoverText + "'>" +
                "<click:suggest_command:'" + commandUsed + " " + target.getName() + " '>" +
                messageForTarget + "</click></hover>";

        messageManager.sendMessage(target, messageManager.formatMessage(messageForTarget + "<reset> ").append(formattedText));
    }

    private void handleBroadcast(Player sender, String text) {
        Component formattedText = renderer.renderMessage(sender, renderer.applyColorPermissions(sender, text));
        messageManager.broadcastMessage(formattedText);
    }
}
