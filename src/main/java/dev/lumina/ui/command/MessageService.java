package dev.lumina.ui.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads user-facing strings from {@code lang/<locale>.yml} and renders them as MiniMessage.
 *
 * <p>Keys are looked up with a fallback chain: the configured locale, then {@code en}, then the
 * bundled default. A missing key therefore produces readable output instead of an empty chat line.
 */
public final class MessageService {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final FileConfiguration locale;
    private final FileConfiguration fallback;
    private final String prefix;

    public MessageService(Plugin plugin, String localeName) {
        this.plugin = plugin;
        this.locale = loadLocale(localeName);
        this.fallback = loadLocale("en");
        this.prefix = locale.getString("prefix", fallback.getString("prefix", "<gradient:#7dd3fc:#a78bfa>LuminaUI</gradient> "));
    }

    private FileConfiguration loadLocale(String name) {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "lang/" + name + ".yml");
        if (file.isFile()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        try (InputStream stream = plugin.getResource("lang/" + name + ".yml")) {
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
        String template = locale.getString(key);
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
