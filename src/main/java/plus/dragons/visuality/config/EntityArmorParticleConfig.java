package plus.dragons.visuality.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
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
    private final IdentityHashMap<Item, ParticleWithVelocity> particles = new IdentityHashMap<>();
    
    public EntityArmorParticleConfig() {
        super(Visuality.location("particle_emitters/entity_armor"));
        this.entries = createDefaultEntries();
        for (Entry entry : entries) {
            for (Item armor : entry.armors) {
                particles.put(armor, entry.particle);
            }
        }
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
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        if (!(renderer instanceof RenderLayerParent<?, ?> parent && parent.getModel() instanceof HumanoidModel<?>))
            return;
        
        if (minecraft.cameraEntity == entity && minecraft.options.getCameraType().isFirstPerson())
            return;
        
        double height = random.nextDouble();
        EquipmentSlot slot = switchEquipmentSlotFromHeight(height);
        Item armor = entity.getItemBySlot(slot).getItem();
        if (particles.containsKey(armor)) {
            double x, y, z;
            AABB aabb = entity.getBoundingBox();
            double radian = 2 * Math.PI * random.nextDouble();
            x = Mth.lerp(0.5 + 0.75 * Math.cos(radian), aabb.minX, aabb.maxX);
            y = Mth.lerp(height, aabb.minY, aabb.maxY);
            z = Mth.lerp(0.5 + 0.75 * Math.sin(radian), aabb.minZ, aabb.maxZ);
            particles.get(armor).spawn(level, x, y, z);
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
                particles.clear();
            }
            for (Entry entry : newEntries) {
                for (Item armor : entry.armors) {
                    particles.put(armor, entry.particle);
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
    
    private record Entry(List<Item> armors, ParticleWithVelocity particle) {
    
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compressedListOf(BuiltInRegistries.ITEM.byNameCodec()).fieldOf("armor")
                .forGetter(Entry::armors),
            ParticleWithVelocity.CODEC.fieldOf("particle")
                .forGetter(Entry::particle)
        ).apply(instance, Entry::new));
    
        private static final Codec<List<Entry>> LIST_CODEC = CODEC.listOf();
    
        private static Entry of(ParticleOptions particle, Item... armors) {
            return new Entry(List.of(armors), ParticleWithVelocity.ofZeroVelocity(particle));
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
