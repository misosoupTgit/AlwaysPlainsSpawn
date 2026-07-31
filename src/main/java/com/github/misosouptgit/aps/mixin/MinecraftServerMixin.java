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
	 * New worlds: relocate immediately after vanilla picks the initial spawn,
	 * so subsequent spawn-chunk loading follows the plains position.
	 */
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
	 * Existing worlds (and any path that skipped setInitialSpawn): relocate once after levels load.
	 */
	@Inject(method = "loadLevel", at = @At("RETURN"))
	private void aps$afterLoadLevel(CallbackInfo ci) {
		PlainsSpawnRelocator.relocateOverworld((MinecraftServer) (Object) this);
	}
}
