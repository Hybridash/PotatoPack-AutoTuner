package com.hybridash.autotuner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saved in config/autotuner.json. Remembers that we already tuned (so we never overwrite
 * settings the player changed later) and keeps a backup of their old settings for /autotune undo.
 */
public class TunerState {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autotuner.json");

	/** Set to false to stop AutoTuner from doing anything on first launch. */
	public boolean enabled = true;
	public boolean tuned = false;
	public String appliedProfile = null;
	public String detectedHardware = null;
	public Snapshot backup = null;

	public static TunerState load() {
		try {
			if (Files.exists(FILE)) {
				TunerState s = GSON.fromJson(Files.readString(FILE), TunerState.class);
				if (s != null) return s;
			}
		} catch (Exception e) {
			AutoTunerClient.LOGGER.warn("Couldn't read {}, starting fresh", FILE, e);
		}
		return new TunerState();
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(this));
		} catch (IOException e) {
			AutoTunerClient.LOGGER.warn("Couldn't save {}", FILE, e);
		}
	}

	/** The player's settings from before AutoTuner touched them. */
	public static class Snapshot {
		int renderDistance;
		int simulationDistance;
		String graphics;
		String clouds;
		String particles;
		boolean smoothLighting;
		int biomeBlend;
		double entityDistance;

		public static Snapshot of(GameOptions o) {
			Snapshot s = new Snapshot();
			s.renderDistance = o.getViewDistance().getValue();
			s.simulationDistance = o.getSimulationDistance().getValue();
			s.graphics = o.getGraphicsMode().getValue().name();
			s.clouds = o.getCloudRenderMode().getValue().name();
			s.particles = o.getParticles().getValue().name();
			s.smoothLighting = o.getAo().getValue();
			s.biomeBlend = o.getBiomeBlendRadius().getValue();
			s.entityDistance = o.getEntityDistanceScaling().getValue();
			return s;
		}

		public void restore(GameOptions o) {
			o.getViewDistance().setValue(renderDistance);
			o.getSimulationDistance().setValue(simulationDistance);
			o.getGraphicsMode().setValue(GraphicsMode.valueOf(graphics));
			o.getCloudRenderMode().setValue(CloudRenderMode.valueOf(clouds));
			o.getParticles().setValue(ParticlesMode.valueOf(particles));
			o.getAo().setValue(smoothLighting);
			o.getBiomeBlendRadius().setValue(biomeBlend);
			o.getEntityDistanceScaling().setValue(entityDistance);
			o.write();
		}
	}
}
