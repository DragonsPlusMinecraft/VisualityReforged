package plus.dragons.visuality.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import plus.dragons.visuality.particle.*;
import plus.dragons.visuality.particle.type.ColorParticleType;
import plus.dragons.visuality.particle.type.ColorScaleParticleType;

import java.util.function.Supplier;

public class VisualityParticles {
    
    public static final DeferredHolder<ParticleType<?>,ColorParticleType> SPARKLE = register("sparkle", ColorParticleType::new);
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> BONE = register("bone");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> WITHER_BONE = register("wither_bone");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> FEATHER = register("feather");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> COLD_FEATHER = register("cold_feather");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> WARM_FEATHER = register("warm_feather");
    public static final DeferredHolder<ParticleType<?>,ColorScaleParticleType> SMALL_SLIME_BLOB = register("small_slime_blob", ColorScaleParticleType::new);
    public static final DeferredHolder<ParticleType<?>,ColorScaleParticleType> MEDIUM_SLIME_BLOB = register("medium_slime_blob", ColorScaleParticleType::new);
    public static final DeferredHolder<ParticleType<?>,ColorScaleParticleType> BIG_SLIME_BLOB = register("big_slime_blob", ColorScaleParticleType::new);
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> CHARGE = register("charge");
    public static final DeferredHolder<ParticleType<?>,ColorParticleType> WATER_CIRCLE = register("water_circle", ColorParticleType::new);
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> EMERALD = register("emerald");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> SOUL = register("soul");

    private static DeferredHolder<ParticleType<?>,SimpleParticleType> register(String name) {
        return VisualityRegistries.PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }
    
    private static <O extends ParticleOptions, T extends ParticleType<O>> DeferredHolder<ParticleType<?>,T> register(String name, Supplier<T> factory) {
        return VisualityRegistries.PARTICLE_TYPES.register(name, factory);
    }
    
    public static void register() {}
    
    public static void registerProviders(RegisterParticleProvidersEvent event) {
        VisualityParticleEngine engine = ((VisualityParticleEngine)Minecraft.getInstance().particleEngine);
        engine.registerVisuality(SPARKLE, SparkleParticle.Provider::new);
        engine.registerVisuality(BONE, SolidFallingParticle.Provider::new);
        engine.registerVisuality(WITHER_BONE, SolidFallingParticle.Provider::new);
        engine.registerVisuality(FEATHER, FeatherParticle.Provider::new);
        engine.registerVisuality(COLD_FEATHER, FeatherParticle.Provider::new);
        engine.registerVisuality(WARM_FEATHER, FeatherParticle.Provider::new);
        engine.registerVisuality(SMALL_SLIME_BLOB, SlimeParticle.Provider::new);
        engine.registerVisuality(MEDIUM_SLIME_BLOB, SlimeParticle.Provider::new);
        engine.registerVisuality(BIG_SLIME_BLOB, SlimeParticle.Provider::new);
        engine.registerVisuality(CHARGE, ChargeParticle.Provider::new);
        engine.registerVisuality(WATER_CIRCLE, WaterCircleParticle.Provider::new);
        engine.registerVisuality(EMERALD, SolidFallingParticle.Provider::new);
        engine.registerVisuality(SOUL, SoulParticle.Provider::new);
    }
    
}
