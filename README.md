# Always Plains Spawn

A Minecraft mod (Fabric / Forge / NeoForge) that relocates the **world spawn** into a **Plains** biome suited for industrial play — flat land, optional nearby water and forest.

Maintained by MisoPy.

## Design assumptions

**APS is built around vanilla overworld terrain generation.**  
Default config values assume vanilla biome layout and height shapes, and are tuned for **industrial / factory-style starts**:

- Flat plains (surface height and local flatness filters)
- Water (river / ocean / beach) within a short travel distance
- Forest nearby for early wood

With those defaults, a new world should feel like a practical industrial spawn rather than a biome-gacha roll.

### Terrain-generation mods

Mods that **heavily change terrain or biomes** (custom worldgen, large biome overhauls, exotic heightmaps, etc.) often clash with these assumptions. Spawn search may become slow, fail, or pick awkward sites.

`config/aps-common.toml` can soften some mismatches (relax flatness, drop water/forest requirements, raise `max_surface_y`, widen search). That only helps when the world still has recognizable plains-like areas. **Forced use alongside deep worldgen overhauls is not recommended.**

## How it works

After the overworld is created/loaded (once per world), the mod searches for `minecraft:plains` using **biome noise only** (no chunk generation during search), then sets the world spawn.

Focus points:

- **Noise-only search** — avoids generating chunks while hunting for plains (fast world create / first join)
- **Interior plains samples** — reduces landing on thin plains strips or forest edges
- **Industrial site filters** — estimated surface Y, sparse flatness ring (rejects steep river cuts), optional nearby water & forest
- **Spawn chunks left to vanilla** — only the spawn coordinate is set; no custom forceload (compatible with removers such as Ksyxis)

Performance-minded details (quality kept close to the strict checks):

- Flatness uses a **sparse** sample (cardinals / diagonals at radius & half-radius), not a dense height grid
- Biome tags use **sea-level noise Y** instead of calling `getBaseHeight` on every query
- Water + forest share **one** nearby spiral (one noise sample per column)
- Winning candidate is not fully re-validated when placing the final Y

Bed / respawn-anchor spawns are unchanged. Without those, players use the plains world spawn.

A per-world marker (`aps_relocated`) prevents re-running after success.

## Config

Path: `config/aps-common.toml` (created on first launch)

| Key | Meaning |
|---|---|
| `search_radius` | Max search radius in blocks (default **4092**) |
| `search_step` | Noise sampling interval (default 64; larger = faster) |
| `algorithm` | `locate` or `spiral` (both noise-only) |
| `interior_checks` | Neighbor plains samples to avoid biome edges |
| `max_surface_y` | Require estimated surface Y ≤ this (default **65**) |
| `require_water_nearby` | Require river/ocean/beach nearby (default **true**) |
| `require_forest_nearby` | Require forest nearby (default **true**) |
| `nearby_check_radius` | Radius for water/forest checks (default 96) |
| `flatness_radius` | Area around spawn for flatness check (default **24**) |
| `flatness_max_delta` | Max surface-Y spread in that area (default **2**) |
| `fallback_to_vanilla` | Keep vanilla spawn if no match |

Biome is fixed to Plains; site filters above are configurable. Defaults target vanilla terrain + industrial convenience.

## Compatibility

- Intended for **vanilla-like** overworld generation (see above)
- Server-side logic (client optional for singleplayer)
- Mixins on vanilla server load / respawn paths
- Nearby patch versions share one jar (`publish.additionalVersions`)

## Requirements

- Minecraft **1.16.5 ~ 26.1.2** (Fabric / Forge / NeoForge — see matrix)
- **Architectury API** (all loaders)
- **Fabric API** on Fabric only

### Version matrix (compile targets)

| Loader | Primary versions |
|---|---|
| Fabric | 1.16.5 … 26.1.2 |
| Forge | 1.17.1 … 1.20.1 |
| NeoForge | 1.20.4 … 26.1.2 |

Notes:

- Forge 1.16.5 is not supported by the current LegacyForge toolchain
- NeoForge covers 1.20.2+ (via 1.20.4+ targets + additional versions)

## License

MIT — see [LICENSE](LICENSE).
