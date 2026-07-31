package com.github.misosouptgit.aps.spawn;

import com.github.misosouptgit.aps.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

//? if >=1.18.2 {
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
//?} else {
/*import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.BiomeSource;
import java.util.Random;*/
//?}

/**
 * Version-flexible plains biome lookup.
 */
public final class PlainsBiomeLookup {
	private PlainsBiomeLookup() {}

	@Nullable
	public static BlockPos findPlains(ServerLevel level, BlockPos origin, int radius, int step, ModConfig.Algorithm algorithm) {
		if (algorithm == ModConfig.Algorithm.SPIRAL) {
			BlockPos spiral = findSpiral(level, origin, radius, step);
			if (spiral != null) return spiral;
		}
		BlockPos located = findLocate(level, origin, radius, step);
		if (located != null) return located;
		if (algorithm != ModConfig.Algorithm.SPIRAL) {
			return findSpiral(level, origin, radius, step);
		}
		return null;
	}

	@Nullable
	private static BlockPos findLocate(ServerLevel level, BlockPos origin, int radius, int step) {
		int safeStep = Math.max(1, step);
		//? if >=1.19 {
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
				PlainsBiomeLookup::isPlainsHolder,
				origin,
				radius,
				safeStep,
				safeStep
		);
		return found == null ? null : found.getFirst();
		//?} else {
		/*//? if <1.18.2 {
		BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
		BlockPos found = source.findBiomeHorizontal(
				origin.getX(),
				64,
				origin.getZ(),
				radius,
				safeStep,
				PlainsBiomeLookup::isPlainsBiome,
				new Random(level.getSeed()),
				false
		);
		return found;
		//?} else {
		// 1.18.2: Climate sampler API differs; spiral fallback handles locate.
		return null;
		//?}
		*/
		//?}
	}

	@Nullable
	private static BlockPos findSpiral(ServerLevel level, BlockPos origin, int radius, int step) {
		int stepSize = Math.max(1, step);
		if (isPlainsAt(level, origin)) {
			return surface(level, origin.getX(), origin.getZ());
		}
		for (int r = stepSize; r <= radius; r += stepSize) {
			for (int dx = -r; dx <= r; dx += stepSize) {
				BlockPos a = probe(level, origin.getX() + dx, origin.getZ() - r);
				if (a != null) return a;
				BlockPos b = probe(level, origin.getX() + dx, origin.getZ() + r);
				if (b != null) return b;
			}
			for (int dz = -r + stepSize; dz <= r - stepSize; dz += stepSize) {
				BlockPos a = probe(level, origin.getX() - r, origin.getZ() + dz);
				if (a != null) return a;
				BlockPos b = probe(level, origin.getX() + r, origin.getZ() + dz);
				if (b != null) return b;
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos probe(ServerLevel level, int x, int z) {
		BlockPos sample = new BlockPos(x, sampleY(level), z);
		if (!isPlainsAt(level, sample)) return null;
		return surface(level, x, z);
	}

	private static int sampleY(ServerLevel level) {
		//? if >=1.18.2 {
		return level.getSeaLevel();
		//?} else {
		/*return 64;*/
		//?}
	}

	private static boolean isPlainsAt(ServerLevel level, BlockPos pos) {
		//? if >=1.18.2 {
		return isPlainsHolder(level.getBiome(pos));
		//?} else {
		/*return isPlainsBiome(level.getBiome(pos));*/
		//?}
	}

	//? if >=1.18.2 {
	private static boolean isPlainsHolder(Holder<Biome> holder) {
		return holder.is(Biomes.PLAINS);
	}
	//?} else {
	/*private static boolean isPlainsBiome(Biome biome) {
		return biome == BuiltinRegistries.BIOME.get(Biomes.PLAINS);
	}
	*/
	//?}

	private static BlockPos surface(ServerLevel level, int x, int z) {
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		return new BlockPos(x, y, z);
	}
}
