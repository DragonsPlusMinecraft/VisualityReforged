package plus.dragons.visuality.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import plus.dragons.visuality.Visuality;
import plus.dragons.visuality.data.ParticleWithVelocity;
import plus.dragons.visuality.data.VisualityCodecs;
import plus.dragons.visuality.registry.VisualityParticles;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class EntityArmorParticleConfig extends ReloadableJsonConfig {
    private boolean enabled = true;
    private int interval = 20;
    private List<Entry> entries;
    private final IdentityHashMap<Item, Emitter> particles = new IdentityHashMap<>();
    private final List<TaggedEmitter> taggedParticles = new ArrayList<>();
    private long nextPriority;
    
    public EntityArmorParticleConfig() {
        super(Visuality.location("particle_emitters/entity_armor"));
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

    private void registerEntry(Entry entry) {
        Emitter emitter = new Emitter(entry.particle(), nextPriority++);
        for (ItemSelector armor : entry.armors()) {
            if (armor.item() != null) {
                particles.put(armor.item(), emitter);
            } else if (armor.tag() != null) {
                taggedParticles.add(new TaggedEmitter(armor.tag(), emitter));
            }
        }
    }

    @Nullable
    private ParticleWithVelocity findParticle(ItemStack stack) {
        Emitter result = particles.get(stack.getItem());
        for (TaggedEmitter tagged : taggedParticles) {
            if (stack.is(tagged.tag()) && (result == null || tagged.emitter().priority() > result.priority())) {
                result = tagged.emitter();
            }
        }
        return result == null ? null : result.particle();
    }
    
    public void spawnParticles(LivingEntity entity) {
        if (!enabled)
            return;

        Level level = entity.level();
        if(!entity.isAlive())
            return;
        
        RandomSource random = entity.getRandom();
        if (random.nextInt(interval) != 0)
            return;
        
        Minecraft minecraft = Minecraft.getInstance();
        EntityRenderer<?,?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof RenderLayerParent<?, ?> parent && parent.getModel() instanceof HumanoidModel<?>))
            return;
        
        if (minecraft.cameraEntity == entity && minecraft.options.getCameraType().isFirstPerson())
            return;
        
        double height = random.nextDouble();
        EquipmentSlot slot = switchEquipmentSlotFromHeight(height);
        ParticleWithVelocity particle = findParticle(entity.getItemBySlot(slot));
        if (particle != null) {
            double x, y, z;
            AABB aabb = entity.getBoundingBox();
            double radian = 2 * Math.PI * random.nextDouble();
            x = Mth.lerp(0.5 + 0.75 * Math.cos(radian), aabb.minX, aabb.maxX);
            y = Mth.lerp(height, aabb.minY, aabb.maxY);
            z = Mth.lerp(0.5 + 0.75 * Math.sin(radian), aabb.minZ, aabb.maxZ);
            particle.spawn(level, x, y, z);
        }
    }
    
    private EquipmentSlot switchEquipmentSlotFromHeight(double height) {
        if (height < 3 / 16d) return EquipmentSlot.FEET;
        if (height < 8 / 16d) return EquipmentSlot.LEGS;
        if (height < 13 / 16d) return EquipmentSlot.CHEST;
        return EquipmentSlot.HEAD;
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
                newInterval = GsonHelper.getAsInt(input, "interval", 20);
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
    
    private record Emitter(ParticleWithVelocity particle, long priority) {}

    private record TaggedEmitter(TagKey<Item> tag, Emitter emitter) {}

    private record ItemSelector(@Nullable Item item, @Nullable TagKey<Item> tag) {
        private static final Codec<ItemSelector> CODEC = Codec.STRING.comapFlatMap(ItemSelector::parse, ItemSelector::encode);

        private static DataResult<ItemSelector> parse(String value) {
            boolean isTag = value.startsWith("#");
            String idString = isTag ? value.substring(1) : value;
            ResourceLocation id = ResourceLocation.tryParse(idString);
            if (id == null) {
                return DataResult.error(() -> "Invalid item selector '" + value + "'");
            }
            if (isTag) {
                return DataResult.success(new ItemSelector(null, TagKey.create(Registries.ITEM, id)));
            }
            return BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> DataResult.success(new ItemSelector(item, null)))
                .orElseGet(() -> DataResult.error(() -> "Unknown item '" + id + "'"));
        }

        private static String encode(ItemSelector selector) {
            return selector.tag() != null
                ? "#" + selector.tag().location()
                : BuiltInRegistries.ITEM.getKey(selector.item()).toString();
        }

        private static ItemSelector of(Item item) {
            return new ItemSelector(item, null);
        }
    }

    private record Entry(List<ItemSelector> armors, ParticleWithVelocity particle) {
    
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compressedListOf(ItemSelector.CODEC).fieldOf("armor")
                .forGetter(Entry::armors),
            ParticleWithVelocity.CODEC.fieldOf("particle")
                .forGetter(Entry::particle)
        ).apply(instance, Entry::new));
    
        private static final Codec<List<Entry>> LIST_CODEC = CODEC.listOf();
    
        private static Entry of(ParticleOptions particle, Item... armors) {
            List<ItemSelector> selectors = new ArrayList<>(armors.length);
            for (Item armor : armors) {
                selectors.add(ItemSelector.of(armor));
            }
            return new Entry(selectors, ParticleWithVelocity.ofZeroVelocity(particle));
        }
        
    }
    
    private static List<Entry> createDefaultEntries() {
        List<Entry> entries = new ArrayList<>();
        
        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xFEFFBD),
            Items.GOLDEN_HELMET,
            Items.GOLDEN_CHESTPLATE,
            Items.GOLDEN_LEGGINGS,
            Items.GOLDEN_BOOTS));
        
        entries.add(Entry.of(VisualityParticles.SPARKLE.get().withColor(0xB4FDEE),
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS));
        
        return entries;
    }
    
}
