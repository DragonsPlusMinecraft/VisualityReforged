package plus.dragons.visuality.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.visuality.config.Config;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void handleParticle(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var living = (LivingEntity) (Object) this;
        if(living.level().isClientSide){
            Config.ENTITY_HIT_PARTICLES.spawnParticles(living,source,amount);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnArmorParticles(CallbackInfo ci) {
        var living = (LivingEntity) (Object) this;
        if (living.level().isClientSide) {
            Config.ENTITY_ARMOR_PARTICLES.spawnParticles(living);
        }
    }
}
