package dev.lumina.ui.script;

import dev.lumina.ui.LuminaPlugin;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

/**
 * {@link ScriptHost} backed by a Bukkit player.
 *
 * <p>Each handler run gets its own host, so the cancel flag and the acting player never leak between
 * runs. Placeholders are resolved locally for the common player keys; external placeholder plugins
 * can be plugged in later behind the same method.
 */
public final class BukkitScriptHost implements ScriptHost {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final LuminaPlugin plugin;
    private final Player player;
    private final String pageId;
    private final String elementId;
    private boolean cancelled;

    public BukkitScriptHost(LuminaPlugin plugin, Player player, String pageId, String elementId) {
        this.plugin = plugin;
        this.player = player;
        this.pageId = pageId;
        this.elementId = elementId;
    }

    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public String placeholder(String name) {
        if (player == null) {
            return "";
        }
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "player", "player_name", "playername" -> player.getName();
            case "uuid" -> player.getUniqueId().toString();
            case "world" -> player.getWorld().getName();
            case "x" -> String.valueOf(Math.round(player.getLocation().getX()));
            case "y" -> String.valueOf(Math.round(player.getLocation().getY()));
            case "z" -> String.valueOf(Math.round(player.getLocation().getZ()));
            case "page" -> pageId == null ? "" : pageId;
            case "element" -> elementId == null ? "" : elementId;
            default -> "%" + name + "%";
        };
    }

    @Override
    public void message(String text) {
        if (player != null) {
            player.sendMessage(MINI.deserialize(text));
        } else {
            plugin.getServer().sendMessage(MINI.deserialize(text));
        }
    }

    @Override
    public void actionBar(String text) {
        if (player != null) {
            player.sendActionBar(MINI.deserialize(text));
        }
    }

    @Override
    public void title(String text) {
        if (player != null) {
            player.showTitle(Title.title(MINI.deserialize(text), net.kyori.adventure.text.Component.empty()));
        }
    }

    @Override
    public void command(String line) {
        if (player == null) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), stripSlash(line));
        } else {
            player.getServer().dispatchCommand(player, stripSlash(line));
        }
    }

    @Override
    public void openPage(String pageId) {
        if (player == null) {
            return;
        }
        org.bukkit.Location origin = player.getEyeLocation()
                .add(player.getLocation().getDirection().multiply(3d));
        plugin.pageService().open(player, pageId, origin);
    }

    @Override
    public void closePage() {
        if (player != null) {
            plugin.pageService().close(player.getUniqueId());
        }
    }

    @Override
    public void sound(String key) {
        if (player == null) {
            return;
        }
        String sound = key == null ? "" : key.trim();
        if (sound.isEmpty()) {
            return;
        }
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    @Override
    public void log(String text) {
        plugin.getLogger().info("[script] " + text);
    }

    @Override
    public void cancelEvent(boolean value) {
        this.cancelled = value;
    }

    @Override
    public void after(long ticks, Runnable continuation) {
        if (player == null) {
            plugin.scheduler().nextTick(continuation);
        } else {
            plugin.scheduler().runLaterForPlayer(player, ticks, continuation);
        }
    }

    @Override
    public void unsupported(String verb, String argument) {
        plugin.getLogger().fine(() -> "script used unsupported verb '" + verb + "' with '" + argument + "'");
    }

    @Override
    public String playerName() {
        return player == null ? "console" : player.getName();
    }

    @Override
    public String pageId() {
        return pageId == null ? "" : pageId;
    }

    @Override
    public String elementId() {
        return elementId == null ? "" : elementId;
    }

    /** Variables exposed to scripts, mainly for tests and future expansion. */
    public Map<String, Object> variables() {
        return Map.of("player", playerName(), "page", pageId(), "element", elementId());
    }

    private static String stripSlash(String line) {
        String value = line == null ? "" : line.trim();
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
