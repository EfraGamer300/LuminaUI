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
    private dev.lumina.ui.script.ScriptService scriptService;

    private static final String[] DATA_DIRECTORIES =
            dev.lumina.ui.core.LuminaLayout.DATA_DIRECTORIES.toArray(new String[0]);

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
        this.pageService = new PageService(this, registry, settings, scheduler, textRenderer, itemRenderer);
        this.editorService = new dev.lumina.ui.editor.EditorService(
                this, registry, pageService, pageStore, () -> settings);
        this.bedrockService = new dev.lumina.ui.bedrock.BedrockService(this, registry, settings);
        this.editorMenu = new dev.lumina.ui.editor.EditorMenu(this);
        this.scriptService = new dev.lumina.ui.script.ScriptService(this);

        installDataLayout();
        loadPages();
        registerCommands();
        registerListeners();
        this.scriptService.start();
        reportScripts(this.scriptService.reload());

        getLogger().info(() -> "LuminaUI " + getPluginMeta().getVersion() + " enabled"
                + (scheduler.isFolia() ? " (Folia)" : ""));
    }

    @Override
    public void onDisable() {
        if (scriptService != null) {
            scriptService.close();
        }
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
        registry.clearPages();
        installDataLayout();
        loadPages();
        pageService.closeMissingPages();
        reportScripts(scriptService.reload());
    }

    /** Logs the result of a script reload, keeping parse errors visible without spamming the console. */
    public void reportScripts(dev.lumina.ui.script.ScriptService.LoadReport report) {
        getLogger().info(() -> "Loaded " + report.loaded().size() + " script(s) with "
                + report.handlers() + " handler(s)");
        for (String error : report.errors()) {
            getLogger().warning("Script error: " + error);
        }
    }

    private void loadPages() {
        List<UiPage> pages = pageStore.loadAll();
        for (UiPage page : pages) {
            registry.registerPage(page);
        }
        getLogger().info(() -> "Loaded " + pages.size() + " page(s)");
    }

    /**
     * Creates the data folder layout on first run and installs the bundled defaults.
     *
     * <p>The layout mirrors the one operators already know: pages, components, effects, images,
     * scripts and world placements live under {@code contents/}, editor templates under
     * {@code editor-defaults/}, and pack output under {@code resourcepack/} and {@code bedrock/}.
     * Directories are created even when a feature is not used yet, so an operator can drop files in
     * place before the corresponding command exists.
     */
    private void installDataLayout() {
        for (String directory : DATA_DIRECTORIES) {
            try {
                java.nio.file.Files.createDirectories(getDataFolder().toPath().resolve(directory));
            } catch (java.io.IOException failure) {
                reportFailure("creating data directory " + directory, failure);
            }
        }
        installIfMissing(dev.lumina.ui.core.LuminaLayout.PAGES + "/example.yml");
        installLangFile();
    }

    private void installIfMissing(String resourcePath) {
        if (java.nio.file.Files.isRegularFile(getDataFolder().toPath().resolve(resourcePath))) {
            return;
        }
        try {
            saveResource(resourcePath, false);
        } catch (RuntimeException failure) {
            reportFailure("installing " + resourcePath, failure);
        }
    }

    /**
     * Writes {@code lang.yml} from the bundled locale selected by the {@code lang} config key.
     * A single language file is what operators edit; the bundled locales stay internal.
     */
    private void installLangFile() {
        java.nio.file.Path target = getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.LANG_FILE);
        if (java.nio.file.Files.isRegularFile(target)) {
            return;
        }
        String locale = getConfig().getString("lang", "en");
        String resource = "locales/" + locale + ".yml";
        if (getResource(resource) == null) {
            resource = "locales/en.yml";
        }
        try (java.io.InputStream stream = getResource(resource)) {
            if (stream == null) {
                return;
            }
            java.nio.file.Files.write(target, stream.readAllBytes());
        } catch (java.io.IOException failure) {
            reportFailure("installing lang.yml", failure);
        }
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
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.PAGES);
    }

    public Path componentsDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.COMPONENTS);
    }

    public Path effectsDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.EFFECTS);
    }

    public Path imagesDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.IMAGES);
    }

    public Path scriptsDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.SCRIPTS);
    }

    public Path worldDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.WORLD);
    }

    public Path editorDefaultsDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.EDITOR_DEFAULTS);
    }

    public Path resourcePackDirectory() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.RESOURCE_PACK);
    }

    /** {@code lang.yml} in the data folder, or {@code null} when it was removed. */
    public Path langFile() {
        return getDataFolder().toPath().resolve(dev.lumina.ui.core.LuminaLayout.LANG_FILE);
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

    public dev.lumina.ui.script.ScriptService scriptService() {
        return scriptService;
    }

    /** Logs a failure without letting a rendering problem take the plugin down. */
    public void reportFailure(String what, Throwable failure) {
        getLogger().log(Level.WARNING, "LuminaUI: " + what, failure);
    }
}
