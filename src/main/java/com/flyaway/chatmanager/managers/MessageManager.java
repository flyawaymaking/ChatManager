package com.flyaway.chatmanager.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageManager {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    // --- Обычные методы с текстом ---
    public static void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(miniMessage.deserialize(message));
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(miniMessage.deserialize(message));
    }

    public static void broadcastMessage(String message) {
        Bukkit.getServer().sendMessage(miniMessage.deserialize(message));
    }

    public static void sendMessageToPlayersInRadius(Player sender, String message, int radius) {
        Component component = miniMessage.deserialize(message);
        sendMessageToPlayersInRadius(sender, component, radius);
    }

    public static void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    public static void broadcastMessage(Component component) {
        Bukkit.getServer().sendMessage(component);
    }

    public static void sendMessageToPlayersInRadius(Player sender, Component component, int radius) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(sender.getWorld()) &&
                    player.getLocation().distance(sender.getLocation()) <= radius) {
                player.sendMessage(component);
            }
        }
    }

    // --- Служебные методы ---
    public static void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "<red>У вас нет прав на использование этой команды.");
    }

    public static void sendReloadSuccess(CommandSender sender) {
        sendMessage(sender, "<green>Конфигурация успешно перезагружена!");
    }

    public static void sendReloadError(CommandSender sender, String error) {
        sendMessage(sender, "<red>Ошибка при перезагрузке: " + error);
    }

    public static void sendUnknownCommand(CommandSender sender) {
        sendMessage(sender, "<red>Неизвестная команда. Используйте <yellow>/chatmanager help</yellow> для списка команд.");
    }

    public static Component formatMessage(String message) {
        return miniMessage.deserialize(message);
    }
}
