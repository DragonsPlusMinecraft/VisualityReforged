package plus.dragons.visuality;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import plus.dragons.visuality.config.Config;
import plus.dragons.visuality.registry.VisualityParticles;
import plus.dragons.visuality.registry.VisualityRegistries;

@Mod(value = Visuality.ID, dist = Dist.CLIENT)
public class Visuality {
    public static final String ID = "visuality";
    private static final Logger LOGGER = LoggerFactory.getLogger("Visuality");
    
    public Visuality(IEventBus modBus, ModContainer container) {
        LOGGER.info("Start");
        this.onInitialize(modBus,container);
        LOGGER.info(container.getModInfo().toString());
        LOGGER.info("Visuality has initialized, have fun with more particles!");
    }
    
    public void onInitialize(IEventBus modBus, ModContainer container) {
        VisualityRegistries.Registers.register(modBus);
        modBus.addListener(Config::registerClientResourceListener);
        modBus.addListener(VisualityParticles::registerProviders);
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC, ID + "/config.toml");
    }
    
    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

}
