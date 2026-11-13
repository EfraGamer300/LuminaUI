package dev.lumina.ui.command;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Loads user-facing strings from {@code lang.yml} in the data folder.
 *
 * <p>Operators edit a single file, which is what the plugin installs on first run from the bundled
 * locale selected by the {@code lang} config key. Missing keys fall back to the bundled English
 * defaults, so an outdated {@code lang.yml} produces readable output instead of raw key names.
 */
public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final FileConfiguration messages;
    private final FileConfiguration fallback;
    private final String prefix;

    public MessageService(Plugin plugin, String localeName) {
        this.plugin = plugin;
        this.messages = loadLangFile();
        this.fallback = loadBundled(localeName);
        this.prefix = messages.getString("prefix",
                fallback.getString("prefix", "<gradient:#7dd3fc:#a78bfa>LuminaUI</gradient> "));
    }

    private FileConfiguration loadLangFile() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "lang.yml");
        if (file.isFile()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        return new YamlConfiguration();
    }

    private FileConfiguration loadBundled(String localeName) {
        FileConfiguration english = readBundled("en");
        if ("en".equalsIgnoreCase(localeName)) {
            return english;
        }
        FileConfiguration locale = readBundled(localeName);
        return locale.getKeys(true).isEmpty() ? english : locale;
    }

    private FileConfiguration readBundled(String name) {
        try (InputStream stream = plugin.getResource("locales/" + name + ".yml")) {
            if (stream != null) {
                return YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException failure) {
            plugin.getLogger().warning("Could not read bundled locale " + name + ": " + failure.getMessage());
        }
        return new YamlConfiguration();
    }

    public String raw(String key, String... placeholders) {
        String template = messages.getString(key);
        if (template == null) {
            template = fallback.getString(key, "<red>missing message: " + key);
        }
        return applyPlaceholders(template, placeholders);
    }

    public Component component(String key, String... placeholders) {
        return MINI.deserialize(raw(key, placeholders));
    }

    public void send(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public void sendPrefixed(CommandSender sender, String key, String... placeholders) {
        sender.sendMessage(MINI.deserialize(prefix + raw(key, placeholders)));
    }

    public void broadcast(String key, String... placeholders) {
        plugin.getServer().sendMessage(component(key, placeholders));
    }

    private static String applyPlaceholders(String template, String... placeholders) {
        String result = template;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            result = result.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return result;
    }
}
