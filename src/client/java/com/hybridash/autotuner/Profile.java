package com.hybridash.autotuner;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/** The four settings presets. Render distance is the one that matters most for FPS. */
public enum Profile {
	//            render sim  graphics            clouds                particles               AO     blend entities
	POTATO("Potato", 4, 5, GraphicsMode.FAST, CloudRenderMode.OFF, ParticlesMode.MINIMAL, false, 0, 0.5),
	LOW("Low", 6, 6, GraphicsMode.FAST, CloudRenderMode.OFF, ParticlesMode.DECREASED, true, 1, 0.75),
	MEDIUM("Medium", 10, 8, GraphicsMode.FANCY, CloudRenderMode.FAST, ParticlesMode.ALL, true, 2, 1.0),
	HIGH("High", 16, 10, GraphicsMode.FANCY, CloudRenderMode.FANCY, ParticlesMode.ALL, true, 3, 1.0);

	public final String displayName;
	final int renderDistance;
	final int simulationDistance;
	final GraphicsMode graphics;
	final CloudRenderMode clouds;
	final ParticlesMode particles;
	final boolean smoothLighting;
	final int biomeBlend;
	final double entityDistance;

	Profile(String displayName, int renderDistance, int simulationDistance, GraphicsMode graphics,
			CloudRenderMode clouds, ParticlesMode particles, boolean smoothLighting, int biomeBlend,
			double entityDistance) {
		this.displayName = displayName;
		this.renderDistance = renderDistance;
		this.simulationDistance = simulationDistance;
		this.graphics = graphics;
		this.clouds = clouds;
		this.particles = particles;
		this.smoothLighting = smoothLighting;
		this.biomeBlend = biomeBlend;
		this.entityDistance = entityDistance;
	}

	public void apply(GameOptions o) {
		o.getViewDistance().setValue(renderDistance);
		o.getSimulationDistance().setValue(simulationDistance);
		o.getGraphicsMode().setValue(graphics);
		o.getCloudRenderMode().setValue(clouds);
		o.getParticles().setValue(particles);
		o.getAo().setValue(smoothLighting);
		o.getBiomeBlendRadius().setValue(biomeBlend);
		o.getEntityDistanceScaling().setValue(entityDistance);
		o.write();
	}

	public String describe() {
		return displayName + ": render distance " + renderDistance + ", " + graphics.name().toLowerCase()
				+ " graphics, clouds " + clouds.name().toLowerCase() + ", particles " + particles.name().toLowerCase();
	}

	public static Profile byName(String name) {
		for (Profile p : values()) {
			if (p.name().equalsIgnoreCase(name)) return p;
		}
		return null;
	}
}
