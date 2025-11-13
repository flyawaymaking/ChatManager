package com.flyaway.chatmanager.listeners;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
import com.flyaway.chatmanager.managers.MessageManager;
import com.flyaway.chatmanager.managers.ChatMessageRenderer;
import com.flyaway.chatmanager.managers.PlaceholderProcessor;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class ChatListener implements Listener {

    private final ConfigManager configManager;
    private final ChatMessageRenderer renderer;
    private final PlaceholderProcessor placeholderProcessor;

    public ChatListener(ChatManagerPlugin plugin) {
        this.configManager = plugin.getConfigManager();
        this.renderer = plugin.getChatMessageRenderer();
        this.placeholderProcessor = plugin.getPlaceholderProcessor();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String plainMessage = renderer.extractPlainMessage(event.message());

        boolean isGlobal = plainMessage.startsWith("!");
        String messageText = isGlobal ? plainMessage.substring(1).trim() : plainMessage.trim();

        // Форматируем сообщение
        Component formatted = renderer.renderMessage(player, messageText, isGlobal);

        // Используем MessageManager для отправки
        if (isGlobal) {
            MessageManager.broadcastMessage(formatted);
        } else {
            int radius = configManager.getLocalChatRadius();
            MessageManager.sendMessageToPlayersInRadius(player, formatted, radius);
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

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String commandLine = event.getCommand(); // полный ввод, например "msg Player текст"
        String lower = commandLine.toLowerCase();

        // Обработка серверных /msg, /m, /tell, /w
        if (lower.startsWith("msg ") || lower.startsWith("m ") ||
                lower.startsWith("tell ") || lower.startsWith("w ")) {

            event.setCancelled(true); // отменяем стандартное выполнение
            String[] parts = commandLine.split(" ", 3);
            if (parts.length < 3) return;

            handleMessageCommand(event.getSender(), parts[1], parts[2], parts[0]);
        }

        // Обработка серверной команды /bc
        else if (lower.startsWith("bc ")) {
            event.setCancelled(true);
            handleBroadcast(event.getSender(), commandLine.substring(3).trim());
        }
    }

    private void handleMessageCommand(CommandSender sender, String targetName, String text, String commandUsed) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(MessageManager.formatMessage("<red>Игрок не найден"));
            return;
        }
        if (sender instanceof Player player) {
            text = renderer.applyColorPermissions(player, text);
        }

        Component formattedText = placeholderProcessor.processAllPlaceholders(sender, MessageManager.formatMessage(text));

        String senderName = sender.getName();
        String miniMessageString = "<gold>[от <red>" + senderName + "<gold>]<reset> ";
        if (sender instanceof Player) {
            sender.sendMessage(MessageManager.formatMessage("<gold>[<red>я <gold>-> <red>" + target.getName() + "<gold>] <reset>").append(formattedText));
            miniMessageString = "<gold>[<hover:show_text:'<yellow>Нажмите чтобы ответить'>" +
                    "<click:run_command:'" + commandUsed + " " + senderName + " '>" +
                    "<gold>от <red>" + senderName + "</click></hover><gold>]<reset> ";
        }

        target.sendMessage(MessageManager.formatMessage(miniMessageString).append(formattedText));
    }

    private void handleBroadcast(CommandSender sender, String text) {
        Component formattedText = placeholderProcessor.processAllPlaceholders(sender, MessageManager.formatMessage(text));
        MessageManager.broadcastMessage(formattedText);
    }
}
