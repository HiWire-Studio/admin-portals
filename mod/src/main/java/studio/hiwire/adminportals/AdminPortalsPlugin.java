package studio.hiwire.adminportals;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
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
  private static final Path OVERRIDES_PATH = Path.of("overrides");
  private static final List<String> TRANSLATION_FILES =
      List.of(
          "HiWire.AdminPortals.Items.lang",
          "HiWire.AdminPortals.ChatMessages.lang",
          "HiWire.AdminPortals.UI.lang");
  private static final List<String> SUPPORTED_LANGUAGES = List.of("en-US", "de-DE");

  @Getter private ComponentType<ChunkStore, PortalConfigComponent> adminPortalConfigComponentType;
  @Getter private PlaceholderManager placeholderManager;
  @Getter private ConfigurationModeManager configurationModeManager;

  public AdminPortalsPlugin(@NonNullDecl JavaPluginInit init) {
    super(init);
    // Merge default translations with user's overrides for all languages
    mergeAllTranslations();
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
  protected void start0() {
    // Register overrides folder as asset pack for user customization
    // Must be registered before super.start0() so overrides take precedence over the main pack
    Path overridesDir = getDataDirectory().resolve(OVERRIDES_PATH);
    if (Files.exists(overridesDir)) {
      AssetModule.get().registerPack(getIdentifier() + "_overrides", overridesDir, getManifest());
      getLogger().at(Level.INFO).log("Registered overrides asset pack from %s", overridesDir);
    }

    super.start0();
  }

  @Override
  protected void shutdown() {
    configurationModeManager.shutdown();
  }

  public static AdminPortalsPlugin get() {
    return INSTANCE;
  }

  private void mergeAllTranslations() {
    TranslationFileManager fileManager = new TranslationFileManager(getClass().getClassLoader());

    for (String language : SUPPORTED_LANGUAGES) {
      for (String file : TRANSLATION_FILES) {
        String resourcePath = String.format("Server/Languages/%s/%s", language, file);
        Path targetPath =
            getDataDirectory()
                .resolve(OVERRIDES_PATH)
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
