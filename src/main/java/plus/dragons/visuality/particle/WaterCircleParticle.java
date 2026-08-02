package plus.dragons.visuality.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import plus.dragons.visuality.particle.type.ColorParticleType;

public class WaterCircleParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private static final Quaternionf QUATERNION = new Quaternionf(0F, -0.7F, 0.7F, 0F);

    private WaterCircleParticle(ClientLevel level, double x, double y, double z, float r, float g, float b, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.lifetime = 5 + this.random.nextInt(3);
        this.setParticleSpeed(0D, 0D, 0D);
        if (r > 0 && g > 0 && b > 0) {
            this.setColor(r, g, b);
        }
        this.scale(2F + (float) this.random.nextInt(11) / 10);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    public void setColor(int rgbHex) {
        float red = (float) ((rgbHex & 16711680) >> 16) / 255.0F;
        float green = (float) ((rgbHex & '\uff00') >> 8) / 255.0F;
        float blue = (float) ((rgbHex & 255)) / 255.0F;
        this.setColor(red, green, blue);
    }

    @Override
    public void tick() {
        if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - ((float) this.age - (float) (this.lifetime / 2)) / (float) this.lifetime);
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
        else {
            this.setSpriteFromAge(sprites);
        }
    }

    @Override
    public void extract(QuadParticleRenderState reusedState, Camera camera, float partialTick) {
        this.extractRotatedQuad(reusedState, camera, QUATERNION, partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleType.Options> {
        
        @Override
        public Particle createParticle(ColorParticleType.Options options, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ, RandomSource random) {
            return new WaterCircleParticle(world, x, y, z, options.r, options.g, options.b, sprites);
        }
        
    }
    
}
