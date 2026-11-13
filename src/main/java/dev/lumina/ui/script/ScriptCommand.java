package dev.lumina.ui.script;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Registers a script-declared {@code command} handler with the server command map.
 */
public final class ScriptCommand extends org.bukkit.command.Command {

    private final Handler handler;

    public ScriptCommand(String name, String permission, String description, Handler handler) {
        super(name, description == null ? "" : description, "/" + name, List.of());
        this.handler = handler;
        if (permission != null && !permission.isBlank()) {
            setPermission(permission);
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        handler.run(sender, args);
        return true;
    }

    /** Callback invoked when the script command is used. */
    @FunctionalInterface
    public interface Handler {
        void run(CommandSender sender, String[] args);
    }
}
