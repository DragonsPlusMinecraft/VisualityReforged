package plus.dragons.visuality.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.RandomSource;
import plus.dragons.visuality.particle.type.ColorParticleType;

public class SparkleParticle extends SingleQuadParticle {
    private final SpriteSet sprites;

    private SparkleParticle(ClientLevel level,
                            double x, double y, double z,
                            float r, float g, float b,
                            SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.setColor(r, g, b);
        this.lifetime = 5 + this.random.nextInt(4);
        this.setParticleSpeed(0D, 0D, 0D);
        this.scale(1.1F);
        this.sprites = sprites;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime)
            this.remove();
        else
            this.setSpriteFromAge(sprites);
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    @Override
    public int getLightColor(float tint) {
        return LightTexture.FULL_BRIGHT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorParticleType.Options> {
        
        @Override
        public Particle createParticle(ColorParticleType.Options options, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ, RandomSource random) {
            return new SparkleParticle(world, x, y, z, options.r, options.g, options.b, sprites);
        }
        
    }
    
}
