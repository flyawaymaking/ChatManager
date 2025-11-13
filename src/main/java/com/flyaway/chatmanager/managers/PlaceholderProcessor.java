package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class PlaceholderProcessor {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final boolean hasPapi;
    private final Map<UUID, Map<ClickType, TimedInventory>> tempInventories = new HashMap<>();

    public PlaceholderProcessor(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.hasPapi = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    /**
     * Обрабатывает все типы плейсхолдеров
     */
    public Component processAllPlaceholders(CommandSender sender, Component component) {
        Map<String, ConfigManager.PlaceholderConfig> placeholders = configManager.getPlaceholderConfigs();
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);

        for (Map.Entry<String, ConfigManager.PlaceholderConfig> entry : placeholders.entrySet()) {
            String placeholderKey = entry.getKey();
            ConfigManager.PlaceholderConfig config = entry.getValue();

            if ("{command}".equals(placeholderKey)) {
                component = processCommandPlaceholders(sender, component, plainText, config);
            } else {
                String placeholder = "[" + placeholderKey + "]";
                if (plainText.contains(placeholder)) {
                    component = replaceCustomPlaceholder(component, placeholder, sender, config);
                }
            }
        }

        return component;
    }

    /**
     * Обрабатывает команды в формате [/command]
     */
    private Component processCommandPlaceholders(CommandSender sender, Component component, String plainText,
                                                 ConfigManager.PlaceholderConfig config) {
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
        String inventoryTitle = processPlaceholderText(config.getClickValue(), sender, value);

        // Создаем базовый компонент
        Component component = MessageManager.formatMessage(displayText);

        // Добавляем hover событие
        if (!hoverText.isEmpty()) {
            component = component.hoverEvent(
                    HoverEvent.showText(MessageManager.formatMessage(hoverText))
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
                ? MessageManager.formatMessage("Инвентарь " + player.getName())
                : MessageManager.formatMessage(inventoryTitle);

        // Создаём инвентарь с Component заголовком (Paper 1.19+)
        inv = Bukkit.createInventory(null, size, titleComponent);

        switch (type) {
            case SHOW_ITEM -> {
                var item = player.getInventory().getItemInMainHand();
                if (!item.getType().isAir()) inv.setItem(4, item.clone());
            }
            case SHOW_ENDER -> inv.setContents(player.getEnderChest().getContents());
            case SHOW_INV -> {
                // --- Ряд 1: броня + офф-рука ---
                ItemStack[] armor = player.getEquipment().getArmorContents(); // boots, leggings, chestplate, helmet
                if (armor.length == 4) {
                    inv.setItem(0, armor[3]); // helmet
                    inv.setItem(1, armor[2]); // chestplate
                    inv.setItem(2, armor[1]); // leggings
                    inv.setItem(3, armor[0]); // boots
                }
                inv.setItem(8, player.getInventory().getItemInOffHand()); // офф-рука

                // --- Ряд 2: панели ---
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) meta.displayName(MessageManager.formatMessage("<gray>"));
                filler.setItemMeta(meta);
                for (int i = 9; i < 18; i++) inv.setItem(i, filler);

                // --- Ряды 3-6: основной инвентарь + хотбар ---
                int targetSlot = 18;
                for (int i = 9; i < 36; i++) { // основной инвентарь
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null) inv.setItem(targetSlot, item.clone());
                    targetSlot++;
                }
                for (int i = 0; i < 9; i++) { // хотбар
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

        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
        } else {
            return "<green>" + formatItemName(item.getType().toString());
        }
    }

    private String formatItemName(String materialName) {
        return materialName.toLowerCase()
                .replace('_', ' ')
                .replace("minecraft:", "");
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
            viewer.sendMessage(MessageManager.formatMessage("<red>Этот просмотр инвентаря истёк."));
            return;
        }

        TimedInventory timedInv = invMap.get(type);
        if (timedInv == null) {
            viewer.sendMessage(MessageManager.formatMessage("<red>Этот просмотр инвентаря истёк."));
            return;
        }

        viewer.openInventory(timedInv.getInventory());
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
        long expiryMillis = 2 * 60 * 1000; // 2 минуты

        tempInventories.forEach((uuid, invMap) -> {
            invMap.entrySet().removeIf(entry -> now - entry.getValue().getTimestamp() > expiryMillis);
        });

        tempInventories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
