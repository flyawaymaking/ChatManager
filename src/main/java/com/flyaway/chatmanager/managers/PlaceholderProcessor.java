package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.regex.Pattern;

public class PlaceholderProcessor {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final PlayerTracker playerTracker;
    private final MentionManager mentionManager;
    private final boolean hasPapi;
    private final Map<UUID, Map<ClickType, TimedInventory>> tempInventories = new HashMap<>();

    public PlaceholderProcessor(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.playerTracker = plugin.getPlayerTracker();
        this.mentionManager = plugin.getMentionManager();
        this.hasPapi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Обрабатывает все типы плейсхолдеров
     */
    public Component processAllPlaceholders(CommandSender sender, Component component) {
        Map<String, ConfigManager.PlaceholderConfig> placeholders = configManager.getPlaceholderConfigs();
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);

        component = processCommandPlaceholders(sender, component, plainText);
        component = processPlayerMention(component, plainText);
        component = processPlayerHover(component, plainText);

        for (Map.Entry<String, ConfigManager.PlaceholderConfig> entry : placeholders.entrySet()) {
            String placeholderKey = entry.getKey();
            ConfigManager.PlaceholderConfig config = entry.getValue();

            String placeholder = "[" + placeholderKey + "]";
            if (plainText.contains(placeholder)) {
                component = replaceCustomPlaceholder(component, placeholder, sender, config);
            }
        }

        return component;
    }

    /**
     * Уведомляет игроков об упоминании в чате
     */
    private Component processPlayerMention(Component component, String plainText) {
        if (!configManager.isPlayerMentionEnabled()) return component;

        for (String name : playerTracker.getPlayerNames()) {
            String mentionName = "@" + name;
            if (!plainText.contains(mentionName)) continue;
            Player player = Bukkit.getPlayerExact(name);
            if (player == null) continue;

            mentionManager.sendMentionMessage(player);

            component = component.replaceText(b -> b
                    .matchLiteral(mentionName)
                    .replacement(messageManager.formatMessage("<aqua>" + mentionName + "</aqua>"))
            );
        }

        return component;
    }

    /**
     * Добавляет hover никам игроков
     */
    private Component processPlayerHover(Component component, String plainText) {
        if (!configManager.isPlayerHoverEnabled()) return component;
        String hoverText = configManager.getMessage("player-hover-text");

        for (String name : playerTracker.getPlayerNames()) {
            if (!plainText.contains(name)) continue;
            Player player = Bukkit.getPlayerExact(name);
            if (player == null) continue;

            Component hover = messageManager.formatMessage(processPlaceholderText(hoverText, player, ""));

            component = component.replaceText(b -> b
                    .matchLiteral(name)
                    .replacement(Component.text(name).hoverEvent(HoverEvent.showText(hover))
                    )
            );
        }

        return component;
    }

    /**
     * Обрабатывает команды в формате [/command]
     */
    private Component processCommandPlaceholders(CommandSender sender, Component component, String plainText) {
        ConfigManager.PlaceholderConfig config = configManager.getCommandConfig();
        if (config == null) return component;

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
        String inventoryTitle = processPlaceholderText(config.getInventoryTitle(), sender, value);

        // Создаем базовый компонент
        Component component = messageManager.formatMessage(displayText);

        // Добавляем hover событие
        if (!hoverText.isEmpty()) {
            component = component.hoverEvent(
                    HoverEvent.showText(messageManager.formatMessage(hoverText))
            );
        }

        // Добавляем click событие
        ClickEvent clickEvent = createClickEvent(config.getClickAction(), clickValue, sender, inventoryTitle);
        if (clickEvent != null) {
            component = component.clickEvent(clickEvent);
        }

        return component;
    }

