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
 * Shared common config. Stored as {@code config/alwaysplainsspawn-common.toml}.
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
	private static int searchStep = defaultStep();
	private static Algorithm algorithm = Algorithm.LOCATE;
	private static boolean fallbackToVanilla = true;
	private static int interiorChecks = 2;
	private static int maxSurfaceY = 65;
	private static boolean requireWaterNearby = true;
	private static boolean requireForestNearby = true;
	private static int nearbyCheckRadius = 96;
	private static int flatnessRadius = 24;
	private static int flatnessMaxDelta = 2;

	private ModConfig() {}

	private static int defaultRadius() {
		return 4092;
	}

	private static int defaultStep() {
		return 64;
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

	public static int interiorChecks() {
		return interiorChecks;
	}

	/** Surface Y must be &lt;= this. Use a very large value to effectively disable. */
	public static int maxSurfaceY() {
		return maxSurfaceY;
	}

	public static boolean requireWaterNearby() {
		return requireWaterNearby;
	}

	public static boolean requireForestNearby() {
		return requireForestNearby;
	}

	public static int nearbyCheckRadius() {
		return nearbyCheckRadius;
	}

	/** Chebyshev radius around the candidate for flatness sampling. */
	public static int flatnessRadius() {
		return flatnessRadius;
	}

	/** Max allowed surface-Y difference within flatness_radius (default 2). */
	public static int flatnessMaxDelta() {
		return flatnessMaxDelta;
	}

	public static void load() {
		Path path = Paths.get("config", "alwaysplainsspawn-common.toml");
		try {
			if (!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				writeDefaults(path);
				return;
			}
			Map<String, String> values = parseSimpleToml(path);
			searchRadius = Math.max(64, parseInt(values.get("search_radius"), defaultRadius()));
			searchStep = Math.max(4, parseInt(values.get("search_step"), defaultStep()));
			algorithm = Algorithm.parse(values.get("algorithm"));
			fallbackToVanilla = parseBool(values.get("fallback_to_vanilla"), true);
			interiorChecks = Math.max(0, parseInt(values.get("interior_checks"), 2));
			maxSurfaceY = parseInt(values.get("max_surface_y"), 65);
			requireWaterNearby = parseBool(values.get("require_water_nearby"), true);
			requireForestNearby = parseBool(values.get("require_forest_nearby"), true);
			nearbyCheckRadius = Math.max(16, parseInt(values.get("nearby_check_radius"), 96));
			flatnessRadius = Math.max(0, parseInt(values.get("flatness_radius"), 24));
			flatnessMaxDelta = Math.max(0, parseInt(values.get("flatness_max_delta"), 2));
		} catch (Throwable t) {
			LOGGER.warn("Failed to load config; using defaults.", t);
			searchRadius = defaultRadius();
			searchStep = defaultStep();
			algorithm = Algorithm.LOCATE;
			fallbackToVanilla = true;
			interiorChecks = 2;
			maxSurfaceY = 65;
			requireWaterNearby = true;
			requireForestNearby = true;
			nearbyCheckRadius = 96;
			flatnessRadius = 24;
			flatnessMaxDelta = 2;
		}
	}

	private static void writeDefaults(Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write("# Always Plains Spawn\n");
			writer.write("# Aimed at industrial play: flat plains with optional water / forest access.\n");
			writer.write("# Search uses biome noise only (no chunk loads) for fast world creation.\n");
			writer.write("#\n");
			writer.write("# search_radius          - max search radius in blocks\n");
			writer.write("# search_step            - sampling interval (larger = faster)\n");
			writer.write("# algorithm              - \"locate\" or \"spiral\"\n");
			writer.write("# interior_checks        - neighbor plains samples to avoid biome edges\n");
			writer.write("# max_surface_y          - require estimated surface Y <= this (default 65)\n");
			writer.write("# require_water_nearby   - require river/ocean/beach within nearby_check_radius\n");
			writer.write("# require_forest_nearby  - require forest within nearby_check_radius\n");
			writer.write("# nearby_check_radius    - radius for water/forest checks (blocks)\n");
			writer.write("# flatness_radius        - area around spawn to check flatness (default 24)\n");
			writer.write("# flatness_max_delta     - max surface-Y spread in that area (default 2)\n");
			writer.write("# fallback_to_vanilla    - keep vanilla spawn if no match\n");
			writer.write("\n");
			writer.write("search_radius = " + defaultRadius() + "\n");
			writer.write("search_step = " + defaultStep() + "\n");
			writer.write("algorithm = \"locate\"\n");
			writer.write("interior_checks = 2\n");
			writer.write("max_surface_y = 65\n");
			writer.write("require_water_nearby = true\n");
			writer.write("require_forest_nearby = true\n");
			writer.write("nearby_check_radius = 96\n");
			writer.write("flatness_radius = 24\n");
			writer.write("flatness_max_delta = 2\n");
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
