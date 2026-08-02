package plus.dragons.visuality.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

/**
 * <p>A {@link Codec} backed by a vanilla {@link Registry} and a custom {@link Registry}.
 * <p>Compressed {@link DynamicOps} are only available for vanilla registries,
 * this {@link Codec} should be used with {@link JsonOps#INSTANCE}.
 * @param <A>
 * @author LimonBlaze
 */
public class CompositeRegistryCodec<A> implements Codec<A> {
    private final Codec<A> primaryCodec;
    private final ResourceKey<Registry<A>> secondaryKey;
    private Codec<A> secondaryCodec;
    
    private CompositeRegistryCodec(Codec<A> primaryCodec, ResourceKey<Registry<A>> secondaryKey) {
        this.primaryCodec = primaryCodec;
        this.secondaryKey = secondaryKey;
    }
    
    public static <T> CompositeRegistryCodec<T> of(Codec<T> primaryCodec, ResourceKey<Registry<T>> secondaryKey) {
        return new CompositeRegistryCodec<>(primaryCodec, secondaryKey);
    }
    
    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        final var result = primaryCodec.decode(ops, input);
        if (result.error().isEmpty() || ops.compressMaps())
            return result;
        if (secondaryCodec == null) {
            Registry<A> registry = (Registry<A>) BuiltInRegistries.REGISTRY.get(secondaryKey.identifier()).get().value();
            if (registry == null)
                return result;
            secondaryCodec = registry.byNameCodec();
        }
        return secondaryCodec.decode(ops, input);
    }
    
    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        final var result = primaryCodec.encode(input, ops, prefix);
        if (result.error().isEmpty() || ops.compressMaps())
            return result;
        if (secondaryCodec == null) {
            Registry<A> registry = (Registry<A>) BuiltInRegistries.REGISTRY.get(secondaryKey.identifier()).get().value();
            if (registry == null)
                return result;
            secondaryCodec = registry.byNameCodec();
        }
        return secondaryCodec.encode(input, ops, prefix);
    }
    
}
