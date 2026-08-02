package plus.dragons.visuality.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plus.dragons.visuality.config.Config;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void spawnArmorParticles(CallbackInfo ci) {
        var living = (LivingEntity) (Object) this;
        if (living.level().isClientSide) {
            Config.ENTITY_ARMOR_PARTICLES.spawnParticles(living);
        }
    }
}
