package com.github.misosouptgit.aps.spawn;

import com.github.misosouptgit.aps.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

//? if >=1.19 {
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.RandomState;
//?} else if >=1.18.2 {
/*import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;*/
//?} else {
/*import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.Random;*/
//?}

/**
 * Fast plains lookup using biome noise only (no chunk generation during search).
 */
public final class PlainsBiomeLookup {
	private PlainsBiomeLookup() {}

	@Nullable
	public static BlockPos findPlains(ServerLevel level, BlockPos origin, int radius, int step, ModConfig.Algorithm algorithm) {
		int safeStep = Math.max(4, step);
		BlockPos candidate;
		if (algorithm == ModConfig.Algorithm.SPIRAL) {
			candidate = findSpiralNoise(level, origin, radius, safeStep);
			if (candidate == null) {
				candidate = findLocateThenValidate(level, origin, radius, safeStep);
			}
		} else {
			candidate = findLocateThenValidate(level, origin, radius, safeStep);
			if (candidate == null) {
				candidate = findSpiralNoise(level, origin, radius, safeStep);
			}
		}
		if (candidate == null) return null;
		return finalizeSurface(level, candidate.getX(), candidate.getZ());
	}

	@Nullable
	private static BlockPos findLocateThenValidate(ServerLevel level, BlockPos origin, int radius, int step) {
		//? if >=1.19 {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
				PlainsBiomeLookup::isPlainsHolder,
				origin,
				radius,
				step,
				step
		);
		if (found == null) return null;
		BlockPos pos = found.getFirst();
		if (acceptCandidate(level, pos.getX(), pos.getZ(), step)) return pos;
		return nudgeInterior(level, pos.getX(), pos.getZ(), step);
		//?} else if <1.18.2 {
		/*BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		BlockPos found = source.findBiomeHorizontal(
				origin.getX(),
				64,
				origin.getZ(),
				radius,
				step,
				PlainsBiomeLookup::isPlainsBiome,
				new Random(level.getSeed()),
				false
		);
		if (found == null) return null;
		if (acceptCandidate(level, found.getX(), found.getZ(), step)) return found;
		return nudgeInterior(level, found.getX(), found.getZ(), step);*/
		//?} else {
		/*return findSpiralNoise(level, origin, radius, step);*/
		//?}
	}

	@Nullable
	private static BlockPos nudgeInterior(ServerLevel level, int x, int z, int step) {
		int[][] dirs = {{step, 0}, {-step, 0}, {0, step}, {0, -step}, {step, step}, {step, -step}, {-step, step}, {-step, -step}};
		for (int[] d : dirs) {
			int nx = x + d[0] * Math.max(1, ModConfig.interiorChecks());
			int nz = z + d[1] * Math.max(1, ModConfig.interiorChecks());
			if (acceptCandidate(level, nx, nz, step)) {
				return new BlockPos(nx, 0, nz);
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos findSpiralNoise(ServerLevel level, BlockPos origin, int radius, int step) {
		if (acceptCandidate(level, origin.getX(), origin.getZ(), step)) {
			return new BlockPos(origin.getX(), 0, origin.getZ());
		}
		for (int r = step; r <= radius; r += step) {
			for (int dx = -r; dx <= r; dx += step) {
				if (acceptCandidate(level, origin.getX() + dx, origin.getZ() - r, step)) {
					return new BlockPos(origin.getX() + dx, 0, origin.getZ() - r);
				}
				if (acceptCandidate(level, origin.getX() + dx, origin.getZ() + r, step)) {
					return new BlockPos(origin.getX() + dx, 0, origin.getZ() + r);
				}
			}
			for (int dz = -r + step; dz <= r - step; dz += step) {
				if (acceptCandidate(level, origin.getX() - r, origin.getZ() + dz, step)) {
					return new BlockPos(origin.getX() - r, 0, origin.getZ() + dz);
				}
				if (acceptCandidate(level, origin.getX() + r, origin.getZ() + dz, step)) {
					return new BlockPos(origin.getX() + r, 0, origin.getZ() + dz);
				}
			}
		}
		return null;
	}

	private static boolean acceptCandidate(ServerLevel level, int x, int z, int step) {
		if (!isPlainsNoise(level, x, z)) return false;

		int checks = ModConfig.interiorChecks();
		if (checks > 0) {
			int d = step * checks;
			if (!isPlainsNoise(level, x + d, z)
					|| !isPlainsNoise(level, x - d, z)
					|| !isPlainsNoise(level, x, z + d)
					|| !isPlainsNoise(level, x, z - d)) {
				return false;
			}
		}

		int surfaceY = estimateSurfaceY(level, x, z);
		if (surfaceY > ModConfig.maxSurfaceY()) return false;
		if (!isFlatEnough(level, x, z, surfaceY)) return false;
		return hasNearbyAmenities(level, x, z);
	}

	/**
	 * Sparse flatness: sample cardinals/diagonals at radius and half-radius.
	 * Catches steep river cuts without dense getBaseHeight grids.
	 */
	private static boolean isFlatEnough(ServerLevel level, int x, int z, int centerY) {
		int radius = ModConfig.flatnessRadius();
		if (radius <= 0) return true;
		int maxDelta = ModConfig.flatnessMaxDelta();
		int half = Math.max(1, radius / 2);
		int[] offsets = {radius, half};
		for (int r : offsets) {
			if (!flatOk(level, x + r, z, centerY, maxDelta)
					|| !flatOk(level, x - r, z, centerY, maxDelta)
					|| !flatOk(level, x, z + r, centerY, maxDelta)
					|| !flatOk(level, x, z - r, centerY, maxDelta)) {
				return false;
			}
			if (r == radius) {
				if (!flatOk(level, x + r, z + r, centerY, maxDelta)
						|| !flatOk(level, x + r, z - r, centerY, maxDelta)
						|| !flatOk(level, x - r, z + r, centerY, maxDelta)
						|| !flatOk(level, x - r, z - r, centerY, maxDelta)) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean flatOk(ServerLevel level, int x, int z, int centerY, int maxDelta) {
		return Math.abs(estimateSurfaceY(level, x, z) - centerY) <= maxDelta;
	}

	/** Single spiral for water + forest; one noise sample per column. */
	private static boolean hasNearbyAmenities(ServerLevel level, int ox, int oz) {
		boolean needWater = ModConfig.requireWaterNearby();
		boolean needForest = ModConfig.requireForestNearby();
		if (!needWater && !needForest) return true;

		boolean foundWater = !needWater;
		boolean foundForest = !needForest;
		int radius = ModConfig.nearbyCheckRadius();
		int step = Math.max(32, ModConfig.searchStep() / 2);
		for (int r = step; r <= radius; r += step) {
			for (int dx = -r; dx <= r; dx += step) {
				int top = amenityFlags(level, ox + dx, oz - r);
				foundWater |= needWater && (top & 1) != 0;
				foundForest |= needForest && (top & 2) != 0;
				if (foundWater && foundForest) return true;
				int bot = amenityFlags(level, ox + dx, oz + r);
				foundWater |= needWater && (bot & 1) != 0;
				foundForest |= needForest && (bot & 2) != 0;
				if (foundWater && foundForest) return true;
			}
			for (int dz = -r + step; dz <= r - step; dz += step) {
				int left = amenityFlags(level, ox - r, oz + dz);
				foundWater |= needWater && (left & 1) != 0;
				foundForest |= needForest && (left & 2) != 0;
				if (foundWater && foundForest) return true;
				int right = amenityFlags(level, ox + r, oz + dz);
				foundWater |= needWater && (right & 1) != 0;
				foundForest |= needForest && (right & 2) != 0;
				if (foundWater && foundForest) return true;
			}
		}
		return foundWater && foundForest;
	}

	/** bit0 = water, bit1 = forest */
	private static int amenityFlags(ServerLevel level, int x, int z) {
		//? if >=1.18.2 {
		Holder<Biome> h = noiseBiome(level, x, z);
		int flags = 0;
		if (h.is(BiomeTags.IS_RIVER) || h.is(BiomeTags.IS_OCEAN) || h.is(BiomeTags.IS_BEACH)) flags |= 1;
		if (h.is(BiomeTags.IS_FOREST)) flags |= 2;
		return flags;
		//?} else {
		/*int flags = 0;
		if (nameContains(level, x, z, "river", "ocean", "beach", "shore")) flags |= 1;
		if (nameContains(level, x, z, "forest", "dark_forest", "birch")) flags |= 2;
		return flags;*/
		//?}
	}

	private static boolean isPlainsNoise(ServerLevel level, int x, int z) {
		//? if >=1.18.2 {
		return isPlainsHolder(noiseBiome(level, x, z));
		//?} else {
		/*return isPlainsBiome(level.getBiome(new BlockPos(x, 64, z)));*/
		//?}
	}

	//? if >=1.19 {
	/** Sea-level Y is enough for surface biome tags; avoids getBaseHeight on every query. */
	private static Holder<Biome> noiseBiome(ServerLevel level, int x, int z) {
		int y = level.getSeaLevel();
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		RandomState randomState = level.getChunkSource().randomState();
		return source.getNoiseBiome(x >> 2, y >> 2, z >> 2, randomState.sampler());
	}

	private static boolean isPlainsHolder(Holder<Biome> holder) {
		return holder.is(Biomes.PLAINS);
	}
	//?} else if >=1.18.2 {
	/*private static Holder<Biome> noiseBiome(ServerLevel level, int x, int z) {
		return level.getNoiseBiome(x >> 2, level.getSeaLevel() >> 2, z >> 2);
	}

	private static boolean isPlainsHolder(Holder<Biome> holder) {
		return holder.is(Biomes.PLAINS);
	}*/
	//?} else {
	/*private static boolean nameContains(ServerLevel level, int x, int z, String... parts) {
		Biome biome = level.getBiome(new BlockPos(x, 64, z));
		ResourceLocation id = BuiltinRegistries.BIOME.getKey(biome);
		if (id == null) return false;
		String path = id.getPath();
		for (String p : parts) {
			if (path.contains(p)) return true;
		}
		return false;
	}

	private static boolean isPlainsBiome(Biome biome) {
		ResourceLocation id = BuiltinRegistries.BIOME.getKey(biome);
		return id != null && id.getPath().equals("plains");
	}*/
	//?}

	private static int estimateSurfaceY(ServerLevel level, int x, int z) {
		try {
			//? if >=1.19 {
			return level.getChunkSource().getGenerator().getBaseHeight(
					x,
					z,
					Heightmap.Types.WORLD_SURFACE_WG,
					level,
					level.getChunkSource().randomState()
			);
			//?} else if >=1.17.1 {
			/*return level.getChunkSource().getGenerator().getBaseHeight(
					x,
					z,
					Heightmap.Types.WORLD_SURFACE_WG,
					level
			);*/
			//?} else {
			/*return level.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG);*/
			//?}
		} catch (Throwable t) {
			//? if >=1.17.1 {
			return level.getSeaLevel();
			//?} else {
			/*return 64;*/
			//?}
		}
	}

	private static BlockPos finalizeSurface(ServerLevel level, int x, int z) {
		int y = estimateSurfaceY(level, x, z);
		//? if >=1.21.11 {
		/*int minY = level.getMinY();*/
		//?} else if >=1.17.1 {
		int minY = level.getMinBuildHeight();
		//?} else {
		/*int minY = 0;*/
		//?}
		return new BlockPos(x, Math.max(y, minY + 1), z);
	}
}
