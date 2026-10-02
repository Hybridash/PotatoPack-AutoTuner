package com.hybridash.autotuner;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * On the very first launch, look at the PC and pick video settings that fit it.
 * After that it never touches your settings again unless you run /autotune.
 */
public class AutoTunerClient implements ClientModInitializer {
	public static final String MOD_ID = "autotuner";
	public static final Logger LOGGER = LoggerFactory.getLogger("AutoTuner");

	private static HardwareInfo hardware;
	private static TunerState state;

	@Override
	public void onInitializeClient() {
		state = TunerState.load();
		ClientLifecycleEvents.CLIENT_STARTED.register(AutoTunerClient::onStarted);
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(buildCommand()));
	}

	private static void onStarted(MinecraftClient client) {
		// CLIENT_STARTED runs on the render thread with the OpenGL context ready, so we can ask for the GPU name.
		hardware = HardwareInfo.detect();
		LOGGER.info("Detected hardware: {} -> tiers gpu={} cpu={} ram={} cap={}", hardware.summary(),
				hardware.gpuTier(), hardware.cpuTier(), hardware.ramTier(), hardware.allocatedCap());

		if (!state.enabled || state.tuned) return;

		Profile profile = hardware.pickProfile();
		state.backup = TunerState.Snapshot.of(client.options);
		profile.apply(client.options);
		state.tuned = true;
		state.appliedProfile = profile.name();
		state.detectedHardware = hardware.summary();
		state.save();

		LOGGER.info("First launch: applied the {} profile", profile.displayName);
		SystemToast.add(client.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION,
				Text.literal("AutoTuner: " + profile.displayName + " settings"),
				Text.literal("Picked for your PC. Type /autotune to change it."));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
		var root = ClientCommandManager.literal("autotune").executes(ctx -> {
			info(ctx.getSource());
			return 1;
		});

		root.then(ClientCommandManager.literal("auto").executes(ctx -> {
			apply(ctx.getSource(), hardware != null ? hardware.pickProfile() : Profile.MEDIUM);
			return 1;
		}));
		for (Profile p : Profile.values()) {
			root.then(ClientCommandManager.literal(p.name().toLowerCase()).executes(ctx -> {
				apply(ctx.getSource(), p);
				return 1;
			}));
		}
		root.then(ClientCommandManager.literal("undo").executes(ctx -> {
			undo(ctx.getSource());
			return 1;
		}));
		return root;
	}

	private static void info(FabricClientCommandSource src) {
		src.sendFeedback(Text.literal("AutoTuner").formatted(Formatting.GOLD, Formatting.BOLD));
		if (hardware != null) {
			src.sendFeedback(Text.literal(hardware.summary()).formatted(Formatting.GRAY));
			src.sendFeedback(Text.literal("Recommended: " + hardware.pickProfile().describe()));
		}
		if (state.appliedProfile != null) {
			src.sendFeedback(Text.literal("Last applied: " + state.appliedProfile.toLowerCase()).formatted(Formatting.GRAY));
		}
		src.sendFeedback(Text.literal("/autotune auto | potato | low | medium | high | undo").formatted(Formatting.YELLOW));
	}

	private static void apply(FabricClientCommandSource src, Profile profile) {
		MinecraftClient client = src.getClient();
		if (state.backup == null) state.backup = TunerState.Snapshot.of(client.options);
		profile.apply(client.options);
		reloadWorld(client);
		state.tuned = true;
		state.appliedProfile = profile.name();
		state.save();
		src.sendFeedback(Text.literal("Applied " + profile.describe()).formatted(Formatting.GREEN));
	}

	private static void undo(FabricClientCommandSource src) {
		if (state.backup == null) {
			src.sendError(Text.literal("Nothing to undo: AutoTuner hasn't changed your settings."));
			return;
		}
		MinecraftClient client = src.getClient();
		state.backup.restore(client.options);
		reloadWorld(client);
		state.backup = null;
		state.appliedProfile = null;
		state.save();
		src.sendFeedback(Text.literal("Put your old settings back.").formatted(Formatting.GREEN));
	}

	/** Graphics mode and render distance changes need the world renderer rebuilt to show up. */
	private static void reloadWorld(MinecraftClient client) {
		if (client.world != null && client.worldRenderer != null) {
			client.worldRenderer.reload();
		}
	}
}
