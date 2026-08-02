package plus.dragons.visuality.config;

import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	public static EntityHitParticleConfig ENTITY_HIT_PARTICLES;
	public static EntityArmorParticleConfig ENTITY_ARMOR_PARTICLES;
	public static BlockAmbientParticleConfig BLOCK_AMBIENT_PARTICLES;
	public static BlockStepParticleConfig BLOCK_STEP_PARTICLES;
	
    public static final ModConfigSpec.BooleanValue SLIME_ENABLED;
    public static final ModConfigSpec.IntValue SLIME_COLOR;
    public static final ModConfigSpec.BooleanValue CHARGE_ENABLED;

    public static final ModConfigSpec.BooleanValue WATER_CIRCLE_ENABLED;
    public static final ModConfigSpec.BooleanValue WATER_CIRCLE_COLORED;
    public static final ModConfigSpec.IntValue WATER_CIRCLE_DENSITY;

    public static final ModConfigSpec SPEC;
	
	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		
		builder.push("slime");
		SLIME_ENABLED = builder
			.translation("config.visuality.option.slime")
			.comment("Slime Blobs Enabled")
			.define("enabled", true);
		SLIME_COLOR = builder
			.translation("config.visuality.option.slime.color")
			.comment("Slime blob RGB color (0x000000 - 0xFFFFFF)")
			.defineInRange("color", 0x88FF79, 0x000000, 0xFFFFFF);
		builder.pop();
		
		builder.push("charge");
		CHARGE_ENABLED = builder
			.translation("config.visuality.option.charge")
			.comment("Charge Particle Enabled")
			.define("enabled", true);
		builder.pop();
		
		builder.push("waterCircle");
		WATER_CIRCLE_ENABLED = builder
			.translation("config.visuality.option.waterCircle")
			.comment("Water Circles Enabled")
			.define("enabled", true);
		WATER_CIRCLE_COLORED = builder
			.translation("config.visuality.option.waterCircle.colored")
			.comment("Water Circles Colored")
			.define("colored", true);
		WATER_CIRCLE_DENSITY = builder
			.translation("config.visuality.option.waterCircle.density")
			.comment("Water Circles Density")
			.defineInRange("density", 16, 0, 64);
		builder.pop();
		
		SPEC = builder.build();
	}
	
	public static void registerClientResourceListener(RegisterClientReloadListenersEvent event) {
	    event.registerReloadListener(ENTITY_HIT_PARTICLES = new EntityHitParticleConfig());
	    event.registerReloadListener(ENTITY_ARMOR_PARTICLES = new EntityArmorParticleConfig());
	    event.registerReloadListener(BLOCK_AMBIENT_PARTICLES = new BlockAmbientParticleConfig());
	    event.registerReloadListener(BLOCK_STEP_PARTICLES = new BlockStepParticleConfig());
	}
	
}
