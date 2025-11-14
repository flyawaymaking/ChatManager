package com.flyaway.chatmanager.commands;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
import com.flyaway.chatmanager.managers.MentionManager;
import com.flyaway.chatmanager.managers.MessageManager;
import com.flyaway.chatmanager.managers.PlaceholderProcessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChatCommand implements CommandExecutor, TabCompleter {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final MentionManager mentionManager;

    public ChatCommand(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.messageManager = plugin.getMessageManager();
        this.mentionManager = plugin.getMentionManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadCommand(sender);
                break;
            case "placeholders":
                placeholdersCommand(sender);
                break;
            case "colors":
                colorsCommand(sender);
                break;
            case "mentiontoggle":
                mentionToggleCommand(sender);
                return true;
            case "help":
                sendHelp(sender);
                break;
            case "info":
                infoCommand(sender);
                break;
            case "openinv":
                if (args.length != 3) return true; // теперь ожидаем 3 аргумента: openinv, UUID, type
                openInvCommand(sender, args);
                break;
            default:
                messageManager.sendUnknownCommand(sender);
                break;
        }

        return true;
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("chatmanager.reload")) {
            messageManager.sendNoPermission(sender);
            return;
        }

        try {
            configManager.reloadConfig();
            messageManager.sendReloadSuccess(sender);
        } catch (Exception e) {
            messageManager.sendReloadError(sender, e.getMessage());
            plugin.getLogger().severe("Ошибка при перезагрузке конфигурации: " + e.getMessage());
        }
    }

    private void colorsCommand(CommandSender sender) {
        messageManager.sendMessage(sender, configManager.getMessage("colors"));
    }

    private void openInvCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return;

        try {
            UUID targetUUID = UUID.fromString(args[1]); // args[1] — UUID
            PlaceholderProcessor.ClickType type = PlaceholderProcessor.ClickType.valueOf(args[2].toUpperCase());
            plugin.getPlaceholderProcessor().openInventoryGUI(player, targetUUID, type);
        } catch (IllegalArgumentException e) {
            messageManager.sendUnknownCommand(sender);
        }
    }

    private void mentionToggleCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) return;
        boolean isEnabled = mentionManager.isMentionEnabled(player);
        mentionManager.setMentionEnabled(player, !isEnabled);

        String status = isEnabled ? configManager.getMessage("player-mention-status-enabled") : configManager.getMessage("player-mention-status-disabled");
        messageManager.sendMessage(sender, configManager.getMessage("player-mention-text").replace("{status}", status));
    }

    private void placeholdersCommand(CommandSender sender) {
        var placeholders = configManager.getPlaceholderConfigs();

        if (placeholders.isEmpty()) {
            messageManager.sendMessage(sender, configManager.getMessage("placeholders-empty"));
            return;
        }

        // Строим одно итоговое сообщение
        StringBuilder builder = new StringBuilder();

        // Заголовок
        builder.append(configManager.getMessage("placeholders-header")).append("\n");

        ConfigManager.PlaceholderConfig commandConfig = configManager.getCommandConfig();
        if (commandConfig != null) {
            String line = configManager.getMessage("placeholder-line").replace("{key}", "/command").replace("{description}", commandConfig.getDescription());
            builder.append(line).append("\n");
        }

        // Линии с плейсхолдерами
        for (Map.Entry<String, ConfigManager.PlaceholderConfig> entry : placeholders.entrySet()) {

            String key = entry.getKey();
            ConfigManager.PlaceholderConfig ph = entry.getValue();

            String line = configManager.getMessage("placeholder-line").replace("{key}", key).replace("{description}", ph.getDescription());
            builder.append(line).append("\n");
        }

        // Отправляем одно сообщение
        messageManager.sendMessage(sender, builder.toString());
    }

    private void infoCommand(CommandSender sender) {
        String apiVersion = plugin.getPluginMeta().getAPIVersion();
        if (apiVersion == null) {
            apiVersion = "unknown";
        }

        String info = configManager.getMessage("info")
                .replace("{version}", plugin.getPluginMeta().getVersion())
                .replace("{authors}", String.join(", ", plugin.getPluginMeta().getAuthors()))
                .replace("{api}", apiVersion)
                .replace("{radius}", String.valueOf(configManager.getLocalChatRadius()))
                .replace("{format}", configManager.getMessageFormat());

        messageManager.sendMessage(sender, info);
    }

    private void sendHelp(CommandSender sender) {
        String help = configManager.getMessage("help");

        messageManager.sendMessage(sender, help);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
            completions.add("placeholders");
            completions.add("colors");
            completions.add("mentiontoggle");
            completions.add("reload");
            completions.add("info");

            // Фильтруем по введенному тексту
            return completions.stream()
                    .filter(completion -> completion.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return completions;
    }
}
