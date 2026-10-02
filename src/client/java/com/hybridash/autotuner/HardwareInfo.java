package com.hybridash.autotuner;

import org.lwjgl.opengl.GL11;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Looks at the GPU, CPU and RAM and turns each one into a tier from 0 (potato) to 3 (strong).
 * The final profile is the weakest of the three, because the slowest part is what holds FPS back.
 */
public record HardwareInfo(String gpu, String gpuVendor, int cpuCores, long totalRamMb, long allocatedRamMb) {

	/** Must be called on the render thread (the OpenGL context has to exist). */
	public static HardwareInfo detect() {
		String renderer = safe(GL11.glGetString(GL11.GL_RENDERER));
		String vendor = safe(GL11.glGetString(GL11.GL_VENDOR));
		int cores = Runtime.getRuntime().availableProcessors();
		long allocated = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		long total = -1;
		try {
			var os = ManagementFactory.getOperatingSystemMXBean();
			if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
				total = sun.getTotalMemorySize() / (1024 * 1024);
			}
		} catch (Throwable ignored) {
			// Some JVMs don't expose this. We fall back to the allocated amount below.
		}
		if (total <= 0) total = allocated * 2;
		return new HardwareInfo(renderer, vendor, cores, total, allocated);
	}

	private static String safe(String s) {
		return s == null ? "unknown" : s.trim();
	}

	// ---- GPU ------------------------------------------------------------------------------

	/** Checked top to bottom, first match wins. */
	private static final Object[][] GPU_RULES = {
			// Software rendering / no real GPU driver: this is as bad as it gets
			{"llvmpipe|softpipe|swiftshader|microsoft basic render|gdi generic|virgl", 0},
			// Phone / single-board GPUs (Pojav, Raspberry Pi, etc.)
			{"mali|adreno|powervr|videocore|v3d", 0},
			// Strong dedicated cards
			{"rtx|radeon rx ?[5-9]\\d{3}|radeon rx ?7\\d{2}\\b|arc a[57]\\d{2}|apple m\\d (pro|max|ultra)", 3},
			// Decent dedicated cards and good integrated
			{"gtx ?(9[6-9]0|10[5-8]0|16\\d0)|radeon rx ?[45]\\d{2}\\b|iris xe|arc|apple m\\d|radeon \\d{3}m|radeon pro", 2},
			// Old or weak dedicated cards
			{"gtx|geforce gt |geforce \\d{3}m|quadro|radeon r[579]|radeon hd", 1},
			// Older integrated graphics
			{"intel.*(hd|uhd) graphics|intel\\(r\\) graphics|radeon\\(tm\\) graphics|radeon graphics|vega", 1},
			// Older Apple GPUs (Intel Macs)
			{"intel iris|iris pro|iris plus", 1},
	};

	public int gpuTier() {
		String g = (gpu + " " + gpuVendor).toLowerCase(Locale.ROOT);
		for (Object[] rule : GPU_RULES) {
			if (Pattern.compile((String) rule[0]).matcher(g).find()) return (int) rule[1];
		}
		return 2; // Unknown GPU: assume it's average rather than punishing it
	}

	// ---- CPU / RAM ------------------------------------------------------------------------

	public int cpuTier() {
		if (cpuCores <= 2) return 0;
		if (cpuCores <= 4) return 2;
		return 3;
	}

	public int ramTier() {
		if (totalRamMb <= 3 * 1024) return 0;
		if (totalRamMb <= 4608) return 1;   // 4 GB machines
		if (totalRamMb <= 8704) return 2;   // 8 GB machines
		return 3;
	}

	/** If the launcher only gave Minecraft a little RAM, high render distance will run out of memory. */
	public int allocatedCap() {
		if (allocatedRamMb < 1600) return 1;
		if (allocatedRamMb < 2500) return 2;
		return 3;
	}

	public Profile pickProfile() {
		int tier = Math.min(Math.min(gpuTier(), cpuTier()), Math.min(ramTier(), allocatedCap()));
		return Profile.values()[Math.max(0, Math.min(3, tier))];
	}

	public String summary() {
		return String.format(Locale.ROOT, "GPU: %s | CPU: %d threads | RAM: %.1f GB (Minecraft gets %.1f GB)",
				gpu, cpuCores, totalRamMb / 1024.0, allocatedRamMb / 1024.0);
	}
}
