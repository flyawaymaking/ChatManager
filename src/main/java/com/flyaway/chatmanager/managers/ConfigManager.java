package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ChatManagerPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // Геттеры для конфигурации
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

    public FileConfiguration getConfig() {
        return config;
    }
}
