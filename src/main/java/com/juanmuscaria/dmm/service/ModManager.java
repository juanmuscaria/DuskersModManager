package com.juanmuscaria.dmm.service;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.juanmuscaria.dmm.data.ModEntry;
import com.juanmuscaria.dmm.data.ModList;
import com.juanmuscaria.dmm.data.ModMetadata;
import com.juanmuscaria.dmm.util.DuskersHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.SetChangeListener;
import lombok.Getter;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

@Singleton
@ReflectiveAccess
public class ModManager {
    public static final String MOD_LIST_FILE = "mods.toml";
    private static final Logger logger = LoggerFactory.getLogger(ModManager.class);
    private final ObjectMapper MAPPER = new TomlMapper();
    private final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();
    private final Tika TIKA = new Tika();
    private final @Getter Path duskersDir;
    private final @Getter Path managerDir;
    private final @Getter Path modsDir;
    private final @Getter Path patchersDir;
    private final @Getter Path pluginsDir;
    private final @Getter Path patchedAssembliesDir;
    private final AtomicBoolean requiresSaving = new AtomicBoolean();
    private final ObjectProperty<ModList> modList = new SimpleObjectProperty<>(new ModList());
    private WatchService modChangeService;

    public ModManager() {
        duskersDir = DuskersHelper.getSelfPath().getParent().toAbsolutePath();
        managerDir = duskersDir.resolve("BepInEx/ModManager");
        modsDir = managerDir.resolve("mods");
        patchersDir = duskersDir.resolve("BepInEx/patchers");
        pluginsDir = duskersDir.resolve("BepInEx/plugins");
        patchedAssembliesDir = patchersDir.resolve(DuskersHelper.ASSEMBLY_PATCHER_PATH).resolve("replace");
        logger.info("Mod Manager files will reside in {}", managerDir);
        modList.addListener((observable, oldValue, newValue) ->
            newValue.getMods().addListener((SetChangeListener<ModEntry>) change -> {
                logger.debug("Change to mod list detected! Added {}, Removed {}.", change.getElementAdded(), change.getElementRemoved());
                requiresSaving.set(true);
            }));
    }

    public void loadOrCreateFiles() throws IOException {
        if (!Files.isDirectory(managerDir)) {
            Files.createDirectories(managerDir);
        }

        if (!Files.isDirectory(modsDir)) {
            Files.createDirectories(modsDir);
        }

        if (!Files.isDirectory(patchersDir)) {
            Files.createDirectories(patchersDir);
        }

        if (!Files.isDirectory(pluginsDir)) {
            Files.createDirectories(pluginsDir);
        }

        if (!Files.isDirectory(patchedAssembliesDir)) {
            Files.createDirectories(patchedAssembliesDir);
        }

        var modListPath = managerDir.resolve(MOD_LIST_FILE);
        if (Files.exists(modListPath)) {
            try {
                modList.set(MAPPER.readValue(Files.newInputStream(modListPath), ModList.class));
            } catch (Throwable e) {
                logger.warn("Unable to load mod list. Enabled mod status will be lost", e);
            }
        }

        updateModList();
        this.requiresSaving.set(true);

        try {
            modChangeService = FileSystems.getDefault().newWatchService();
            modsDir.register(modChangeService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (Exception e) {
            logger.warn("File watching is unsupported in your system! Manual changes to mods won't be detected without restart", e);
        }
    }

    public void updateModList() {
        var foundMods = new HashSet<ModEntry>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsDir)) {
            for (Path entry : stream) {
                try {
                    var mod = readMod(entry);
                    if (mod != null) {
                        foundMods.add(new ModEntry(entry.toAbsolutePath().toString(), mod, false));
                    }
                } catch (IOException e) {
                    logger.warn("Unable to parse mod {}", entry, e);
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to update the mod list", e);
        }

        modList.get().getMods().retainAll(foundMods);
        foundMods.removeAll(modList.get().getMods());

        for (ModEntry foundMod : foundMods) {
            this.getModlist().getMods().add(foundMod);
        }
    }

    public ModMetadata readMod(Path modPath) throws IOException {
        if (Files.isDirectory(modPath)) {
            var metadata = modPath.resolve("mod.toml");
            if (Files.isRegularFile(metadata)) {
                return MAPPER.readValue(Files.newInputStream(metadata), ModMetadata.class);
            }
        } else if (Files.isRegularFile(modPath)) {
            var mimeType = TIKA.detect(modPath);
            if (mimeType.equals("application/zip")) {
                try (var zipFile = new ZipFile(modPath.toFile())) {
                    var entry = zipFile.getEntry("mod.toml");
                    if (entry != null) {
                        return MAPPER.readValue(zipFile.getInputStream(entry), ModMetadata.class);
                    }
                }
            }
        } else {
            logger.warn("Tried to read {} but it does not exists!?", modPath);
        }
        return null;
    }

    @Scheduled(fixedRate = "1s")
    void pollFilesystemEvents() {
        if (modChangeService != null) {
            try {
                var key = modChangeService.poll(0, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        var path = modsDir.resolve(((Path) event.context())).toAbsolutePath();
                        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            try {
                                var metadata = readMod(path);
                                if (metadata != null) {
                                    Platform.runLater(() -> this.getModlist().getMods().add(new ModEntry(path.toString(), metadata, false)));
                                }
                            } catch (Exception e) {
                                logger.debug("Ignoring mods folder change, possible invalid mod file", e);
                            }
                        } else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            Platform.runLater(() -> this.getModlist().getMods().removeIf(modEntry -> modEntry.getModPath().equals(path.toString())));
                        }
                    }
                    key.reset(); // Continue listening to new events
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Pass up
            }
        }
    }

