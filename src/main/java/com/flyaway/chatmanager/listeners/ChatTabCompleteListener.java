package com.flyaway.chatmanager.listeners;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatTabCompleteListener implements Listener {

    private final ChatManagerPlugin plugin;
    private final List<String> cachedPlaceholders = new CopyOnWriteArrayList<>();

    public ChatTabCompleteListener(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        updateCachedPlaceholders();
    }

    /**
     * Обновление кэша плейсхолдеров. Можно вызывать при перезагрузке конфигурации.
     */
    public void updateCachedPlaceholders() {
        ConfigManager config = plugin.getConfigManager();
        cachedPlaceholders.clear();
        cachedPlaceholders.addAll(
                config.getPlaceholderConfigs().keySet().stream()
                        .filter(key -> !key.equals("{command}"))
                        .map(key -> "[" + key + "]")
                        .toList()
        );
    }

    @EventHandler
    public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
        // Игнорируем команды
        if (event.getBuffer().startsWith("/")) return;

        String buffer = event.getBuffer().toLowerCase();

        // Фильтруем кэшированные плейсхолдеры по введенному тексту
        List<String> suggestions = cachedPlaceholders.stream()
                .filter(ph -> ph.toLowerCase().startsWith(buffer))
                .toList();

        // Добавляем свои подсказки поверх уже существующих, не удаляя чужие
        event.getCompletions().addAll(suggestions);
    }
}
