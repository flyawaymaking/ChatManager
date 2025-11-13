package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class PlaceholderProcessor {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final boolean hasPapi;

    public PlaceholderProcessor(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.hasPapi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Обрабатывает все типы плейсхолдеров
     */
    public Component processAllPlaceholders(CommandSender sender, Component component) {
        Map<String, ConfigManager.PlaceholderConfig> placeholders = configManager.getPlaceholderConfigs();
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);

        for (Map.Entry<String, ConfigManager.PlaceholderConfig> entry : placeholders.entrySet()) {
            String placeholderKey = entry.getKey();
            ConfigManager.PlaceholderConfig config = entry.getValue();

            if ("{command}".equals(placeholderKey)) {
                component = processCommandPlaceholders(sender, component, plainText, config);
            } else {
                String placeholder = "[" + placeholderKey + "]";
                if (plainText.contains(placeholder)) {
                    component = replaceCustomPlaceholder(component, placeholder, sender, config);
                }
            }
        }

        return component;
    }

    /**
     * Обрабатывает команды в формате [/command]
     */
    private Component processCommandPlaceholders(CommandSender sender, Component component, String plainText,
                                                 ConfigManager.PlaceholderConfig config) {
        Pattern commandPattern = Pattern.compile("\\[(/[^]]+)]");
        java.util.regex.Matcher matcher = commandPattern.matcher(plainText);

        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            String command = matcher.group(1);

            // Создаем компонент для команды
            Component commandComponent = createPlaceholderComponent(sender, command, config);

            // Заменяем в исходном компоненте
            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .match(Pattern.quote(fullMatch))
                            .replacement(commandComponent)
                            .build()
            );
        }

        return component;
    }

    /**
     * Заменяет кастомный заполнитель на интерактивный компонент
     */
    private Component replaceCustomPlaceholder(Component component, String placeholder,
                                               CommandSender sender, ConfigManager.PlaceholderConfig config) {
        Component placeholderComponent = createPlaceholderComponent(sender, placeholder, config);

        return component.replaceText(
                TextReplacementConfig.builder()
                        .match(Pattern.quote(placeholder))
                        .replacement(placeholderComponent)
                        .build()
        );
    }

    /**
     * Создает интерактивный компонент для плейсхолдера
     */
    private Component createPlaceholderComponent(CommandSender sender, String value, ConfigManager.PlaceholderConfig config) {
        // Подготавливаем текст
        String displayText = processPlaceholderText(config.getDisplayText(), sender, value);
        String hoverText = processPlaceholderText(config.getHoverText(), sender, value);
        String clickValue = processPlaceholderText(config.getClickValue(), sender, value);

        // Создаем базовый компонент
        Component component = MessageManager.formatMessage(displayText);

        // Добавляем hover событие
        if (!hoverText.isEmpty()) {
            component = component.hoverEvent(
                    HoverEvent.showText(MessageManager.formatMessage(hoverText))
            );
        }

        // Добавляем click событие
        ClickEvent clickEvent = createClickEvent(config.getClickAction(), clickValue, sender);
        if (clickEvent != null) {
            component = component.clickEvent(clickEvent);
        }

        return component;
    }

    /**
     * Создает click event
     */
    private ClickEvent createClickEvent(String action, String value, CommandSender sender) {
        if (action == null || value == null) return null;

        ClickType type;
        try {
            type = ClickType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Неизвестный тип click-action: " + action);
            return null;
        }

        return switch (type) {
            case OPEN_URL -> ClickEvent.openUrl(value);
            case RUN_COMMAND -> ClickEvent.runCommand(value);
            case SUGGEST_COMMAND -> ClickEvent.suggestCommand(value);
            case COPY_TO_CLIPBOARD -> ClickEvent.copyToClipboard(value);
            case SHOW_INV, SHOW_ENDER, SHOW_ITEM -> {
                if (sender instanceof Player player) {
                    String cmd = getInventoryCommand(type, player.getName());
                    yield ClickEvent.runCommand(cmd);
                }
                yield null;
            }
        };
    }

    /**
     * Генерирует команду для просмотра инвентарей
     */
    private String getInventoryCommand(ClickType type, String name) {
        return switch (type) {
            case SHOW_INV -> "/inventory " + name;
            case SHOW_ENDER -> "/enderchest " + name;
            case SHOW_ITEM -> "/viewitem " + name;
            default -> "";
        };
    }

    /**
     * Обрабатывает текст с подстановкой плейсхолдеров
     */
    private String processPlaceholderText(String text, CommandSender sender, String value) {
        if (text == null) return "";

        String result = text
                .replace("{command}", value)
                .replace("{player}", sender.getName());

        // PlaceholderAPI
        if (hasPapi) {
            if (sender instanceof Player player) {
                result = PlaceholderAPI.setPlaceholders(player, result);
            } else {
                result = PlaceholderAPI.setPlaceholders(null, result);
            }
        }

        // Предмет в руке — только если sender это игрок
        if (sender instanceof Player player && result.contains("{item}")) {
            result = result.replace("{item}", getItemInHandDisplay(player));
        }

        return result;
    }

    /**
     * Получает отображаемое название предмета в руке
     */
    private String getItemInHandDisplay(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            return "<gray>Пусто";
        }

        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
        } else {
            return "<green>" + formatItemName(item.getType().toString());
        }
    }

    private String formatItemName(String materialName) {
        return materialName.toLowerCase()
                .replace('_', ' ')
                .replace("minecraft:", "");
    }

    public enum ClickType {
        OPEN_URL,
        RUN_COMMAND,
        SUGGEST_COMMAND,
        COPY_TO_CLIPBOARD,
        SHOW_INV,
        SHOW_ENDER,
        SHOW_ITEM
    }
}
