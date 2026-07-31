package com.github.misosouptgit.aps.spawn;

import com.github.misosouptgit.aps.AlwaysPlainsSpawn;
import com.github.misosouptgit.aps.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Relocates overworld spawn into Plains once per world.
 * {@link ServerLevel#setDefaultSpawnPos} also moves spawn-chunk tickets on MC &lt; 1.21.9.
 */
public final class PlainsSpawnRelocator {
	private static final String MARKER_FILE = "aps_relocated";

	private PlainsSpawnRelocator() {}

	public static void relocateIfNeeded(ServerLevel level) {
		if (level == null) return;
		if (level.dimension() != Level.OVERWORLD) return;

		MinecraftServer server = level.getServer();
		if (server == null) return;
		if (isMarked(server)) return;

		BlockPos origin = level.getSharedSpawnPos();
		BlockPos found = PlainsBiomeLookup.findPlains(
				level,
				origin,
				ModConfig.searchRadius(),
				ModConfig.searchStep(),
				ModConfig.algorithm()
		);

		if (found == null) {
			AlwaysPlainsSpawn.LOGGER.warn(
					"Could not find plains within radius {}; fallback_to_vanilla={}",
					ModConfig.searchRadius(),
					ModConfig.fallbackToVanilla()
			);
			if (ModConfig.fallbackToVanilla()) {
				mark(server);
			}
			return;
		}

		float angle = level.getSharedSpawnAngle();
		level.setDefaultSpawnPos(found, angle);
		mark(server);
		AlwaysPlainsSpawn.LOGGER.info(
				"World spawn relocated to plains at {}, {}, {}",
				found.getX(),
				found.getY(),
				found.getZ()
		);
	}

	public static void relocateOverworld(MinecraftServer server) {
		if (server == null) return;
		relocateIfNeeded(server.overworld());
	}

	private static Path markerPath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(MARKER_FILE);
	}

	private static boolean isMarked(MinecraftServer server) {
		try {
			return Files.exists(markerPath(server));
		} catch (Throwable t) {
			AlwaysPlainsSpawn.LOGGER.warn("Failed to read APS marker", t);
			return false;
		}
	}

	private static void mark(MinecraftServer server) {
		try {
			Path path = markerPath(server);
			Files.createDirectories(path.getParent());
			if (!Files.exists(path)) {
				Files.write(path, "relocated\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			}
		} catch (Throwable t) {
			AlwaysPlainsSpawn.LOGGER.warn("Failed to write APS marker", t);
		}
	}
}
