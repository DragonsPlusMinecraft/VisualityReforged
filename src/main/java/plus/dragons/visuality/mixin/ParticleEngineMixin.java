package plus.dragons.visuality.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleResources;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import plus.dragons.visuality.particle.VisualityParticleEngine;
import plus.dragons.visuality.particle.VisualityParticleResources;
import plus.dragons.visuality.registry.VisualityRegistries;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin implements VisualityParticleEngine {
    
    @Shadow @Final private ParticleResources resourceManager;
    
    @Override
    public <O extends ParticleOptions, T extends ParticleType<O>> void registerVisuality(DeferredHolder<ParticleType<?>,T> type, ParticleProvider<O> provider) {
        ((VisualityParticleResources) this.resourceManager).registerVisuality(type, provider);
    }

    @Override
    public <O extends ParticleOptions, T extends ParticleType<O>> void registerVisuality(DeferredHolder<ParticleType<?>,T> type, ParticleResources.SpriteParticleRegistration<O> registration) {
        ((VisualityParticleResources) this.resourceManager).registerVisuality(type, registration);
    }
    
    @Nullable
    @ModifyExpressionValue(method = "makeParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/Identifier;"))
    private Identifier visuality$particleAlias(@Nullable Identifier original, @Local(ordinal = 0, argsOnly = true) ParticleOptions options) {
        if(VisualityRegistries.PARTICLE_TYPES_REGISTRY!=null){
            return original == null ? VisualityRegistries.PARTICLE_TYPES_REGISTRY.getKey(options.getType()) : original;
        }
        return original;
    }
    
}
