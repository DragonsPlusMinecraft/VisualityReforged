package plus.dragons.visuality.config;

import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	public static EntityHitParticleConfig ENTITY_HIT_PARTICLES;
	public static EntityArmorParticleConfig ENTITY_ARMOR_PARTICLES;
	public static BlockAmbientParticleConfig BLOCK_AMBIENT_PARTICLES;
	public static BlockStepParticleConfig BLOCK_STEP_PARTICLES;
	
    public static final ModConfigSpec.BooleanValue SLIME_ENABLED;
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
	
	public static void registerClientResourceListener(AddClientReloadListenersEvent event) {
		ENTITY_HIT_PARTICLES = new EntityHitParticleConfig();
		ENTITY_ARMOR_PARTICLES = new EntityArmorParticleConfig();
		BLOCK_AMBIENT_PARTICLES = new BlockAmbientParticleConfig();
		BLOCK_STEP_PARTICLES = new BlockStepParticleConfig();
	    event.addListener(ENTITY_HIT_PARTICLES.id, ENTITY_HIT_PARTICLES);
		event.addListener(ENTITY_HIT_PARTICLES.id, ENTITY_ARMOR_PARTICLES);
		event.addListener(ENTITY_HIT_PARTICLES.id, BLOCK_AMBIENT_PARTICLES);
		event.addListener(ENTITY_HIT_PARTICLES.id, BLOCK_STEP_PARTICLES);
	}
	
}
