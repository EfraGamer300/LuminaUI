package dev.lumina.ui.storage;

import dev.lumina.ui.api.UiPage;
import dev.lumina.ui.core.PageValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads and saves pages as one YAML file per page inside a directory.
 *
 * <p>Files are named after the page id so the directory stays browsable. Loading is tolerant: a
 * broken file is logged and skipped so one bad page cannot take the whole server down.
 */
public final class YamlPageStore {

    private static final String EXTENSION = ".yml";

    private final Path directory;
    private final PageCodec codec = new PageCodec();
    private final SimpleYaml yaml = new SimpleYaml();
    private final AtomicFileWriter writer = new AtomicFileWriter();
    private final SchemaMigrator migrator = new SchemaMigrator();
    private final PageValidator validator;
    private final Logger logger;

    public YamlPageStore(Path directory, PageValidator validator, Logger logger) {
        this.directory = directory;
        this.validator = validator;
        this.logger = logger;
    }

    public Path directory() {
        return directory;
    }

    public List<UiPage> loadAll() {
        List<UiPage> pages = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return pages;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .sorted()
                    .forEach(path -> {
                        UiPage page = load(path);
                        if (page != null) {
                            pages.add(page);
                        }
                    });
        } catch (IOException failure) {
            logger.warning("Could not list page directory " + directory + ": " + failure.getMessage());
        }
        return pages;
    }

    public UiPage load(Path file) {
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            Map<String, Object> root = yaml.read(raw);
            String id = idFromFile(file);
            Map<String, Object> migrated = migrator.migrate(id, root);
            UiPage page = codec.decode(id, migrated);

            List<String> problems = validator.validate(page);
            if (!problems.isEmpty()) {
                logger.warning("Page '" + id + "' has problems, keeping it as-is: " + String.join("; ", problems));
            }
            return page;
        } catch (IOException failure) {
            logger.warning("Could not read page file " + file.getFileName() + ": " + failure.getMessage());
            return null;
        } catch (RuntimeException failure) {
            logger.warning("Invalid page file " + file.getFileName() + ": " + failure.getMessage());
            return null;
        }
    }

    public void save(UiPage page) throws IOException {
        List<String> problems = validator.validate(page);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("refusing to save invalid page '" + page.id() + "': "
                    + String.join("; ", problems));
        }
        Path target = directory.resolve(page.id() + EXTENSION);
        writer.writeUtf8(target, yaml.write(codec.encode(page)));
    }

    public boolean delete(String pageId) {
        try {
            return Files.deleteIfExists(directory.resolve(pageId + EXTENSION));
        } catch (IOException failure) {
            logger.warning("Could not delete page '" + pageId + "': " + failure.getMessage());
            return false;
        }
    }

    private static String idFromFile(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }
}
