package com.flyaway.chatmanager.managers;

import com.flyaway.chatmanager.ChatManagerPlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class LanguageManager {
    private final ChatManagerPlugin plugin;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Gson gson = new GsonBuilder().create();
    private final YamlConfiguration translations = new YamlConfiguration();

    public LanguageManager(ChatManagerPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        String lang = plugin.getConfigManager().getLanguage().toLowerCase();

        File file = new File(plugin.getDataFolder(), "translations/" + lang + ".yml");
        file.getParentFile().mkdirs();

        if (file.exists()) {
            try {
                translations.load(file);
                if (!translations.getKeys(false).isEmpty()) return;
            } catch (Exception ignored) {
            }
        }

        plugin.getLogger().info("Загрузка языка " + lang + "...");

        String version = plugin.getServer().getMinecraftVersion();
        String url = "https://api.github.com/repos/InventivetalentDev/minecraft-assets"
                + "/contents/assets/minecraft/lang/" + lang + ".json?ref=" + version;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject root = gson.fromJson(resp.body(), JsonObject.class);
            String base64Content = root.get("content").getAsString();

            JsonObject json = gson.fromJson(
                    new String(Base64Coder.decodeLines(base64Content)),
                    JsonObject.class
            );

            for (Map.Entry<String, JsonElement> e : json.entrySet()) {

                if (e.getKey().startsWith("item.minecraft.")) {
                    String name = e.getKey().replace("item.minecraft.", "");
                    if (name.contains(".")) continue;
                    translations.set("material." + name, e.getValue().getAsString());
                }

                if (e.getKey().startsWith("block.minecraft.")) {
                    String name = e.getKey().replace("block.minecraft.", "");
                    if (name.contains(".")) continue;
                    translations.set("material." + name, e.getValue().getAsString());
                }
            }

            translations.save(file);
            plugin.getLogger().info("Язык успешно загружен.");

        } catch (Exception ex) {
            plugin.getLogger().warning(ex.getMessage());
            plugin.getLogger().severe("Ошибка загрузки языка!");
        }
    }

    public Component translate(Material mat) {
        String key = mat.name().toLowerCase();
        String def = key.replace("_", " ");
        return miniMessage.deserialize(translations.getString("material." + key, def));
    }
}
