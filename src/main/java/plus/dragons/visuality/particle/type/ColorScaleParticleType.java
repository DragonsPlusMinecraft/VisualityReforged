package plus.dragons.visuality.particle.type;

import com.mojang.serialization.Codec;
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


public class ColorScaleParticleType extends ParticleType<ColorScaleParticleType.Options> {
    private final MapCodec<Options> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, Options> streamCodec;
    
    public ColorScaleParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
        this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.VECTOR3F.fieldOf("color")
                .forGetter(Options::color),
            Codec.FLOAT.fieldOf("scale")
                .forGetter(Options::scale)
        ).apply(instance, Options::new));
        this.streamCodec = StreamCodec.ofMember(Options::encode,Options::new);
    }
    
    public ColorScaleParticleType() {
        this(false);
    }
    
    public Options withColor(float r, float g, float b) {
        return new Options(r, g, b, 1);
    }
    
    public Options withColor(int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        return new Options(r, g, b, 1);
    }
    
    public Options withColorAndScale(float r, float g, float b, float scale) {
        return new Options(r, g, b, scale);
    }
    
    public Options withColorAndScale(int color, float scale) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        return new Options(r, g, b, scale);
    }
    
    @Override
    public MapCodec<Options> codec() {
        return codec;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, Options> streamCodec() {
        return streamCodec;
    }

    public class Options implements ParticleOptions {
        public final float r;
        public final float g;
        public final float b;
        public final float scale;
    
        private Options(float r, float g, float b, float scale) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.scale = scale;
        }

        private Options(FriendlyByteBuf buffer) {
            this.r = buffer.readFloat();
            this.g = buffer.readFloat();
            this.b = buffer.readFloat();
            this.scale = buffer.readFloat();
        }
        
        private Options(Vector3fc vec, float scale) {
            this(vec.x(), vec.y(), vec.z(), scale);
        }
        
        public Vector3f color() {
            return new Vector3f(r, g, b);
        }
        
        public float scale() {
            return scale;
        }
    
        @Override
        public ParticleType<?> getType() {
            return ColorScaleParticleType.this;
        }

        public void encode(FriendlyByteBuf buffer) {
            buffer.writeFloat(r);
            buffer.writeFloat(g);
            buffer.writeFloat(b);
            buffer.writeFloat(scale);
        }
    }
}
