package com.github.misosouptgit.aps.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared common config. Stored as {@code config/aps-common.toml}.
 */
public final class ModConfig {
	private static final Logger LOGGER = LogManager.getLogger("AlwaysPlainsSpawn/Config");

	public enum Algorithm {
		LOCATE,
		SPIRAL;

		public static Algorithm parse(String raw) {
			if (raw == null) return LOCATE;
			String v = raw.trim().toLowerCase(Locale.ROOT);
			if (v.equals("spiral")) return SPIRAL;
			return LOCATE;
		}
	}

	private static int searchRadius = defaultRadius();
	private static int searchStep = 8;
	private static Algorithm algorithm = Algorithm.LOCATE;
	private static boolean fallbackToVanilla = true;

	private ModConfig() {}

	private static int defaultRadius() {
		// Wider default on older noise/biome layouts; tighter on modern 3d noise.
		//? if >=1.18.2 {
		return 6400;
		//?} else {
		/*return 10240;*/
		//?}
	}

	public static int searchRadius() {
		return searchRadius;
	}

	public static int searchStep() {
		return searchStep;
	}

	public static Algorithm algorithm() {
		return algorithm;
	}

	public static boolean fallbackToVanilla() {
		return fallbackToVanilla;
	}

	public static void load() {
		Path path = Paths.get("config", "aps-common.toml");
		try {
			if (!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				writeDefaults(path);
				return;
			}
			Map<String, String> values = parseSimpleToml(path);
			searchRadius = parseInt(values.get("search_radius"), defaultRadius());
			searchStep = Math.max(1, parseInt(values.get("search_step"), 8));
			algorithm = Algorithm.parse(values.get("algorithm"));
			fallbackToVanilla = parseBool(values.get("fallback_to_vanilla"), true);
		} catch (Throwable t) {
			LOGGER.warn("Failed to load config; using defaults.", t);
			searchRadius = defaultRadius();
			searchStep = 8;
			algorithm = Algorithm.LOCATE;
			fallbackToVanilla = true;
		}
	}

	private static void writeDefaults(Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("# Always Plains Spawn\n");
			writer.write("# Target biome is always minecraft:plains (not configurable).\n");
			writer.write("#\n");
			writer.write("# search_radius  - max search radius in blocks\n");
			writer.write("# search_step    - sampling interval in blocks\n");
			writer.write("# algorithm      - \"locate\" (BiomeSource) or \"spiral\"\n");
			writer.write("# fallback_to_vanilla - keep vanilla spawn if plains not found\n");
			writer.write("\n");
			writer.write("search_radius = " + defaultRadius() + "\n");
			writer.write("search_step = 8\n");
			writer.write("algorithm = \"locate\"\n");
			writer.write("fallback_to_vanilla = true\n");
		}
	}

	private static Map<String, String> parseSimpleToml(Path path) throws IOException {
		Map<String, String> map = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#") || line.startsWith("[")) continue;
				int eq = line.indexOf('=');
				if (eq <= 0) continue;
				String key = line.substring(0, eq).trim();
				String value = line.substring(eq + 1).trim();
				if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
					value = value.substring(1, value.length() - 1);
				}
				map.put(key, value);
			}
		}
		return map;
	}

	private static int parseInt(String raw, int fallback) {
		if (raw == null) return fallback;
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static boolean parseBool(String raw, boolean fallback) {
		if (raw == null) return fallback;
		String v = raw.trim().toLowerCase(Locale.ROOT);
		if (v.equals("true") || v.equals("yes") || v.equals("1")) return true;
		if (v.equals("false") || v.equals("no") || v.equals("0")) return false;
		return fallback;
	}
}
