package plus.dragons.visuality.mixin.immersivedamageindicators;

import net.minecraft.client.particle.ParticleEngine;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import toni.immersivedamageindicators.foundation.ParticleRegistry;
import toni.immersivedamageindicators.particle.DamageParticle;

@Mixin(DamageParticle.Factory.class)
public class DamageParticleFactoryMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void onUpdate(RegisterParticleProvidersEvent event, CallbackInfo ci) {
        event.registerSpecial(ParticleRegistry.DAMAGE_PARTICLE, new DamageParticle.Factory(new ParticleEngine.MutableSpriteSet()));
        ci.cancel();
    }
}
