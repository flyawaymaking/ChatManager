package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChatMessageRenderer {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;
    private final boolean hasPapi;
    private final boolean hasLuckPerms;
    private LuckPerms luckPerms;

    private final Map<String, String> legacyColors = new HashMap<>();

    public ChatMessageRenderer(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.miniMessage = MiniMessage.miniMessage();

        PluginManager pm = plugin.getServer().getPluginManager();
        this.hasPapi = pm.getPlugin("PlaceholderAPI") != null;
        this.hasLuckPerms = pm.getPlugin("LuckPerms") != null;

        if (hasLuckPerms) {
            this.luckPerms = LuckPermsProvider.get();
        }

        // &-цвета и форматирование
        legacyColors.put("&0", "<black>");
        legacyColors.put("&1", "<dark_blue>");
        legacyColors.put("&2", "<dark_green>");
        legacyColors.put("&3", "<dark_aqua>");
        legacyColors.put("&4", "<dark_red>");
        legacyColors.put("&5", "<dark_purple>");
        legacyColors.put("&6", "<gold>");
        legacyColors.put("&7", "<gray>");
        legacyColors.put("&8", "<dark_gray>");
        legacyColors.put("&9", "<blue>");
        legacyColors.put("&a", "<green>");
        legacyColors.put("&b", "<aqua>");
        legacyColors.put("&c", "<red>");
        legacyColors.put("&d", "<light_purple>");
        legacyColors.put("&e", "<yellow>");
        legacyColors.put("&f", "<white>");
        legacyColors.put("&l", "<bold>");
        legacyColors.put("&o", "<italic>");
        legacyColors.put("&n", "<underlined>");
        legacyColors.put("&m", "<strikethrough>");
        legacyColors.put("&k", "<obfuscated>");
        legacyColors.put("&r", "<reset>");
    }

    /**
     * Основной метод форматирования сообщений.
     * @param player Игрок, который отправил сообщение
     * @param message Текст сообщения (без '!')
     * @param isGlobal true, если сообщение глобальное
     * @return Готовый Component для отправки
     */
    public @NotNull Component renderMessage(Player player, String message, boolean isGlobal) {
        String messageFormat = configManager.getMessageFormat();
        String scopeFormat = isGlobal
                ? configManager.getGlobalFormat()
                : configManager.getLocalFormat();

        // Подставляем в message-format текст конкретного сообщения
        String formattedMessage = applyColorPermissions(player, message);

        // Получаем LuckPerms данные (если есть)
        String prefix = "";
        String suffix = "";
        String usernameColor = "";
        if (hasLuckPerms) {
            User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
            CachedMetaData meta = user.getCachedData().getMetaData();
            prefix = Objects.requireNonNullElse(meta.getPrefix(), "");
            suffix = Objects.requireNonNullElse(meta.getSuffix(), "");
            usernameColor = Objects.requireNonNullElse(meta.getMetaValue("username-color"), "");
        }

        // Подготавливаем формат с заменами
        String format = messageFormat
                .replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{username-color}", usernameColor)
                .replace("{displayname}", PlainTextComponentSerializer.plainText().serialize(player.displayName()))
                .replace("{name}", player.getName())
                .replace("{message}", formattedMessage);

        // Добавляем значок локального/глобального чата
        format = scopeFormat.replace("{message}", format);

        // Обрабатываем PlaceholderAPI, если есть
        if (hasPapi) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        // Десериализация в Component
        return miniMessage.deserialize(format);
    }

    /**
     * Обработка разрешений игрока для цвета и форматирования.
     */
    private String applyColorPermissions(Player player, String message) {
        String result = message;

        // Игрок имеет полные права
        if (player.hasPermission("chatmanager.color.*") || player.hasPermission("chatmanager.format.*")) {
            for (Map.Entry<String, String> e : legacyColors.entrySet()) {
                result = result.replace(e.getKey(), e.getValue());
            }
            return result;
        }

        // MiniMessage теги
        if (!player.hasPermission("chatmanager.color.advanced")) {
            result = result.replaceAll("<[^>]+>", "");
        }

        // Legacy цвета (&0–&f)
        if (player.hasPermission("chatmanager.color.basic")) {
            for (Map.Entry<String, String> e : legacyColors.entrySet()) {
                if (e.getKey().matches("&[0-9a-f]")) {
                    result = result.replace(e.getKey(), e.getValue());
                } else {
                    result = result.replace(e.getKey(), "");
                }
            }
        } else {
            result = result.replaceAll("&[0-9a-f]", "");
        }

        // Форматирование
        if (!player.hasPermission("chatmanager.format.italic")) {
            result = result.replace("&o", "");
        }
        if (!player.hasPermission("chatmanager.format.bold")) {
            result = result.replace("&l", "");
        }
        if (!player.hasPermission("chatmanager.format.*")) {
            result = result.replaceAll("&[mnkr]", ""); // зачеркнутый, подчёркнутый, обфускация, reset
        }

        return result;
    }

    public String extractPlainMessage(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
