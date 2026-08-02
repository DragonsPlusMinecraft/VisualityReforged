package plus.dragons.visuality;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.dragons.visuality.config.Config;
import plus.dragons.visuality.registry.VisualityParticles;
import plus.dragons.visuality.registry.VisualityRegistries;

@Mod(Visuality.ID)
public class Visuality {
    public static final String ID = "visuality";
    private static final Logger LOGGER = LoggerFactory.getLogger("Visuality");
    
    public Visuality(FMLJavaModLoadingContext context) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            this.onInitialize(context);
            LOGGER.info("Visuality has initialized, have fun with more particles!");
        } else {
            LOGGER.warn("Visuality is installed on a dedicated server, skip loading...");
        }
    }
    
    public void onInitialize(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.CLIENT, Config.SPEC, ID + "/config.toml");
        
        IEventBus modBus = context.getModEventBus();
        VisualityRegistries.Registers.register(modBus);
        VisualityParticles.register();
        
        modBus.addListener(Config::registerClientResourceListener);
        modBus.addListener(VisualityParticles::registerProviders);
    }
    
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

}
