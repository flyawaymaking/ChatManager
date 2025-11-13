package com.flyaway.chatmanager;

import com.flyaway.chatmanager.commands.ChatCommand;
import com.flyaway.chatmanager.listeners.ChatListener;
import com.flyaway.chatmanager.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class ChatManagerPlugin extends JavaPlugin {

    private static ChatManagerPlugin instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);

        // Загрузка конфигурации
        configManager.loadConfig();

        // Регистрация ивентов
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Регистрация команд
        ChatCommand chatCommand = new ChatCommand(this);
        Objects.requireNonNull(getCommand("chatmanager")).setExecutor(chatCommand);
        Objects.requireNonNull(getCommand("chatmanager")).setTabCompleter(chatCommand);

        getLogger().info("ChatManager успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatManager отключен!");
    }

    public static ChatManagerPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
