package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageManager {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final ConfigManager configManager;

    public MessageManager(ChatManagerPlugin plugin) {
        this.configManager = plugin.getConfigManager();
    }

    private String msg(String key) {
        return configManager.getMessage(key);
    }

    // --- Обычные методы с текстом ---
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }

    public void sendMessage(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    public void broadcastMessage(String message) {
        Bukkit.getServer().sendMessage(miniMessage.deserialize(message));
    }

    public void sendMessageToPlayersInRadius(Player sender, String message, int radius) {
        Component component = miniMessage.deserialize(message);
        sendMessageToPlayersInRadius(sender, component, radius);
    }

    public void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    public void broadcastMessage(Component component) {
        Bukkit.getServer().sendMessage(component);
    }

    public void sendMessageToPlayersInRadius(Player sender, Component component, int radius) {
        for (Player player : Bukkit.getOnlinePlayers()) {

            boolean sameWorld = player.getWorld().equals(sender.getWorld());
            boolean inRange = sameWorld && player.getLocation().distance(sender.getLocation()) <= radius;
            boolean hasBypass = player.hasPermission("chatmanager.local.listener");

            Component messageToSend = component;

            if (!inRange && hasBypass) {
                Component prefix = formatMessage(msg("bypass-prefix"));
                messageToSend = prefix.append(component);
            }

            if (inRange || hasBypass) {
                player.sendMessage(messageToSend);
            }
        }
    }

    // --- Служебные методы ---
    public void sendNoPermission(CommandSender sender) {
        sendMessage(sender, msg("no-permission"));
    }

    public void sendReloadSuccess(CommandSender sender) {
        sendMessage(sender, msg("reload-success"));
    }

    public void sendReloadError(CommandSender sender, String error) {
        sendMessage(sender, msg("reload-error").replace("{error}", error));
    }

    public void sendUnknownCommand(CommandSender sender) {
        sendMessage(sender, msg("unknown-command"));
    }

    public Component formatMessage(String message) {
        return miniMessage.deserialize(message);
    }
}
