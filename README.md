# Always Plains Spawn

A Minecraft mod (Fabric / Forge / NeoForge) that relocates the **world spawn** into a **Plains** biome so new players and bed-less respawns always start in plains.

Maintained by MisoPy.

## How It Works

After the overworld is created/loaded (once per world), the mod searches for `minecraft:plains`, then calls vanilla `setDefaultSpawnPos`. That moves the world spawn and, on **Minecraft &lt; 1.21.9**, also moves the **spawn chunks** with it. From **1.21.9+**, Mojang removed spawn chunks; only the spawn point is updated.

Bed / respawn-anchor spawns are unchanged. Without those, players use the plains world spawn.

A per-world marker (`aps_relocated` in the world folder) prevents re-running after success, so later `/setworldspawn` is not overwritten.

## Config

Path: `config/aps-common.toml` (created on first launch)

| Key | Meaning |
|---|---|
| `search_radius` | Max search radius in blocks |
| `search_step` | Sampling interval in blocks |
| `algorithm` | `locate` (BiomeSource) or `spiral` (grid spiral) |
| `fallback_to_vanilla` | Keep vanilla spawn if plains cannot be found |

Biome selection is fixed to Plains; only search parameters are configurable.

## Compatibility

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
| Forge | 1.17.1 … 1.19.4 (**Forge 1.20.1 excluded**) |
| NeoForge | 1.20.4 … 26.1.2 |

Notes:

- Forge 1.16.5 is not supported by the current LegacyForge toolchain
- NeoForge covers 1.20.2+ (via 1.20.4+ targets + additional versions)

## License

MIT — see [LICENSE](LICENSE).