    @Scheduled(fixedRate = "1s")
    void saveModList() {
        if (this.requiresSaving.getAndSet(false)) {
            try {
                Files.writeString(managerDir.resolve(MOD_LIST_FILE),
                    WRITER.writeValueAsString(modList.get()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                logger.error("Unable to save mod list to disk!", e);
            }
        }
    }

    public ModList getModlist() {
        return this.modList.get();
    }

    public BooleanProperty makeModEnabledProperty(ModEntry entry) {
        var property = new SimpleBooleanProperty(entry.isEnabled());
        property.addListener((observable, oldValue, newValue) -> {
            entry.setEnabled(newValue);
            requiresSaving.set(true);
        });
        return property;
    }

    public void updateInstalledMods() throws IOException {
        var encoder = Base64.getUrlEncoder();
        var decoder = Base64.getUrlDecoder();
        var enabledMods = new DualHashBidiMap<ModEntry, String>();

        // Make sure the patcher is installed
        DuskersHelper.unpackAssemblyPatcher(patchersDir);
        FileUtils.cleanDirectory(patchedAssembliesDir.toFile());

        // Compute enabled mods
        for (ModEntry mod : this.getModlist().getMods()) {
            if (mod.isEnabled()) {
                var identifier = encoder.encodeToString(("dmmManagedMod:"+mod.getMetadata().id()).getBytes(StandardCharsets.UTF_8));
                if (enabledMods.containsValue(identifier)) {
                    logger.warn("{} is incompatible with {} and will be ignored!", mod, enabledMods.getKey(identifier));
                } else {
                    enabledMods.put(mod, identifier);
                }
            }
        }

        // First, we need to remove any non-enabled mods
        try (var modPaths = Stream.concat(Files.walk(patchersDir, 0), Files.walk(pluginsDir, 0))) {
            modPaths.parallel().filter(path -> {
                try {
                    var identifier = new String(decoder.decode(path.getFileName().toString()), StandardCharsets.UTF_8);
                    return identifier.startsWith("dmmManagedMod") && !enabledMods.containsValue(identifier);
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }).forEach(path -> {
                try {
                    FileUtils.deleteDirectory(path.toFile());
                } catch (IOException e) {
                    logger.error("Unable to delete mod {}", path, e);
                }
            });
        }

        // Now we unpack all enabled mods
        var osSpecificReplacePath = "replace/" + (SystemUtils.IS_OS_WINDOWS ? "windows" : "linux");
        for (Map.Entry<ModEntry, String> modEntry : enabledMods.entrySet()) {
            var modPath = Path.of(modEntry.getKey().getModPath());
            if (Files.isDirectory(modPath)) {
                var modReplaceDir = modPath.resolve(osSpecificReplacePath);
                var modPluginDir = modPath.resolve("plugin");
                var modPatcherDir = modPath.resolve("patcher");
                if (Files.isDirectory(modReplaceDir)) {
                    FileUtils.copyDirectory(modReplaceDir.toFile(), patchedAssembliesDir.toFile());
                }
                if (Files.isDirectory(modPluginDir)) {
                    FileUtils.copyDirectory(modPluginDir.toFile(), pluginsDir.resolve(modEntry.getValue()).toFile());
                }
                if (Files.isDirectory(modPatcherDir)) {
                    FileUtils.copyDirectory(modPatcherDir.toFile(), patchersDir.resolve(modEntry.getValue()).toFile());
                }
            } else if (Files.isRegularFile(modPath)) {
                var mimeType = TIKA.detect(modPath);
                if (mimeType.equals("application/zip")) {
                    //TODO: Prevent transversal path?
                    try (var zipFile = new ZipFile(modPath.toFile())) {
                        zipFile.entries().asIterator().forEachRemaining(entry -> {
                            try {
                                if (!entry.isDirectory()) {
                                    if (entry.getName().startsWith(osSpecificReplacePath)) {
                                        var targetPath = patchedAssembliesDir.resolve(entry.getName().substring(osSpecificReplacePath.length() + 1));
                                        Files.createDirectories(targetPath.getParent());
                                        Files.copy(zipFile.getInputStream(entry), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                    } else if (entry.getName().startsWith("plugin")) {
                                        var targetPath =  pluginsDir.resolve(modEntry.getValue()).resolve(entry.getName().substring(7));
                                        Files.createDirectories(targetPath.getParent());
                                        Files.copy(zipFile.getInputStream(entry), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                    } else if (entry.getName().startsWith("patcher")) {
                                        var targetPath = patchersDir.resolve(modEntry.getValue()).resolve(entry.getName().substring(8));
                                        Files.createDirectories(targetPath.getParent());
                                        Files.copy(zipFile.getInputStream(entry), targetPath, StandardCopyOption.REPLACE_EXISTING);
                                    } else {
                                        logger.debug("Ignored entry {}", entry.getName());
                                    }
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                    } catch (UncheckedIOException e) {
                        throw e.getCause(); // Ugh java lambdas
                    }
                }
            }
        }
    }
}
