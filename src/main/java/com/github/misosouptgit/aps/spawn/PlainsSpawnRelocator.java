package com.github.misosouptgit.aps.spawn;

import com.github.misosouptgit.aps.AlwaysPlainsSpawn;
import com.github.misosouptgit.aps.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.ServerLevelData;

//? if >=1.21.11 {
/*import net.minecraft.world.level.storage.LevelData;*/
//?}

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Relocates overworld spawn into Plains once per world.
 * <p>
 * Only updates the world-spawn coordinate. Spawn-chunk tickets are left to vanilla
 * {@code prepareLevels} / gamerules, so spawn-chunk removers (e.g. Ksyxis) stay compatible:
 * we never forceload or re-ticket chunks ourselves.
 */
public final class PlainsSpawnRelocator {
	private static final String MARKER_FILE = "alwaysplainsspawn_relocated";

	private PlainsSpawnRelocator() {}

	/**
	 * Used from {@code setInitialSpawn} HEAD to skip vanilla's expensive chunk walk.
	 *
	 * @return true if plains spawn was applied (caller should cancel vanilla method)
	 */
	public static boolean trySetInitialPlainsSpawn(ServerLevel level, ServerLevelData levelData, boolean debug) {
		if (debug) return false;
		if (level == null || levelData == null) return false;
		if (level.dimension() != Level.OVERWORLD) return false;

		MinecraftServer server = level.getServer();
		if (server == null) return false;
		if (isMarked(server)) return false;

		BlockPos origin = new BlockPos(0, seaLevel(level), 0);
		BlockPos found = PlainsBiomeLookup.findPlains(
				level,
				origin,
				ModConfig.searchRadius(),
				ModConfig.searchStep(),
				ModConfig.algorithm()
		);

		if (found == null) {
			AlwaysPlainsSpawn.LOGGER.warn(
					"Could not find plains within radius {} (noise search); leaving vanilla spawn logic",
					ModConfig.searchRadius()
			);
			if (ModConfig.fallbackToVanilla()) {
				mark(server);
			}
			return false;
		}

		applySpawn(level, levelData, found, 0.0F);
		mark(server);
		AlwaysPlainsSpawn.LOGGER.info(
				"Initial world spawn set to plains at {}, {}, {} (noise search)",
				found.getX(),
				found.getY(),
				found.getZ()
		);
		return true;
	}

	public static void relocateIfNeeded(ServerLevel level) {
		if (level == null) return;
		if (level.dimension() != Level.OVERWORLD) return;

		MinecraftServer server = level.getServer();
		if (server == null) return;
		if (isMarked(server)) return;

		BlockPos origin = currentSpawnPos(level);
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

		applySpawn(level, (ServerLevelData) level.getLevelData(), found, currentSpawnAngle(level));
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

	private static BlockPos currentSpawnPos(ServerLevel level) {
		//? if >=1.21.11 {
		/*return level.getRespawnData().pos();*/
		//?} else {
		return level.getSharedSpawnPos();
		//?}
	}

	private static float currentSpawnAngle(ServerLevel level) {
		//? if >=1.21.11 {
		/*return level.getRespawnData().yaw();*/
		//?} else {
		return level.getSharedSpawnAngle();
		//?}
	}

	private static void applySpawn(ServerLevel level, ServerLevelData levelData, BlockPos pos, float yaw) {
		//? if >=1.21.11 {
		/*LevelData.RespawnData data = LevelData.RespawnData.of(Level.OVERWORLD, pos, yaw, 0.0F);
		levelData.setSpawn(data);
		level.setRespawnData(data);*/
		//?} else {
		levelData.setSpawn(pos, yaw);
		level.setDefaultSpawnPos(pos, yaw);
		//?}
	}

	private static Path markerPath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(MARKER_FILE);
	}

	private static int seaLevel(ServerLevel level) {
		//? if >=1.17.1 {
		return level.getSeaLevel();
		//?} else {
		/*return 64;*/
		//?}
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
