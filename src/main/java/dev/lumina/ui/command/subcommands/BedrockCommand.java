package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.bedrock.BedrockExporter;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.core.UiRegistry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Exports pages to Bedrock form definitions.
 */
public final class BedrockCommand implements SubCommand {

    private final LuminaPlugin plugin;
    private final UiRegistry registry;
    private final MessageService messages;

    public BedrockCommand(LuminaPlugin plugin, UiRegistry registry, MessageService messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "bedrock";
    }

    @Override
    public String usage() {
        return "/lumina bedrock <build [page]|status>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }
        if (args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gray>" + plugin.bedrockService().status()));
            return true;
        }
        if (!args[0].equalsIgnoreCase("build")) {
            messages.send(sender, "command.usage", "usage", usage());
            return true;
        }

        if (!plugin.bedrockService().enabled()) {
            messages.send(sender, "bedrock.disabled");
            return true;
        }

        try {
            if (args.length > 1) {
                String pageId = args[1];
                if (registry.page(pageId) == null) {
                    messages.send(sender, "page.not-found", "page", pageId);
                    return true;
                }
                BedrockExporter.ExportResult result = plugin.bedrockService().export(pageId);
                sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gray>Exported <white>" + pageId + "</white> with <white>"
                                + result.widgetCount() + "</white> widget(s)"));
                if (!result.unsupportedNodes().isEmpty()) {
                    sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                            .deserialize("<yellow>Not representable on Bedrock: <white>"
                                    + String.join(", ", result.unsupportedNodes())));
                }
                return true;
            }

            List<Path> written = plugin.bedrockService().exportAll();
            List<String> names = new ArrayList<>();
            for (Path path : written) {
                names.add(path.getFileName().toString());
            }
            messages.sendPrefixed(sender, "bedrock.built",
                    "count", String.valueOf(registry.pageIds().spliterator().getExactSizeIfKnown()),
                    "files", names.isEmpty() ? "not deployed (packs folder unset)" : String.join(", ", names));
            return true;
        } catch (IOException failure) {
            plugin.reportFailure("exporting Bedrock forms", failure);
            messages.send(sender, "bedrock.export-failed", "error", String.valueOf(failure.getMessage()));
            return true;
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("build", "status").stream().filter(option -> option.startsWith(args[0])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("build")) {
            List<String> ids = new ArrayList<>();
            for (String id : registry.pageIds()) {
                if (id.startsWith(args[1])) {
                    ids.add(id);
                }
            }
            return ids;
        }
        return List.of();
    }
}
