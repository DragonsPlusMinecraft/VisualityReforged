package plus.dragons.visuality.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A JSON-based config which can be loaded from the config directory and resource packs.
 */
public abstract class ReloadableJsonConfig extends SimplePreparableReloadListener<List<Pair<String, JsonObject>>> {
    private static final Map<ResourceLocation, ReloadableJsonConfig> CONFIGS = new HashMap<>();
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected final ResourceLocation id;
    protected final Path path;
    protected final Logger logger;
    @Nullable
    private JsonObject config;
    private boolean configLoadFailed;

    protected ReloadableJsonConfig(ResourceLocation id) {
        this.id = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + ".json");
        this.path = FMLPaths.CONFIGDIR.get().resolve(this.id.getNamespace()).resolve(this.id.getPath());
        this.logger = LoggerFactory.getLogger(this.getClass());
        CONFIGS.put(id, this);
    }

    @Override
    protected List<Pair<String, JsonObject>> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.startTick();
        profiler.push("config");
        profiler.push("parse");
        configLoadFailed = false;
        config = loadConfig();
        profiler.pop();
        profiler.pop();

        List<Pair<String, JsonObject>> list = new ArrayList<>();
        try {
            for (String namespace : resourceManager.getNamespaces()) {
                profiler.push(namespace);
                ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(namespace, this.id.getPath());
                for (Resource resource : resourceManager.getResourceStack(resourceId)) {
                    profiler.push(resource.sourcePackId());
                    try (Reader reader = resource.openAsReader()) {
                        profiler.push("parse");
                        try {
                            JsonObject object = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                            list.add(Pair.of(resource.sourcePackId() + '#' + resourceId, object));
                        } finally {
                            profiler.pop();
                        }
                    } catch (RuntimeException exception) {
                        logger.warn("Invalid {} in resource pack '{}'", resourceId, resource.sourcePackId(), exception);
                    }
                    profiler.pop();
                }
                profiler.pop();
            }
        } catch (IOException exception) {
            logger.warn("Failed to enumerate resource-pack configs for {}", id, exception);
        }
        profiler.endTick();
        return list;
    }

    @Override
    protected void apply(List<Pair<String, JsonObject>> list, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.startTick();
        resetRuntimeData();
        JsonObject configToSave = null;

        if (configLoadFailed) {
            logger.error("Keeping invalid config at {} unchanged; fix the error above and reload resources", path);
        } else if (config == null) {
            configToSave = serializeConfig();
        } else {
            try {
                JsonObject invalidConfig = apply(config, true, path.toString(), profiler);
                if (invalidConfig != null) {
                    logger.error("Keeping invalid config at {} unchanged; fix the reported entries and reload resources", path);
                }
            } catch (RuntimeException exception) {
                logger.error("Failed to apply config from {}; the file has been left unchanged", path, exception);
            }
        }

        for (Pair<String, JsonObject> entry : list) {
            String name = entry.getFirst();
            JsonObject object = entry.getSecond();
            try {
                if (object.has("conditions") &&
                    !CraftingHelper.processConditions(object, "conditions", ICondition.IContext.EMPTY)) {
                    logger.debug("Skipping loading {} from {} as its conditions were not met", id, name);
                    continue;
                }
                apply(object, false, name, profiler);
            } catch (RuntimeException exception) {
                logger.error("Failed to apply {} from a resource pack; skipping it", name, exception);
            }
        }

        if (configToSave != null) {
            profiler.push("save");
            saveConfig(configToSave);
            profiler.pop();
        }
        config = null;
        profiler.endTick();
    }

    /**
     * Deserialize a config object and refresh its runtime data.
     *
     * @return a non-null value if a local config is invalid, otherwise {@code null}
     */
    @Nullable
    protected abstract JsonObject apply(JsonObject input, boolean config, String source, ProfilerFiller profiler);

    /**
     * Serialize the last successfully applied local configuration.
     */
    protected abstract JsonObject serializeConfig();

    /**
     * Rebuild runtime lookup data from the last successfully applied local config.
     * Resource-pack additions are reapplied after this method returns.
     */
    protected void resetRuntimeData() {}

    @Nullable
    protected JsonObject loadConfig() {
        if (!Files.exists(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject result = GSON.fromJson(reader, JsonObject.class);
            if (result == null) {
                throw new IllegalArgumentException("Config is empty or contains JSON null");
            }
            return result;
        } catch (Throwable throwable) {
            configLoadFailed = true;
            logger.error("Failed to read config from {}; the file will not be overwritten", path, throwable);
            return null;
        }
    }

    private void saveConfig(JsonObject output) {
        try {
            Files.createDirectories(path.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                GSON.toJson(output, writer);
            }
            logger.info("Saved config to {}", path);
        } catch (Throwable throwable) {
            logger.error("Failed to save config to {}", path, throwable);
        }
    }
}
