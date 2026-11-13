package dev.lumina.ui.command;

import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * One {@code /lumina <name>} branch.
 */
public interface SubCommand {

    String name();

    default List<String> aliases() {
        return List.of();
    }

    default String permission() {
        return "luminaui.command." + name();
    }

    default String usage() {
        return "/lumina " + name();
    }

    boolean execute(CommandSender sender, String[] args);

    default List<String> complete(CommandSender sender, String[] args) {
        return List.of();
    }
}
