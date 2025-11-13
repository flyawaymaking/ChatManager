package com.flyaway.chatmanager.commands;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.flyaway.chatmanager.managers.ConfigManager;
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
import java.util.UUID;

public class ChatCommand implements CommandExecutor, TabCompleter {

    private final ChatManagerPlugin plugin;
    private final ConfigManager configManager;

    public ChatCommand(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
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
            case "help":
                sendHelp(sender);
                break;
            case "info":
                infoCommand(sender);
                break;
            default:
                MessageManager.sendUnknownCommand(sender);
                break;
        }

        if (!(sender instanceof Player player)) return true;
        if (args.length != 2) return true;

        UUID targetUUID = UUID.fromString(args[0]);
        PlaceholderProcessor.ClickType type = PlaceholderProcessor.ClickType.valueOf(args[1].toUpperCase());

        plugin.getPlaceholderProcessor().openInventoryGUI(player, targetUUID, type);
        return true;
    }

    private void reloadCommand(CommandSender sender) {
        if (!sender.hasPermission("chatmanager.reload")) {
            MessageManager.sendNoPermission(sender);
            return;
        }

        try {
            configManager.reloadConfig();
            MessageManager.sendReloadSuccess(sender);
        } catch (Exception e) {
            MessageManager.sendReloadError(sender, e.getMessage());
            plugin.getLogger().severe("Ошибка при перезагрузке конфигурации: " + e.getMessage());
        }
    }

    private void infoCommand(CommandSender sender) {
        String info = """
                <gradient:gold:yellow>ChatManager v%s</gradient>
                <gray>Автор: <white>%s</white>
                <gray>Версия API: <white>%s</white>
                <gray>Радиус локального чата: <white>%d блоков</white>
                <gray>Формат сообщения: <white>%s</white>"""
                .formatted(
                        plugin.getPluginMeta().getVersion(),
                        String.join(", ", plugin.getPluginMeta().getAuthors()),
                        plugin.getPluginMeta().getAPIVersion(),
                        configManager.getLocalChatRadius(),
                        configManager.getMessageFormat()
                );

        MessageManager.sendMessage(sender, info);
    }

    private void sendHelp(CommandSender sender) {
        String help = """
                <gradient:gold:yellow>ChatManager - Помощь по командам</gradient>
                <gray>/chatmanager help</gray> - <white>Показать это сообщение</white>
                <gray>/chatmanager reload</gray> - <white>Перезагрузить конфигурацию</white>
                <gray>/chatmanager info</gray> - <white>Информация о плагине</white>
                
                <gray>Использование чата:</gray>
                <gray>- Обычное сообщение</gray> - <white>локальный чат</white>
                <gray>- !сообщение</gray> - <white>глобальный чат</white>""";

        MessageManager.sendMessage(sender, help);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("help");
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
