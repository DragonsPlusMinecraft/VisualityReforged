package plus.dragons.visuality.mixin;

import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import plus.dragons.visuality.particle.VisualityParticleResources;

import java.util.Map;

@Mixin(ParticleResources.class)
public class ParticleResourcesMixin implements VisualityParticleResources {

    @Shadow @Final private Map<Identifier, ParticleResources.MutableSpriteSet> spriteSets;
    @Shadow @Final private Map<Identifier, ParticleProvider<?>> providers;

    @Override
    public <O extends ParticleOptions, T extends ParticleType<O>> void registerVisuality(
        DeferredHolder<ParticleType<?>, T> type,
        ParticleProvider<O> provider
    ) {
        this.providers.put(type.getId(), provider);
    }

    @Override
    public <O extends ParticleOptions, T extends ParticleType<O>> void registerVisuality(
        DeferredHolder<ParticleType<?>, T> type,
        ParticleResources.SpriteParticleRegistration<O> registration
    ) {
        ParticleResources.MutableSpriteSet spriteSet = new ParticleResources.MutableSpriteSet();
        this.spriteSets.put(type.getId(), spriteSet);
        this.providers.put(type.getId(), registration.create(spriteSet));
    }
}
