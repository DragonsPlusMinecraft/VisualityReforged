package plus.dragons.visuality.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plus.dragons.visuality.config.Config;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow @Final protected RandomSource random;

    @Inject(method = "hurtClient", at = @At("HEAD"))
    private void handleParticle(DamageSource damageSource, CallbackInfoReturnable<Boolean> cir) {
        Object entity = this;
        if (entity instanceof LivingEntity living && living.level().isClientSide()) {
            double insignificantAmount = random.nextDouble() + random.nextInt(6);
            Config.ENTITY_HIT_PARTICLES.spawnParticles(living,damageSource,insignificantAmount);
        }
    }
}
