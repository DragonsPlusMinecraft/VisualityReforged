package com.github.channelingmc.visuality.config.block;

import com.github.channelingmc.visuality.Visuality;
import com.github.channelingmc.visuality.config.ReloadableJsonConfig;
import com.github.channelingmc.visuality.data.Particle;
import com.github.channelingmc.visuality.data.VisualityCodecs;
import com.github.channelingmc.visuality.particle.type.VisualityParticleTypes;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

@ApiStatus.Internal
public class BlockStepParticles extends ReloadableJsonConfig {
    private boolean enabled = true;
    private int interval = 10;
    private List<Entry> entries;
    private final IdentityHashMap<Block, Particle> particles = new IdentityHashMap<>();
    
    public BlockStepParticles() {
        super(Visuality.loc("particle_emitters/block/step"));
        this.entries = createDefaultEntries();
        for (Entry entry : entries) {
            for (Block block : entry.blocks) {
                particles.put(block, entry.particle);
            }
        }
    }
    
    public int interval() {
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
        if (config) {
            enabled = GsonHelper.getAsBoolean(input, "enabled", true);
            interval = GsonHelper.getAsInt(input, "interval", 10);
        }
        JsonArray array = GsonHelper.getAsJsonArray(input, "entries", null);
        if (array == null) {
            logger.warn("Failed to load options entries from {}: Missing JsonArray 'entries'.", source);
            profiler.pop();
            return config ? serializeConfig() : null;
        }
        boolean save = false;
        List<Entry> newEntries = new ArrayList<>();
        List<JsonElement> elements = Lists.newArrayList(array);
        for (JsonElement element : elements) {
            var data = Entry.CODEC.parse(JsonOps.INSTANCE, element);
            if (data.error().isPresent()) {
                save = config;
                logger.warn("Error parsing {} from {}: {}", id, source, data.error().get().message());
                continue;
            }
            if (data.result().isPresent())
                newEntries.add(data.result().get());
            else {
                save = config;
                logger.warn("Error parsing {} from {}: Missing decode result", id, source);
            }
        }
        if (config) {
            entries = newEntries;
            particles.clear();
        }
        for (Entry entry : newEntries) {
            for (Block block : entry.blocks) {
                particles.put(block, entry.particle);
            }
        }
        profiler.pop();
        return save ? serializeConfig() : null;
    }
    
    @Override
    protected JsonObject serializeConfig() {
        JsonObject object = new JsonObject();
        object.addProperty("enabled", enabled);
        object.addProperty("interval", interval);
        object.add("entries", Entry.LIST_CODEC.encodeStart(JsonOps.INSTANCE, entries)
            .getOrThrow(true, msg -> logger.error("Failed to serialize config entries: {}", msg)));
        return object;
    }
    
    private record Entry(List<Block> blocks, Particle particle) {
    
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compactListOf(ForgeRegistries.BLOCKS.getCodec()).fieldOf("block")
                .forGetter(Entry::blocks),
            Particle.CODEC.fieldOf("particle")
                .forGetter(Entry::particle)
        ).apply(instance, Entry::new));
    
        private static final Codec<List<Entry>> LIST_CODEC = CODEC.listOf();
    
        private static Entry of(Block block, ParticleOptions particle) {
            return new Entry(List.of(block), Particle.ofZeroVelocity(particle));
        }
        
        static Entry of(List<Block> blocks, ParticleOptions particle) {
            return new Entry(blocks, Particle.ofZeroVelocity(particle));
        }
        
    }
    
    private static List<Entry> createDefaultEntries() {
        List<Entry> entries = new ArrayList<>();
        SimpleParticleType soul = VisualityParticleTypes.SOUL.get();
        entries.add(Entry.of(List.of(Blocks.SOUL_SAND, Blocks.SOUL_SOIL), soul));
        return entries;
    }
    
}