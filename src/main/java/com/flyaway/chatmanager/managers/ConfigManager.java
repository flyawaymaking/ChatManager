package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final ChatManagerPlugin plugin;
    private FileConfiguration config;
    private final Map<String, PlaceholderConfig> placeholderConfigs = new HashMap<>();

    public ConfigManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadPlaceholderConfigs();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        placeholderConfigs.clear();
        loadPlaceholderConfigs();
    }

    private void loadCommandConfig() {
        if (!config.getBoolean("commands.enabled")) {
            return;
        }
        PlaceholderConfig commandConfig = new PlaceholderConfig(
                config.getString("commands.display-text", "<aqua>[<yellow>{command}<aqua>]<reset>"),
                config.getString("commands.hover-text", "<yellow>Нажмите, чтобы использовать команду!"),
                config.getString("commands.click-action", "SUGGEST_COMMAND"),
                "{command}",
                null
        );
        placeholderConfigs.put("{command}", commandConfig);
    }

    private void loadPlaceholderConfigs() {
        loadCommandConfig();
        if (!config.contains("custom-placeholders")) {
            return;
        }

        for (String key : config.getConfigurationSection("custom-placeholders").getKeys(false)) {
            String path = "custom-placeholders." + key;

            PlaceholderConfig placeholder = new PlaceholderConfig(
                    config.getString(path + ".display-text"),
                    config.getString(path + ".hover-text"),
                    config.getString(path + ".click-action"),
                    config.getString(path + ".click-value"),
                    config.getString(path + ".inventory-title")
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

    public Map<String, PlaceholderConfig> getPlaceholderConfigs() {
        return placeholderConfigs;
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

        public PlaceholderConfig(String displayText, String hoverText, String clickAction,
                                 String clickValue, String inventoryTitle) {
            this.displayText = displayText;
            this.hoverText = hoverText;
            this.clickAction = clickAction;
            this.clickValue = clickValue;
            this.inventoryTitle = inventoryTitle;
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
    }
}
