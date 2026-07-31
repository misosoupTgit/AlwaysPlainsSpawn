package com.github.misosouptgit.aps.mixin;

import com.github.misosouptgit.aps.spawn.PlainsSpawnRelocator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	/**
	 * Replace vanilla's chunk-walking spawn finder with a noise-only plains search.
	 * Cancelling avoids loading dozens of chunks before prepareLevels.
	 * Bonus-chest worlds keep the vanilla method so the chest still places; TAIL relocates after.
	 * Spawn-chunk removers remain compatible: we only write the spawn coordinate.
	 */
	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void aps$replaceInitialSpawn(
			ServerLevel level,
			ServerLevelData levelData,
			boolean generateBonusChest,
			boolean debug,
			CallbackInfo ci
	) {
		if (generateBonusChest) return;
		if (PlainsSpawnRelocator.trySetInitialPlainsSpawn(level, levelData, debug)) {
			ci.cancel();
		}
	}

	@Inject(method = "setInitialSpawn", at = @At("TAIL"))
	private static void aps$afterInitialSpawn(
			ServerLevel level,
			ServerLevelData levelData,
			boolean generateBonusChest,
			boolean debug,
			CallbackInfo ci
	) {
		PlainsSpawnRelocator.relocateIfNeeded(level);
	}

	/**
	 * Existing worlds that never got an APS marker: relocate once after load.
	 * New worlds are already marked in {@code setInitialSpawn}.
	 */
	@Inject(method = "loadLevel", at = @At("RETURN"))
	private void aps$afterLoadLevel(CallbackInfo ci) {
		PlainsSpawnRelocator.relocateOverworld((MinecraftServer) (Object) this);
	}
}
