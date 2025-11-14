package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MentionManager {
    private final MessageManager messageManager;
    private final Map<UUID, Boolean> mentionNotifications = new HashMap<>();

    public MentionManager(ChatManagerPlugin plugin) {
        this.messageManager = plugin.getMessageManager();
    }

    // Метод для установки состояния упоминаний для игрока
    public void setMentionEnabled(Player player, boolean enabled) {
        mentionNotifications.put(player.getUniqueId(), enabled);
    }

    // Проверка, разрешено ли уведомление об упоминаниях для игрока
    public boolean isMentionEnabled(Player player) {
        return mentionNotifications.getOrDefault(player.getUniqueId(), true);
    }

    // Отправка сообщения о том, что игрок был упомянут
    public void sendMentionMessage(Player player) {
        if (!isMentionEnabled(player)) return;
        Component actionBarMessage = messageManager.formatMessage("<gradient:gold:yellow>Вас упомянули в чате!</gradient>");
        player.sendActionBar(actionBarMessage);
    }
}
