package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.GlobalTranslator;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LanguageManager {

    private final ChatManagerPlugin plugin;
    private ResourceBundle bundle;

    public LanguageManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            // Попытка загрузить файл перевода для русской локали
            bundle = ResourceBundle.getBundle("lang.ru_ru");

            plugin.getLogger().info("Загружен русский перевод");

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка загрузки переводов: " + e.getMessage());
        }
    }

    public Component translate(@NotNull TranslatableComponent key, @NotNull Locale locale) {
        if (locale.toString().toLowerCase().contains("ru") && bundle != null) {
            // Получаем перевод по ключу
            try {
                String result = bundle.getString(key.key());
                return Component.text(result);
            } catch (MissingResourceException e) {
                // Если ключ не найден, возвращаем исходный ключ
                plugin.getLogger().warning("Не найден перевод для ключа: " + key.key());
            }
        }

        // Если не найден перевод для русской локали, используем fallback
        return GlobalTranslator.render(key, locale);
    }
}
