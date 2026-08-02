package plus.dragons.visuality.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * A Json-based config which can be load from config and resource packs
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
            for(String namespace : resourceManager.getNamespaces()) {
                profiler.push(namespace);
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, this.id.getPath());
                for (Resource resource : resourceManager.getResourceStack(id)) {
                    profiler.push(resource.sourcePackId());
                    try {
                        Reader reader = resource.openAsReader();
                        profiler.push("parse");
                        JsonObject object = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                        profiler.pop();
                        list.add(Pair.of(resource.sourcePackId() + '#' + id, object));
                    } catch (RuntimeException exception) {
                        logger.warn("Invalid {} in resourcepack: '{}'", id, resource.sourcePackId(), exception);
                    }
                    profiler.pop();
                }
                profiler.pop();
            }
        } catch (IOException ignored) {
        }
        profiler.endTick();
        return list;
    }
    
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
        for (var entry : list) {
            String name = entry.getFirst();
            JsonObject object = entry.getSecond();
            if (!processConditions(object, "conditions", ICondition.IContext.EMPTY)) {
                logger.debug("Skipping loading {} from {} as it's conditions were not met", id, name);
                continue;
            }
            try {
                apply(object, false, name, profiler);
            } catch (RuntimeException exception) {
                logger.error("Failed to apply {} from resource pack; skipping it", name, exception);
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

    protected boolean processConditions(JsonObject json, String item, ICondition.IContext context){
        if(json.has(item)){
            var condition = getOrThrow(ICondition.CODEC.parse(JsonOps.INSTANCE, json.getAsJsonObject(item)), JsonParseException::new);
            return condition.test(context);
        }
        return true;
    }

    public static <T, E extends Exception> T getOrThrow(DataResult<T> dataResult, Function<String, E> exThrower) throws E {
        Optional<DataResult.Error<T>> optional = dataResult.error();
        if (optional.isPresent()) {
            throw exThrower.apply((optional.get()).message());
        } else {
            return dataResult.result().orElseThrow();
        }
    }
    
    /**
     * Deserialize the config JsonObject and refresh the config data
     * @param input the JsonObject read from file in {@link ReloadableJsonConfig#loadConfig()}
     * @param config if the JsonObject is from config
     * @param source a String to identify the source of the JsonObject
     * @return a non-null value if the file is invalid, or null if it was applied successfully
     */
    @Nullable
    protected abstract JsonObject apply(JsonObject input, boolean config, String source, ProfilerFiller profiler);
    
    /**
     * Serialize the config data into JsonElement for saving
     * @return the serialized config data
     */
    protected abstract JsonObject serializeConfig();

    /**
     * Rebuild runtime lookup data from the last successfully applied local config.
     * Resource-pack additions are reapplied after this method returns.
     */
    protected void resetRuntimeData() {}
    
    /**
     * Load the config JsonElement from file
     * @return the raw JsonElement, or null if the file does not exist or failed to load
     */
    @Nullable
    protected JsonObject loadConfig() {
        if (!Files.exists(path)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            JsonObject result = GSON.fromJson(reader, JsonObject.class);
            if (result == null) {
                throw new JsonParseException("Config is empty or contains JSON null");
            }
            return result;
        } catch (Throwable throwable) {
            configLoadFailed = true;
            logger.error("Failed to read config from {}; the file will not be overwritten", path, throwable);
            return null;
        }
    }
    
    /**
     * Save the config to file
     * @param output the config JsonElement to save
     */
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
