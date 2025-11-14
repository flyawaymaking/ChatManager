package com.flyaway.chatmanager;

import com.flyaway.chatmanager.commands.ChatCommand;
import com.flyaway.chatmanager.listeners.*;
import com.flyaway.chatmanager.managers.*;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class ChatManagerPlugin extends JavaPlugin {

    private static ChatManagerPlugin instance;
    private ConfigManager configManager;
    private ChatMessageRenderer chatMessageRenderer;
    private PlaceholderProcessor placeholderProcessor;
    private LanguageManager languageManager;
    private MessageManager messageManager;
    private PlayerTracker playerTracker;

    // Ссылка на задачу очистки инвентарей
    private int cleanupTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);
        // Загрузка конфигурации
        configManager.loadConfig();
        this.playerTracker = new PlayerTracker();
        this.placeholderProcessor = new PlaceholderProcessor(this);
        this.chatMessageRenderer = new ChatMessageRenderer(this);
        this.languageManager = new LanguageManager(this);
        this.messageManager = new MessageManager(this);

        loadTranslations();

        for (Player p : Bukkit.getOnlinePlayers()) {
            playerTracker.add(p);
        }

        // Регистрация ивентов
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryProtectListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerTrackerListener(this), this);

        // Регистрация команд
        ChatCommand chatCommand = new ChatCommand(this);
        Objects.requireNonNull(getCommand("chatmanager")).setExecutor(chatCommand);
        Objects.requireNonNull(getCommand("chatmanager")).setTabCompleter(chatCommand);

        // Запуск периодической задачи очистки устаревших инвентарей
        cleanupTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            getPlaceholderProcessor().cleanupExpiredInventories();
        }, 1200L, 1200L).getTaskId();

        getLogger().info("ChatManager успешно запущен!");
    }

    public void loadTranslations() {
        languageManager.load();
    }

    @Override
    public void onDisable() {
        languageManager.unload();
        // Отмена подписки LuckPerms
        if (chatMessageRenderer != null) {
            EventSubscription<UserDataRecalculateEvent> sub = chatMessageRenderer.getSubscription();
            if (sub != null) {
                sub.close();
            }
        }

        // Отмена задачи очистки инвентарей
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }

        getLogger().info("ChatManager отключен!");
    }

    public static ChatManagerPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChatMessageRenderer getChatMessageRenderer() {
        return chatMessageRenderer;
    }

    public PlaceholderProcessor getPlaceholderProcessor() {
        return placeholderProcessor;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public PlayerTracker getPlayerTracker() {
        return playerTracker;
    }
}
