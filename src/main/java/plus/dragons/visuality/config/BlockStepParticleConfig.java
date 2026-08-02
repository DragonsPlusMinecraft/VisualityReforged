package plus.dragons.visuality.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import plus.dragons.visuality.Visuality;
import plus.dragons.visuality.data.ParticleWithVelocity;
import plus.dragons.visuality.data.VisualityCodecs;
import plus.dragons.visuality.registry.VisualityParticles;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class BlockStepParticleConfig extends ReloadableJsonConfig {
    private boolean enabled = true;
    private int interval = 10;
    private List<Entry> entries;
    private final IdentityHashMap<Block, ParticleWithVelocity> particles = new IdentityHashMap<>();
    
    public BlockStepParticleConfig() {
        super(Visuality.location("particle_emitters/block_step"));
        this.entries = createDefaultEntries();
        for (Entry entry : entries) {
            for (Block block : entry.blocks) {
                particles.put(block, entry.particle);
            }
        }
    }
    
    public int getInterval() {
        return interval;
    }
    
    public void spawnParticles(int amount, Level level, BlockState state, BlockPos pos, Entity entity) {
        if (!enabled || entity.isSteppingCarefully())
            return;
        Block block = state.getBlock();
        if (!particles.containsKey(block))
            return;
        var particle = particles.get(block);
        double y = entity.getY() + 0.0625;
        for (int i = 0; i < amount; ++i) {
            double x = pos.getX() + level.random.nextDouble();
            double z = pos.getZ() + level.random.nextDouble();
            particle.spawn(level, x, y, z);
        }
    }
    
    @Override
    @Nullable
    protected JsonObject apply(JsonObject input, boolean config, String source, ProfilerFiller profiler) {
        profiler.push(source);
        try {
            boolean newEnabled = enabled;
            int newInterval = interval;
            if (config) {
                newEnabled = GsonHelper.getAsBoolean(input, "enabled", true);
                newInterval = GsonHelper.getAsInt(input, "interval", 10);
                if (newInterval < 1) {
                    throw new IllegalArgumentException("'interval' must be at least 1");
                }
            }
            JsonArray array = GsonHelper.getAsJsonArray(input, "entries", null);
            if (array == null) {
                logger.warn("Failed to load options entries from {}: Missing JsonArray 'entries'.", source);
                return config ? serializeConfig() : null;
            }
            boolean invalid = false;
            List<Entry> newEntries = new ArrayList<>();
            List<JsonElement> elements = Lists.newArrayList(array);
            for (JsonElement element : elements) {
                var data = Entry.CODEC.parse(JsonOps.INSTANCE, element);
                if (data.error().isPresent()) {
                    invalid = config;
                    logger.warn("Error parsing {} from {}: {}", id, source, data.error().get().message());
                    continue;
                }
                if (data.result().isPresent())
                    newEntries.add(data.result().get());
                else {
                    invalid = config;
                    logger.warn("Error parsing {} from {}: Missing decode result", id, source);
                }
            }
            if (invalid)
                return serializeConfig();
            if (config) {
                enabled = newEnabled;
                interval = newInterval;
                entries = newEntries;
                particles.clear();
            }
            for (Entry entry : newEntries) {
                for (Block block : entry.blocks) {
                    particles.put(block, entry.particle);
                }
            }
            return null;
        } finally {
            profiler.pop();
        }
    }
    
    @Override
    protected JsonObject serializeConfig() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.addProperty("interval", interval);
        object.add("entries", Entry.LIST_CODEC.encodeStart(JsonOps.INSTANCE, entries).getOrThrow());
        return object;
    }
    
    private record Entry(List<Block> blocks, ParticleWithVelocity particle) {
    
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compressedListOf(BuiltInRegistries.BLOCK.byNameCodec()).fieldOf("block")
                .forGetter(Entry::blocks),
            ParticleWithVelocity.CODEC.fieldOf("particle")
                .forGetter(Entry::particle)
        ).apply(instance, Entry::new));
    
        private static final Codec<List<Entry>> LIST_CODEC = CODEC.listOf();
        
        static Entry of(ParticleOptions particle, Block... blocks) {
            return new Entry(List.of(blocks), ParticleWithVelocity.ofZeroVelocity(particle));
        }
        
    }
    
    private static List<Entry> createDefaultEntries() {
        List<Entry> entries = new ArrayList<>();
        
        entries.add(Entry.of(VisualityParticles.SOUL.get(),
            Blocks.SOUL_SAND,
            Blocks.SOUL_SOIL));
        
        return entries;
    }
    
}
