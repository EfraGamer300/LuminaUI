package dev.lumina.ui.command.subcommands;

import dev.lumina.ui.LuminaPlugin;
import dev.lumina.ui.command.MessageService;
import dev.lumina.ui.command.SubCommand;
import dev.lumina.ui.session.PageService;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * Runtime diagnostics: perf counters and internal state.
 */
public final class DebugCommand implements SubCommand {

    private final LuminaPlugin plugin;
    private final PageService pageService;
    private final MessageService messages;

    public DebugCommand(LuminaPlugin plugin, PageService pageService, MessageService messages) {
        this.plugin = plugin;
        this.pageService = pageService;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String usage() {
        return "/lumina debug <perf|state>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("perf")) {
            PageService.PerfSnapshot perf = pageService.perfSnapshot();
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gray>sessions=<white>" + perf.sessions()
                            + " <gray>elements=<white>" + perf.elements()
                            + " <gray>dirty=<white>" + perf.dirtyNodes()
                            + " <gray>editors=<white>" + plugin.editorService().openCount()
                            + " <gray>jobs=<white>" + plugin.autoworker().pending()));
            return true;
        }
        if (args[0].equalsIgnoreCase("state")) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gray>pages=<white>" + countPages()
                            + " <gray>item-cache=<white>" + plugin.itemRenderer().cacheSize()
                            + " <gray>folia=<white>" + plugin.scheduler().isFolia()));
            return true;
        }
        messages.send(sender, "command.usage", "usage", usage());
        return true;
    }

    private int countPages() {
        return plugin.registry().pageCount();
    }

    @Override
    public List<String> complete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("perf", "state").stream().filter(option -> option.startsWith(args[0])).toList();
        }
        return List.of();
    }
}
