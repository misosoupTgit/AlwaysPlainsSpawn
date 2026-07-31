package com.github.misosouptgit.aps.mixin;

import com.github.misosouptgit.aps.spawn.PlainsSpawnRelocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ensures plains relocation has run before bed-less respawn uses world spawn.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	//? if >=1.21 {
	@Inject(method = "respawn", at = @At("HEAD"))
	private void aps$beforeRespawn(
			ServerPlayer player,
			boolean keepInventory,
			net.minecraft.world.entity.Entity.RemovalReason reason,
			CallbackInfoReturnable<ServerPlayer> cir
	) {
		if (player != null && player.server != null) {
			PlainsSpawnRelocator.relocateOverworld(player.server);
		}
	}
	//?} else {
	/*@Inject(method = "respawn", at = @At("HEAD"))
	private void aps$beforeRespawn(ServerPlayer player, boolean keepInventory, CallbackInfoReturnable<ServerPlayer> cir) {
		if (player != null && player.server != null) {
			PlainsSpawnRelocator.relocateOverworld(player.server);
		}
	}
	*///?}
}