    /**
     * Создает click event
     */
    private ClickEvent createClickEvent(String action, String value, CommandSender sender, String inventoryTitle) {
        if (action == null || value == null) return null;

        ClickType type;
        try {
            type = ClickType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Неизвестный тип click-action: " + action);
            return null;
        }

        // Создаём заранее GUI для инвентарей
        if (sender instanceof Player player && (type == ClickType.SHOW_INV || type == ClickType.SHOW_ENDER || type == ClickType.SHOW_ITEM)) {
            Inventory inv = createInventoryForPlayer(player, type, inventoryTitle);
            TimedInventory timedInv = new TimedInventory(inv);
            tempInventories.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(type, timedInv);

            // Кликабельный компонент просто вызовет команду открытия GUI
            String cmd = "/chatmanager openinv " + player.getUniqueId() + " " + type.name().toLowerCase();
            return ClickEvent.runCommand(cmd);
        }

        return switch (type) {
            case OPEN_URL -> ClickEvent.openUrl(value);
            case RUN_COMMAND -> ClickEvent.runCommand(value);
            case SUGGEST_COMMAND -> ClickEvent.suggestCommand(value);
            case COPY_TO_CLIPBOARD -> ClickEvent.copyToClipboard(value);
            default -> null;
        };
    }

    /**
     * Создает GUI для игрока с дополнительными рядами
     */
    private Inventory createInventoryForPlayer(Player player, ClickType type, String inventoryTitle) {
        Inventory inv;
        int size;
        switch (type) {
            case SHOW_ITEM -> size = 9;
            case SHOW_ENDER -> size = 9 * 3;
            case SHOW_INV -> size = 9 * 6;
            default -> size = 9;
        }

        // Преобразуем название в Component через MessageManager
        Component titleComponent = (inventoryTitle == null || inventoryTitle.isEmpty())
                ? messageManager.formatMessage("Инвентарь " + player.getName())
                : messageManager.formatMessage(inventoryTitle);

        // Создаём инвентарь с Component заголовком (Paper 1.19+)
        inv = Bukkit.createInventory(new ViewOnlyHolder(), size, titleComponent);

        switch (type) {
            case SHOW_ITEM -> {
                var item = player.getInventory().getItemInMainHand();
                if (!item.getType().isAir()) inv.setItem(4, item.clone());

                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                int[] firstRow = {0, 1, 2, 3, 5, 6, 7, 8};
                for (int slot : firstRow) {
                    if (inv.getItem(slot) == null || Objects.requireNonNull(inv.getItem(slot)).getType().isAir()) {
                        inv.setItem(slot, filler);
                    }
                }
            }
            case SHOW_ENDER -> inv.setContents(player.getEnderChest().getContents());
            case SHOW_INV -> {

                // === Подготовка заливки стеклом ===
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) meta.displayName(messageManager.formatMessage("<gray>"));
                filler.setItemMeta(meta);

                // === Ряд 1: голова + опыт + броня + офф-рука ===
                // Слоты: [0]=голова игрока, [1]=уровень,
                //         [3]=шлем, [4]=нагрудник, [5]=поножи, [6]=ботинки,
                //         [8]=офф-рука, остальные заполняются стеклом
                // slot 0 — голова игрока
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                if (skullMeta != null) {
                    skullMeta.setOwningPlayer(player);
                    skullMeta.displayName(messageManager.formatMessage("<yellow>" + player.getName()));
                    skull.setItemMeta(skullMeta);
                }
                inv.setItem(0, skull);

                // slot 1 — уровень опыта (как EXPERIENCE_BOTTLE)
                ItemStack exp = new ItemStack(Material.EXPERIENCE_BOTTLE);
                ItemMeta expMeta = exp.getItemMeta();
                if (expMeta != null) {
                    expMeta.displayName(messageManager.formatMessage("<yellow>Уровень: " + player.getLevel()));
                    exp.setItemMeta(expMeta);
                }
                inv.setItem(1, exp);

                // 3–6: броня
                ItemStack[] armor = player.getEquipment().getArmorContents(); // boots, leggings, chest, helmet
                if (armor.length == 4) {
                    inv.setItem(3, armor[3]); // helmet
                    inv.setItem(4, armor[2]); // chestplate
                    inv.setItem(5, armor[1]); // leggings
                    inv.setItem(6, armor[0]); // boots
                }

                // slot 8 — офф-рука
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (!offhand.getType().isAir()) {
                    inv.setItem(8, offhand.clone());
                }

                // Остальные ячейки первого ряда → стекло
                int[] firstRow = {0, 1, 2, 3, 4, 5, 6, 7, 8};
                for (int slot : firstRow) {
                    if (inv.getItem(slot) == null || Objects.requireNonNull(inv.getItem(slot)).getType().isAir()) {
                        inv.setItem(slot, filler);
                    }
                }

                // === Ряд 2: стекло (как и раньше) ===
                for (int i = 9; i < 18; i++) inv.setItem(i, filler);

                // === Ряды 3-6: основной инвентарь + хотбар ===
                int targetSlot = 18;

                // Основной инвентарь
                for (int i = 9; i < 36; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null) inv.setItem(targetSlot, item.clone());
                    targetSlot++;
                }

                // Хотбар
                for (int i = 0; i < 9; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null) inv.setItem(targetSlot, item.clone());
                    targetSlot++;
                }
            }
        }

        return inv;
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

        try {
            // Получаем переведенное название
            Component translatedName = getItemDisplayName(item, player.locale());

            // Добавляем количество если больше 1
            if (item.getAmount() > 1) {
                translatedName = translatedName
                        .append(Component.text(" × "))
                        .append(Component.text(item.getAmount()));
            }

            return "<green>" + PlainTextComponentSerializer.plainText().serialize(translatedName);

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении названия предмета: " + e.getMessage());
            // Fallback на старый метод
            return "<green>" + formatItemName(item.getType().toString());
        }
    }

    private Component getItemDisplayName(ItemStack item, Locale locale) {
        if (item == null || item.getType().isAir()) {
            return Component.text("Пусто");
        }

        ItemMeta meta = item.getItemMeta();
        Component displayName;

        if (meta != null && meta.hasDisplayName()) {
            // Используем кастомное имя предмета
            displayName = meta.displayName();
        } else {
            // Используем translation key предмета для автоматического перевода
            displayName = Component.translatable(item.getType().translationKey());
        }

        // Переводим через GlobalTranslator
        return GlobalTranslator.render(displayName, locale);
    }

    private String formatItemName(String materialName) {
        return materialName.toLowerCase()
                .replace('_', ' ')
                .replace("minecraft:", "")
                .replace("block.", "")
                .replace("item.", "");
    }

    /**
     * Открывает GUI для просмотра инвентаря/руки/эндер сундука игрока.
     *
     * @param viewer     Игрок, который будет видеть GUI
     * @param targetUUID UUID игрока, чей инвентарь показываем
     * @param type       Тип GUI (SHOW_INV, SHOW_ENDER, SHOW_ITEM)
     */
    public void openInventoryGUI(Player viewer, UUID targetUUID, ClickType type) {
        Map<ClickType, TimedInventory> invMap = tempInventories.get(targetUUID);
        if (invMap == null) {
            viewer.sendMessage(messageManager.formatMessage("<red>Этот просмотр инвентаря истёк."));
            return;
        }

        TimedInventory timedInv = invMap.get(type);
        if (timedInv == null) {
            viewer.sendMessage(messageManager.formatMessage("<red>Этот просмотр инвентаря истёк."));
            return;
        }

        viewer.openInventory(timedInv.getInventory());
    }

    private class ViewOnlyHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null; // не используется
        }
    }


    public boolean isTempInventory(Inventory inv) {
        return inv.getHolder() instanceof ViewOnlyHolder;
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

    private static class TimedInventory {
        private final Inventory inventory;
        private final long timestamp;

        public TimedInventory(Inventory inventory) {
            this.inventory = inventory;
            this.timestamp = System.currentTimeMillis();
        }

        public Inventory getInventory() {
            return inventory;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    public void cleanupExpiredInventories() {
        long now = System.currentTimeMillis();
        long expiryMillis = configManager.getInvExpiredMinutes() * 60 * 1000;

        tempInventories.forEach((uuid, invMap) -> {
            invMap.entrySet().removeIf(entry -> now - entry.getValue().getTimestamp() > expiryMillis);
        });

        tempInventories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
