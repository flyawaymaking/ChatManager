package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatMessageRenderer {

    private final ConfigManager configManager;
    private final boolean hasPapi;
    private final boolean hasLuckPerms;
    private final PlaceholderProcessor placeholderProcessor;
    private final MessageManager messageManager;
    private final Map<UUID, CachedMetaData> metaCache = new ConcurrentHashMap<>();
    private EventSubscription<UserDataRecalculateEvent> subscription;
    private LuckPerms luckPerms;

    private final Map<String, String> legacyColors = new HashMap<>();

    public ChatMessageRenderer(ChatManagerPlugin plugin) {
        this.configManager = plugin.getConfigManager();
        this.placeholderProcessor = plugin.getPlaceholderProcessor();
        this.messageManager = plugin.getMessageManager();

        PluginManager pm = plugin.getServer().getPluginManager();
        this.hasPapi = pm.getPlugin("PlaceholderAPI") != null;
        this.hasLuckPerms = pm.getPlugin("LuckPerms") != null;

        if (hasLuckPerms) {
            luckPerms = LuckPermsProvider.get();
            subscription = luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, event -> {
                metaCache.put(event.getUser().getUniqueId(), event.getUser().getCachedData().getMetaData());
            });
        }

        // Инициализация legacy цветов (остается без изменений)
        initializeLegacyColors();
    }

    private void initializeLegacyColors() {
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

    public EventSubscription<UserDataRecalculateEvent> getSubscription() {
        return subscription;
    }

    /**
     * Форматирование сообщений от команд.
     * - заменяет legacy-цвета (&a) → <green>
     * - обрабатывает PlaceholderAPI (если есть)
     * - применяет кастомные плейсхолдеры (processAllPlaceholders)
     */
    public @NotNull Component renderMessage(CommandSender sender, String message) {
        String result = message;
        for (Map.Entry<String, String> e : legacyColors.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
        }

        if (hasPapi) {
            result = PlaceholderAPI.setPlaceholders(null, result);
        }

        return placeholderProcessor.processAllPlaceholders(sender, messageManager.formatMessage(result));
    }

    /**
     * Основной метод форматирования сообщений.
     *
     * @param player   Игрок, который отправил сообщение
     * @param message  Текст сообщения (без '!')
     * @param isGlobal true, если сообщение глобальное
     * @return Готовый Component для отправки
     */
    public @NotNull Component renderPlayerMessage(Player player, String message, boolean isGlobal) {
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
            CachedMetaData meta = metaCache.computeIfAbsent(
                    player.getUniqueId(),
                    id -> luckPerms.getPlayerAdapter(Player.class)
                            .getUser(player)
                            .getCachedData()
                            .getMetaData()
            );
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
        Component component = messageManager.formatMessage(format);

        // Обрабатываем все специальные заполнители и команды
        component = placeholderProcessor.processAllPlaceholders(player, component);
        return component;
    }

    /**
     * Обработка разрешений игрока для цвета и форматирования.
     * (остается без изменений)
     */
    public String applyColorPermissions(Player player, String message) {
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
