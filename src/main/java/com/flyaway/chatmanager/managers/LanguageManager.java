package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationStore;
import net.kyori.adventure.translation.TranslationStore.StringBased;

import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class LanguageManager {

    private final ChatManagerPlugin plugin;
    public static final Locale RU = Locale.forLanguageTag("ru-RU");
    private TranslationStore store;

    public LanguageManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            // Создаем TranslationStore
            this.store = TranslationStore.messageFormat(Key.key("chatmanager", "translations"));

            // Загружаем переводы
            loadFromResourceBundle();

            // Добавляем в GlobalTranslator
            GlobalTranslator.translator().addSource(store);

            plugin.getLogger().info("Загружен русский перевод");

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка загрузки переводов: " + e.getMessage());
        }
    }

    private void loadFromResourceBundle() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("lang.ru_ru", RU);

            for (String key : bundle.keySet()) {
                String pattern = bundle.getString(key);
                store.register(key, RU, new MessageFormat(pattern, RU));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Не удалось загрузить ResourceBundle: " + e.getMessage());
        }
    }

    public void unload() {
        if (store != null) {
            GlobalTranslator.translator().removeSource(store);
        }
    }
}
