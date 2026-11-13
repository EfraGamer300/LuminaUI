package dev.lumina.ui;

import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.command.CommandRouter;
import dev.lumina.ui.compat.SchedulerBridge;
import dev.lumina.ui.core.PageValidator;
import dev.lumina.ui.core.Settings;
import dev.lumina.ui.core.UiRegistry;
import dev.lumina.ui.editor.Autoworker;
import dev.lumina.ui.render.ItemRenderer;
import dev.lumina.ui.render.TextRenderer;
import dev.lumina.ui.session.PageService;
import dev.lumina.ui.storage.YamlPageStore;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point. Wires the engine together and owns its lifecycle; all behaviour lives in the
 * services it creates, so this class stays a thin composition root.
 */
public final class LuminaPlugin extends JavaPlugin {

    private static LuminaPlugin instance;

    private Settings settings;
    private UiRegistry registry;
    private SchedulerBridge scheduler;
    private YamlPageStore pageStore;
    private PageService pageService;
    private TextRenderer textRenderer;
    private ItemRenderer itemRenderer;
    private Autoworker autoworker;
    private dev.lumina.ui.editor.EditorService editorService;
    private dev.lumina.ui.editor.EditorMenu editorMenu;
    private dev.lumina.ui.bedrock.BedrockService bedrockService;

    public static LuminaPlugin instance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        saveDefaultConfig();
        this.settings = Settings.from(getConfig());
    }

    @Override
    public void onEnable() {
        this.scheduler = new SchedulerBridge(this, SchedulerBridge.detectFolia());
        this.registry = new UiRegistry();
        this.textRenderer = new TextRenderer();
        this.itemRenderer = new ItemRenderer();
        this.autoworker = new Autoworker(this, settings);

        this.pageStore = new YamlPageStore(
                pagesDirectory(),
                new PageValidator(settings.maxNodesPerPage()),
                getLogger());
        this.pageService = new PageService(this, registry, settings, textRenderer, itemRenderer);
        this.editorService = new dev.lumina.ui.editor.EditorService(
                this, registry, pageService, pageStore, settings);
        this.bedrockService = new dev.lumina.ui.bedrock.BedrockService(this, registry, settings);
        this.editorMenu = new dev.lumina.ui.editor.EditorMenu(this);

        loadPages();
        registerCommands();
        registerListeners();

        getLogger().info(() -> "LuminaUI " + getPluginMeta().getVersion() + " enabled"
                + (scheduler.isFolia() ? " (Folia)" : ""));
    }

    @Override
    public void onDisable() {
        if (editorService != null) {
            editorService.flushAll();
        }
        if (pageService != null) {
            pageService.closeAll();
        }
        if (autoworker != null) {
            autoworker.shutdown();
        }
        instance = null;
    }

    /** Reloads config and page definitions, closing open sessions that no longer exist. */
    public void reloadEverything() {
        reloadConfig();
        this.settings = Settings.from(getConfig());
        loadPages();
        pageService.closeMissingPages();
    }

    private void loadPages() {
        List<UiPage> pages = pageStore.loadAll();
        for (UiPage page : pages) {
            registry.registerPage(page);
        }
        getLogger().info(() -> "Loaded " + pages.size() + " page(s)");
    }

    private void registerCommands() {
        PluginCommand command = getCommand("lumina");
        if (command == null) {
            getLogger().severe("Command /lumina is missing from plugin.yml");
            return;
        }
        CommandRouter router = new CommandRouter(this, registry, pageService, pageStore);
        command.setExecutor(router);
        command.setTabCompleter(router);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(pageService, this);
        getServer().getPluginManager().registerEvents(editorMenu, this);
    }

    public Path pagesDirectory() {
        return getDataFolder().toPath().resolve("pages");
    }

    public Settings settings() {
        return settings;
    }

    public UiRegistry registry() {
        return registry;
    }

    public SchedulerBridge scheduler() {
        return scheduler;
    }

    public PageService pageService() {
        return pageService;
    }

    public YamlPageStore pageStore() {
        return pageStore;
    }

    public TextRenderer textRenderer() {
        return textRenderer;
    }

    public ItemRenderer itemRenderer() {
        return itemRenderer;
    }

    public Autoworker autoworker() {
        return autoworker;
    }

    public dev.lumina.ui.editor.EditorService editorService() {
        return editorService;
    }

    public dev.lumina.ui.editor.EditorMenu editorMenu() {
        return editorMenu;
    }

    public dev.lumina.ui.bedrock.BedrockService bedrockService() {
        return bedrockService;
    }

    /** Logs a failure without letting a rendering problem take the plugin down. */
    public void reportFailure(String what, Throwable failure) {
        getLogger().log(Level.WARNING, "LuminaUI: " + what, failure);
    }
}
