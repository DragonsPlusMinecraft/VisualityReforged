package plus.dragons.visuality.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import plus.dragons.visuality.Visuality;
import plus.dragons.visuality.data.ParticleWithVelocity;
import plus.dragons.visuality.data.VisualityCodecs;
import plus.dragons.visuality.registry.VisualityParticles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

public class BlockAmbientParticleConfig extends ReloadableJsonConfig {
    private boolean enabled = true;
    private int interval = 10;
    private List<Entry> entries;
    private final IdentityHashMap<Block, Emitter> particles = new IdentityHashMap<>();
    private final List<TaggedEmitter> taggedParticles = new ArrayList<>();
    private long nextPriority;

    public BlockAmbientParticleConfig() {
        super(Visuality.location("particle_emitters/block_ambient"));
        this.entries = createDefaultEntries();
        resetRuntimeData();
    }

    @Override
    protected void resetRuntimeData() {
        particles.clear();
        taggedParticles.clear();
        nextPriority = 0;
        entries.forEach(this::registerEntry);
    }

    @SuppressWarnings("deprecation")
    public void spawnParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!enabled)
            return;
        Emitter emitter = findEmitter(state);
        if (emitter == null)
            return;
        if (!level.isAreaLoaded(pos, 1))
            return;
        Direction[] directions = emitter.directions();
        int i = random.nextInt(emitter.interval());
        if (i >= directions.length)
            return;
        boolean fullBlock = state.isSolidRender();
        if (fullBlock) {
            Direction direction = directions[i];
            BlockPos facePos = pos.relative(direction);
            if (level.getBlockState(facePos).isSolidRender())
                return;
            Direction.Axis axis = direction.getAxis();
            double x = pos.getX() + (axis == Direction.Axis.X ? 0.5 + 0.5625 * direction.getStepX() : random.nextDouble());
            double y = pos.getY() + (axis == Direction.Axis.Y ? 0.5 + 0.5625 * direction.getStepY() : random.nextDouble());
            double z = pos.getZ() + (axis == Direction.Axis.Z ? 0.5 + 0.5625 * direction.getStepZ() : random.nextDouble());
            emitter.particle().spawn(level, x, y, z);
        } else {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            emitter.particle().spawn(level, x, y, z);
        }
    }

    @Nullable
    private Emitter findEmitter(BlockState state) {
        Emitter result = particles.get(state.getBlock());
        for (TaggedEmitter tagged : taggedParticles) {
            if (state.is(tagged.tag()) && (result == null || tagged.emitter().priority() > result.priority())) {
                result = tagged.emitter();
            }
        }
        return result;
    }

    private void registerEntry(Entry entry) {
        int entryInterval = entry.interval().orElseGet(() ->
            entry.particle().options().getType() == VisualityParticles.SOUL.get() ? 200 : interval);
        Emitter emitter = new Emitter(
            entry.directions().toArray(Direction[]::new), entry.particle(), entryInterval, nextPriority++);
        for (BlockSelector selector : entry.blocks()) {
            if (selector.block() != null) {
                particles.put(selector.block(), emitter);
            } else if (selector.tag() != null) {
                taggedParticles.add(new TaggedEmitter(selector.tag(), emitter));
            }
        }

        // Configs generated before 3.0 used explicit vanilla block IDs. Promote unchanged
        // ore groups to their common tags at runtime so modded ores work without deleting configs.
        registerLegacyCommonTag(entry, emitter, Tags.Blocks.ORES_GOLD,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        registerLegacyCommonTag(entry, emitter, Tags.Blocks.ORES_DIAMOND,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        registerLegacyCommonTag(entry, emitter, Tags.Blocks.ORES_EMERALD,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
    }

    private void registerLegacyCommonTag(Entry entry, Emitter emitter, TagKey<Block> tag, Block... legacyBlocks) {
        if (!entry.containsTag(tag) && entry.containsBlocks(legacyBlocks)) {
            taggedParticles.add(new TaggedEmitter(tag, emitter));
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
                var result = data.result().filter(entry -> !entry.directions().isEmpty());
                if (result.isPresent()) {
                    newEntries.add(result.get());
                } else {
                    logger.warn("Error parsing {} from {}: Directions must not be empty", id, source);
                    invalid = config;
                }
            }
            if (invalid)
                return serializeConfig();
            if (config) {
                enabled = newEnabled;
                interval = newInterval;
                entries = newEntries;
                resetRuntimeData();
            } else {
                newEntries.forEach(this::registerEntry);
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

    private record Emitter(Direction[] directions, ParticleWithVelocity particle, int interval, long priority) {}

    private record TaggedEmitter(TagKey<Block> tag, Emitter emitter) {}

    private record BlockSelector(@Nullable Block block, @Nullable TagKey<Block> tag) {
        private static final Codec<BlockSelector> CODEC = Codec.STRING.comapFlatMap(BlockSelector::parse, BlockSelector::encode);

        private static DataResult<BlockSelector> parse(String value) {
            boolean isTag = value.startsWith("#");
            String idString = isTag ? value.substring(1) : value;
            ResourceLocation id = ResourceLocation.tryParse(idString);
            if (id == null) {
                return DataResult.error(() -> "Invalid block selector '" + value + "'");
            }
            if (isTag) {
                return DataResult.success(new BlockSelector(null, TagKey.create(Registries.BLOCK, id)));
            }
            return BuiltInRegistries.BLOCK.getOptional(id)
                .map(block -> DataResult.success(new BlockSelector(block, null)))
                .orElseGet(() -> DataResult.error(() -> "Unknown block '" + id + "'"));
        }

        private static String encode(BlockSelector selector) {
            return selector.tag != null
                ? "#" + selector.tag.location()
                : BuiltInRegistries.BLOCK.getKey(selector.block).toString();
        }

        private static BlockSelector of(Block block) {
            return new BlockSelector(block, null);
        }

        private static BlockSelector of(TagKey<Block> tag) {
            return new BlockSelector(null, tag);
        }
    }

    private record Entry(List<BlockSelector> blocks, EnumSet<Direction> directions,
                         ParticleWithVelocity particle, Optional<Integer> interval) {
        private static final EnumSet<Direction> ALL_DIRECTIONS = EnumSet.allOf(Direction.class);

        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compressedListOf(BlockSelector.CODEC).fieldOf("block")
                .forGetter(Entry::blocks),
            Codec.optionalField("direction", VisualityCodecs.compressedSetOf(Direction.CODEC, EnumSet::copyOf), true)
                .xmap(optional -> optional.orElse(ALL_DIRECTIONS),
                    set -> set.size() == 6 ? Optional.empty() : Optional.of(set))
                .forGetter(Entry::directions),
            ParticleWithVelocity.CODEC.fieldOf("particle")
                .forGetter(Entry::particle),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("interval")
                .forGetter(Entry::interval)
        ).apply(instance, Entry::new));

        private static final Codec<List<Entry>> LIST_CODEC = CODEC.listOf();

        private boolean containsTag(TagKey<Block> tag) {
            return blocks.stream().anyMatch(selector -> tag.equals(selector.tag()));
        }

        private boolean containsBlocks(Block... requiredBlocks) {
            for (Block required : requiredBlocks) {
                if (blocks.stream().noneMatch(selector -> selector.block() == required)) {
                    return false;
                }
            }
            return true;
        }

        private static Entry of(ParticleOptions options, BlockSelector... blocks) {
            return new Entry(List.of(blocks), ALL_DIRECTIONS,
                ParticleWithVelocity.ofZeroVelocity(options), Optional.empty());
        }

        private static Entry of(ParticleOptions options, Direction direction, int interval, BlockSelector... blocks) {
            return new Entry(List.of(blocks), EnumSet.of(direction),
                ParticleWithVelocity.ofZeroVelocity(options), Optional.of(interval));
        }
    }

    private static List<Entry> createDefaultEntries() {
        List<Entry> entries = new ArrayList<>();

        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xFEFFBD),
            BlockSelector.of(Tags.Blocks.ORES_GOLD)));

        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xB4FDEE),
            BlockSelector.of(Tags.Blocks.ORES_DIAMOND)));

        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xD9FFEB),
            BlockSelector.of(Tags.Blocks.ORES_EMERALD)));

        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xFECBE6),
            BlockSelector.of(Blocks.AMETHYST_CLUSTER)));

        entries.add(Entry.of(VisualityParticles.SOUL.get(), Direction.UP, 200,
            BlockSelector.of(Blocks.SOUL_SAND),
            BlockSelector.of(Blocks.SOUL_SOIL)));

        return entries;
    }
}
