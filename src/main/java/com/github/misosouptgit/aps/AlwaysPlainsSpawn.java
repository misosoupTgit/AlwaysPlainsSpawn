package com.github.misosouptgit.aps;

import com.github.misosouptgit.aps.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AlwaysPlainsSpawn {
	public static final String MOD_ID = "aps";
	public static final Logger LOGGER = LogManager.getLogger("AlwaysPlainsSpawn");

	private static boolean initialized;

	private AlwaysPlainsSpawn() {}

	public static void init() {
		if (initialized) return;
		initialized = true;
		ModConfig.load();
		LOGGER.info("Always Plains Spawn initialized");
	}
}
