package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final ChatManagerPlugin plugin;
    private FileConfiguration config;
    private final Map<String, PlaceholderConfig> placeholderConfigs = new HashMap<>();
    private PlaceholderConfig commandConfig;

    public ConfigManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadPlaceholderConfigs();
        loadCommandConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        placeholderConfigs.clear();
        loadPlaceholderConfigs();
        loadCommandConfig();
    }

    private void loadCommandConfig() {
        if (!config.getBoolean("commands.enabled")) {
            commandConfig = null;
        } else {
            commandConfig = new PlaceholderConfig(
                    config.getString("commands.display-text", "<aqua>[<yellow>{command}<aqua>]<reset>"),
                    config.getString("commands.hover-text", "<yellow>Нажмите, чтобы использовать команду!"),
                    config.getString("commands.click-action", "SUGGEST_COMMAND"),
                    "{command}",
                    null,
                    config.getString("commands.description", "Преобразует команды в квадратных скобках в кликабельные элементы")
            );
        }
    }

    private void loadPlaceholderConfigs() {
        if (!config.contains("custom-placeholders")) {
            return;
        }

        for (String key : config.getConfigurationSection("custom-placeholders").getKeys(false)) {
            String path = "custom-placeholders." + key;

            PlaceholderConfig placeholder = new PlaceholderConfig(
                    config.getString(path + ".display-text", "display-" + key),
                    config.getString(path + ".hover-text", "hover-" + key),
                    config.getString(path + ".click-action"),
                    config.getString(path + ".click-value"),
                    config.getString(path + ".inventory-title", "title-" + key),
                    config.getString(path + ".description", "description-" + key)
            );

            placeholderConfigs.put(key.toLowerCase(), placeholder);
        }
    }

    // Геттеры
    public Integer getLocalChatRadius() {
        return config.getInt("local-chat-radius", 100);
    }

    public String getMessageFormat() {
        return config.getString("message-format", "{prefix}{username-color}{displayname}{suffix}<dark_gray> »<reset> {message}");
    }

    public String getLocalFormat() {
        return config.getString("formats.local", "<yellow>Ⓛ</yellow> {message}");
    }

    public String getGlobalFormat() {
        return config.getString("formats.global", "<green>Ⓖ</green> {message}");
    }

    public Integer getInvExpiredMinutes() {
        return config.getInt("inv-expired-minutes", 3);
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "<red>message." + key + " not-found");
        return message.replaceAll("\\s+$", "");
    }

    public Map<String, PlaceholderConfig> getPlaceholderConfigs() {
        return placeholderConfigs;
    }

    public boolean isPlayerHoverEnabled() {
        return config.getBoolean("player-hover.enabled", true);
    }

    public boolean isPlayerMentionEnabled() {
        return config.getBoolean("player-mention.enabled", true);
    }

    public PlaceholderConfig getCommandConfig() {
        return commandConfig;
    }

    public String getLanguage() {
        return config.getString("language", "ru_RU");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // Конфиг для плейсхолдеров
    public static class PlaceholderConfig {
        private final String displayText;
        private final String hoverText;
        private final String clickAction;
        private final String clickValue;
        private final String inventoryTitle;
        private final String description;

        public PlaceholderConfig(String displayText, String hoverText, String clickAction,
                                 String clickValue, String inventoryTitle, String description) {
            this.displayText = displayText;
            this.hoverText = hoverText;
            this.clickAction = clickAction;
            this.clickValue = clickValue;
            this.inventoryTitle = inventoryTitle;
            this.description = description;
        }

        public String getDisplayText() {
            return displayText;
        }

        public String getHoverText() {
            return hoverText;
        }

        public String getClickAction() {
            return clickAction;
        }

        public String getClickValue() {
            return clickValue;
        }

        public String getInventoryTitle() {
            return inventoryTitle;
        }

        public String getDescription() {
            return description;
        }
    }
}
