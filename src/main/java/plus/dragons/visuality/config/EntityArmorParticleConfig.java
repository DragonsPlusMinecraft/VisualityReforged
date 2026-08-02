package plus.dragons.visuality.config;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import plus.dragons.visuality.Visuality;
import plus.dragons.visuality.data.ParticleWithVelocity;
import plus.dragons.visuality.data.VisualityCodecs;
import plus.dragons.visuality.registry.VisualityParticles;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class EntityArmorParticleConfig extends ReloadableJsonConfig {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private boolean enabled = true;
    private int interval = 20;
    private List<Entry> entries;
    private final IdentityHashMap<Item, ParticleWithVelocity> particles = new IdentityHashMap<>();

    public EntityArmorParticleConfig() {
        super(Visuality.location("particle_emitters/entity_armor"));
        this.entries = createDefaultEntries();
        resetRuntimeData();
        MinecraftForge.EVENT_BUS.addListener(this::spawnParticles);
    }

    @Override
    protected void resetRuntimeData() {
        particles.clear();
        for (Entry entry : entries) {
            for (Item armor : entry.armors()) {
                particles.put(armor, entry.particle());
            }
        }
    }

    public void spawnParticles(LivingEvent.LivingTickEvent event) {
        if (!enabled) {
            return;
        }

        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide || !entity.isAlive()) {
            return;
        }

        RandomSource random = entity.getRandom();
        if (random.nextInt(interval) != 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.cameraEntity == entity && minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        List<ArmorParticle> wornParticles = new ArrayList<>(ARMOR_SLOTS.length);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ParticleWithVelocity particle = particles.get(entity.getItemBySlot(slot).getItem());
            if (particle != null) {
                wornParticles.add(new ArmorParticle(slot, particle));
            }
        }
        if (wornParticles.isEmpty()) {
            return;
        }

        ArmorParticle selected = wornParticles.get(random.nextInt(wornParticles.size()));
        double height = randomHeight(selected.slot(), random);
        AABB bounds = entity.getBoundingBox();
        double radians = 2 * Math.PI * random.nextDouble();
        double x = Mth.lerp(0.5 + 0.75 * Math.cos(radians), bounds.minX, bounds.maxX);
        double y = Mth.lerp(height, bounds.minY, bounds.maxY);
        double z = Mth.lerp(0.5 + 0.75 * Math.sin(radians), bounds.minZ, bounds.maxZ);
        selected.particle().spawn(level, x, y, z);
    }

    private static double randomHeight(EquipmentSlot slot, RandomSource random) {
        double min;
        double max;
        switch (slot) {
            case FEET -> {
                min = 0;
                max = 3 / 16.0;
            }
            case LEGS -> {
                min = 3 / 16.0;
                max = 8 / 16.0;
            }
            case CHEST -> {
                min = 8 / 16.0;
                max = 13 / 16.0;
            }
            case HEAD -> {
                min = 13 / 16.0;
                max = 1;
            }
            default -> throw new IllegalArgumentException("Not an armor slot: " + slot);
        }
        return Mth.lerp(random.nextDouble(), min, max);
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
            for (JsonElement element : Lists.newArrayList(array)) {
                var data = Entry.CODEC.parse(JsonOps.INSTANCE, element);
                if (data.error().isPresent()) {
                    invalid = config;
                    logger.warn("Error parsing {} from {}: {}", id, source, data.error().get().message());
                    continue;
                }
                if (data.result().isPresent()) {
                    newEntries.add(data.result().get());
                } else {
                    invalid = config;
                    logger.warn("Error parsing {} from {}: Missing decode result", id, source);
                }
            }

            if (invalid) {
                return serializeConfig();
            }
            if (config) {
                enabled = newEnabled;
                interval = newInterval;
                entries = newEntries;
                resetRuntimeData();
            } else {
                for (Entry entry : newEntries) {
                    for (Item armor : entry.armors()) {
                        particles.put(armor, entry.particle());
                    }
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
        object.add("entries", Entry.LIST_CODEC.encodeStart(JsonOps.INSTANCE, entries)
            .getOrThrow(true, message -> logger.error("Failed to serialize config entries: {}", message)));
        return object;
    }

    private record ArmorParticle(EquipmentSlot slot, ParticleWithVelocity particle) {}

    private record Entry(List<Item> armors, ParticleWithVelocity particle) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            VisualityCodecs.compressedListOf(ForgeRegistries.ITEMS.getCodec()).fieldOf("armor")
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
