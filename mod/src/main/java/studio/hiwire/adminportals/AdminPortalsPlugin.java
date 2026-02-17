package studio.hiwire.adminportals;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import lombok.Getter;
import org.bson.BsonDocument;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import studio.hiwire.adminportals.command.AdminPortalsCommand;
import studio.hiwire.adminportals.component.PortalConfigComponent;
import studio.hiwire.adminportals.configmode.ConfigurationModeManager;
import studio.hiwire.adminportals.interaction.AdminPortalInteraction;
import studio.hiwire.adminportals.placeholder.PlaceholderManager;
import studio.hiwire.adminportals.util.TranslationFileManager;

public class AdminPortalsPlugin extends JavaPlugin {

  private static AdminPortalsPlugin INSTANCE;
  public static final String PREFIX = "[HiWire:AdminPortals]";
  private static final Path OLD_OVERRIDES_PATH = Path.of("overrides");
  private static final Path OVERRIDES_MOD_PATH = Path.of("HiWire_AdminPortals_Overrides");
  private static final List<String> TRANSLATION_FILES =
      List.of(
          "HiWire.AdminPortals.Items.lang",
          "HiWire.AdminPortals.ChatMessages.lang",
          "HiWire.AdminPortals.UI.lang");
  private static final List<String> SUPPORTED_LANGUAGES = List.of("en-US", "de-DE");

  @Getter private ComponentType<ChunkStore, PortalConfigComponent> adminPortalConfigComponentType;
  @Getter private PlaceholderManager placeholderManager;
  @Getter private ConfigurationModeManager configurationModeManager;

  public AdminPortalsPlugin(@NonNullDecl JavaPluginInit init) throws IOException {
    super(init);
    createOverrideModFiles();
  }

  @Override
  protected void setup() {
    INSTANCE = this;
    placeholderManager = new PlaceholderManager();
    configurationModeManager = new ConfigurationModeManager(getEventRegistry());

    getCodecRegistry(Interaction.CODEC)
        .register(
            "HiWire_AdminPortals_PortalInteraction",
            AdminPortalInteraction.class,
            AdminPortalInteraction.CODEC);

    adminPortalConfigComponentType =
        getChunkStoreRegistry()
            .registerComponent(
                PortalConfigComponent.class,
                "HiWire_AdminPortals_PortalConfig",
                PortalConfigComponent.CODEC);

    getCommandRegistry()
        .registerCommand(new AdminPortalsCommand(placeholderManager, configurationModeManager));
  }

  @Override
  protected void shutdown() {
    configurationModeManager.shutdown();
  }

  public static AdminPortalsPlugin get() {
    return INSTANCE;
  }

  private void createOverrideModFiles() throws IOException {
    createOverrideModManifest();
    migrateOldTranslationFiles();
    mergeAllTranslations();
  }

  private void createOverrideModManifest() throws IOException {
    final var manifest = getManifest();

    final var overwriteManifest =
        new PluginManifest(
            manifest.getGroup(),
            "AdminPortals_Overrides",
            manifest.getVersion(),
            null,
            manifest.getAuthors(),
            manifest.getWebsite(),
            null,
            manifest.getServerVersion(),
            Map.of(getIdentifier(), SemverRange.WILDCARD),
            new HashMap<>(),
            new HashMap<>(),
            new ArrayList<>(),
            false);

    final var manifestJson =
        (BsonDocument) PluginManifest.CODEC.encode(overwriteManifest, new ExtraInfo());

    final var jsonWriterSettings =
        JsonWriterSettings.builder().indent(true).outputMode(JsonMode.RELAXED).build();

    final var overwriteManifestJsonString = manifestJson.toJson(jsonWriterSettings);

    final var overwriteManifestPath =
        getDataDirectory().getParent().resolve(OVERRIDES_MOD_PATH).resolve("manifest.json");
    Files.createDirectories(overwriteManifestPath.getParent());

    try (FileWriter fileWriter = new FileWriter(overwriteManifestPath.toFile())) {
      fileWriter.write(overwriteManifestJsonString);
    }
  }

  private void migrateOldTranslationFiles() throws IOException {
    // The old asset pack location was at
    // getPluginDir()/overrides/Server/Languages/{language}/*.lang
    // This needs to be moved to
    // getPluginDir().getParent()/HiWire_AdminPortals_Overrides/Server/Languages/{language}/*.lang
    for (String language : SUPPORTED_LANGUAGES) {
      final var oldLanguagePath =
          getDataDirectory()
              .resolve(OLD_OVERRIDES_PATH)
              .resolve("Server")
              .resolve("Languages")
              .resolve(language);
      final var newLanguagePath =
          getDataDirectory()
              .getParent()
              .resolve(OVERRIDES_MOD_PATH)
              .resolve("Server")
              .resolve("Languages")
              .resolve(language);
      Files.createDirectories(newLanguagePath);

      for (String file : TRANSLATION_FILES) {
        Path oldPath = oldLanguagePath.resolve(file);
        Path newPath = newLanguagePath.resolve(file);

        if (!Files.exists(oldPath)) {
          continue;
        }
        getLogger().at(Level.INFO).log("Migrating translation file " + file);

        try {
          Files.move(oldPath, newPath);
        } catch (FileAlreadyExistsException e) {
          getLogger()
              .at(Level.INFO)
              .log("File " + file + " already exists at new location, skipping");
        }
      }
    }

    if (Files.exists(getDataDirectory().resolve(OLD_OVERRIDES_PATH))) {
      FileUtil.deleteDirectory(getDataDirectory().resolve(OLD_OVERRIDES_PATH));
    }
  }

  private void mergeAllTranslations() {
    TranslationFileManager fileManager = new TranslationFileManager(getClass().getClassLoader());

    for (String language : SUPPORTED_LANGUAGES) {
      for (String file : TRANSLATION_FILES) {
        String resourcePath = String.format("Server/Languages/%s/%s", language, file);
        Path targetPath =
            getDataDirectory()
                .getParent()
                .resolve(OVERRIDES_MOD_PATH)
                .resolve("Server")
                .resolve("Languages")
                .resolve(language)
                .resolve(file);

        TranslationFileManager.MergeResult result = fileManager.merge(resourcePath, targetPath);

        Level level = result.isSuccess() ? Level.INFO : Level.WARNING;
        if (result.status() != TranslationFileManager.MergeResult.Status.NO_CHANGES) {
          getLogger().at(level).log(result.message());
        }
      }
    }
  }
}
