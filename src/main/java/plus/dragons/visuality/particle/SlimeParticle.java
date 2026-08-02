package plus.dragons.visuality.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.RandomSource;
import plus.dragons.visuality.particle.type.ColorScaleParticleType;

public class SlimeParticle extends SingleQuadParticle {

    private SlimeParticle(ClientLevel level,
                          double x, double y, double z,
                          float r, float g, float b,
                          float scale,
                          SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0, sprites.first());
        this.setColor(r, g, b);
        this.setAlpha(0.8F);
        this.xd *= 0.1;
        this.yd *= 0.1;
        this.zd *= 0.1;
        this.gravity = 1.0F;
        this.scale(scale + random.nextInt(6) / 10F);
        this.lifetime = 10 + random.nextInt(7);
    }

    @Override
    public void tick() {
        if (this.age > this.lifetime / 2) {
            this.setAlpha(1.0F - ((float) this.age - (float) (this.lifetime / 2)) / (float) this.lifetime);
        }
        super.tick();
        if (this.onGround) {
            this.gravity = 0F;
            this.setParticleSpeed(0D, 0D, 0D);
            this.setPos(xo, yo + 0.1D, zo);
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ColorScaleParticleType.Options> {
        
        @Override
        public Particle createParticle(ColorScaleParticleType.Options options, ClientLevel world, double x, double y, double z, double velX, double velY, double velZ, RandomSource random) {
            SlimeParticle particle = new SlimeParticle(world, x, y, z, options.r, options.g, options.b, options.scale, sprites);
            particle.setSprite(sprites.get(world.getRandom()));
            return particle;
        }
        
    }
    
}
