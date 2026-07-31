package com.github.misosouptgit.aps.platform.neoforge;

//? neoforge {
import com.github.misosouptgit.aps.AlwaysPlainsSpawn;
import net.neoforged.fml.common.Mod;

@Mod(AlwaysPlainsSpawn.MOD_ID)
public class NeoforgeEntrypoint {
	public NeoforgeEntrypoint() {
		AlwaysPlainsSpawn.init();
	}
}
//?}
