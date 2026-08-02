package plus.dragons.visuality.particle.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;


public class ColorParticleType extends ParticleType<ColorParticleType.Options> {
    private final MapCodec<Options> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, Options> streamCodec;

    public ColorParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.VECTOR3F.fieldOf("color")
                        .forGetter(ColorParticleType.Options::color)
        ).apply(instance, ColorParticleType.Options::new));
        this.streamCodec = StreamCodec.ofMember(Options::encode,Options::new);
    }

    @Override
    public MapCodec<Options> codec() {
        return codec;
    }

    public ColorParticleType() {
        this(false);
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Options> streamCodec() {
        return streamCodec;
    }

    public Options withColor(float r, float g, float b) {
        return new Options(r, g, b);
    }
    
    public Options withColor(int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        return new Options(r, g, b);
    }
    
    public class Options implements ParticleOptions {
        public final float r;
        public final float g;
        public final float b;
    
        private Options(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }

        private Options(Vector3fc vec) {
            this(vec.x(), vec.y(), vec.z());
        }

        private Options(FriendlyByteBuf buffer) {
            this.r = buffer.readFloat();
            this.g = buffer.readFloat();
            this.b = buffer.readFloat();
        }
    
        @Override
        public ParticleType<?> getType() {
            return ColorParticleType.this;
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeFloat(r);
            buffer.writeFloat(g);
            buffer.writeFloat(b);
        }

        public Vector3f color() {
            return new Vector3f(r, g, b);
        }
    }
}
