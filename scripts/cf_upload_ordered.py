from pathlib import Path
import json
import urllib.request
import urllib.error
import time
import uuid

PROJECT_ID = 1633549
MOD_ID = "alwaysplainsspawn"
MOD_VERSION = "1.0.0"
DIST = Path("build/dist")
ENDPOINT = "https://minecraft.curseforge.com"

# Fabric -> Forge -> NeoForge; oldest MC first within each loader.
TARGETS = [
	("fabric", "1.16.5", []),
	("fabric", "1.17.1", ["1.17"]),
	("fabric", "1.18.2", ["1.18", "1.18.1"]),
	("fabric", "1.19.2", ["1.19", "1.19.1"]),
	("fabric", "1.19.4", ["1.19.3"]),
	("fabric", "1.20.1", ["1.20"]),
	("fabric", "1.20.4", ["1.20.2", "1.20.3"]),
	("fabric", "1.20.6", ["1.20.5"]),
	("fabric", "1.21.1", ["1.21"]),
	("fabric", "1.21.3", ["1.21.2"]),
	("fabric", "1.21.4", []),
	("fabric", "1.21.5", []),
	("fabric", "1.21.8", ["1.21.6", "1.21.7"]),
	("fabric", "1.21.11", ["1.21.9", "1.21.10"]),
	("fabric", "26.1.2", ["26.1.1", "26.1"]),
	("forge", "1.17.1", ["1.17"]),
	("forge", "1.18.2", ["1.18", "1.18.1"]),
	("forge", "1.19.2", ["1.19", "1.19.1"]),
	("forge", "1.19.4", ["1.19.3"]),
	("forge", "1.20.1", ["1.20"]),
	("neoforge", "1.20.4", ["1.20.2", "1.20.3"]),
	("neoforge", "1.20.6", ["1.20.5"]),
	("neoforge", "1.21.1", ["1.21"]),
	("neoforge", "1.21.3", ["1.21.2"]),
	("neoforge", "1.21.4", []),
	("neoforge", "1.21.5", []),
	("neoforge", "1.21.8", ["1.21.6", "1.21.7"]),
	("neoforge", "1.21.11", ["1.21.9", "1.21.10"]),
	("neoforge", "26.1.2", ["26.1.1", "26.1"]),
]

LOADER_NAME = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def read_token() -> str:
	for line in Path(".env").read_text(encoding="utf-8").splitlines():
		line = line.strip()
		if not line.startswith("CURSEFORGE_TOKEN="):
			continue
		value = line.split("=", 1)[1].strip()
		if (value.startswith('"') and value.endswith('"')) or (value.startswith("'") and value.endswith("'")):
			value = value[1:-1]
		return value
	raise SystemExit("CURSEFORGE_TOKEN missing")


def api_get(token: str, path: str):
	req = urllib.request.Request(
		ENDPOINT + path,
		headers={"X-Api-Token": token, "User-Agent": "AlwaysPlainsSpawn-uploader"},
	)
	with urllib.request.urlopen(req, timeout=120) as resp:
		return json.load(resp)


def build_version_index(versions: list) -> dict[str, list[dict]]:
	idx: dict[str, list[dict]] = {}
	for v in versions:
		idx.setdefault(v.get("name", ""), []).append(v)
	return idx


def pick_version_ids(idx: dict[str, list[dict]], types: dict[int, dict], names: list[str]) -> list[int]:
	ids: list[int] = []
	for name in names:
		cands = idx.get(name) or []
		if not cands:
			raise SystemExit(f"CurseForge game version not found: {name}")

		def rank(v: dict) -> tuple:
			tid = int(v.get("gameVersionTypeID") or 0)
			tinfo = types.get(tid) or {}
			slug = str(tinfo.get("slug") or "")
			tname = str(tinfo.get("name") or "")
			if slug.startswith("minecraft-") or tname.startswith("Minecraft"):
				prio = 0
			elif slug == "modloader" or tname == "Modloader":
				prio = 0
			elif slug == "environment" or tname == "Environment":
				prio = 0
			elif slug == "addons" or tid in (1, 615):
				prio = 2
			else:
				prio = 1
			return (prio, -int(v.get("id") or 0))

		chosen = sorted(cands, key=rank)[0]
		ids.append(int(chosen["id"]))
	return ids


def multipart_upload(token: str, project_id: int, jar: Path, metadata: dict) -> dict:
	boundary = "----APS" + uuid.uuid4().hex
	meta_json = json.dumps(metadata, ensure_ascii=False).encode("utf-8")
	file_bytes = jar.read_bytes()

	body = b""
	body += f"--{boundary}\r\n".encode()
	body += b'Content-Disposition: form-data; name="metadata"\r\n'
	body += b"Content-Type: application/json; charset=utf-8\r\n\r\n"
	body += meta_json + b"\r\n"
	body += f"--{boundary}\r\n".encode()
	body += f'Content-Disposition: form-data; name="file"; filename="{jar.name}"\r\n'.encode()
	body += b"Content-Type: application/java-archive\r\n\r\n"
	body += file_bytes + b"\r\n"
	body += f"--{boundary}--\r\n".encode()

	req = urllib.request.Request(
		f"{ENDPOINT}/api/projects/{project_id}/upload-file",
		data=body,
		method="POST",
		headers={
			"X-Api-Token": token,
			"User-Agent": "AlwaysPlainsSpawn-uploader",
			"Content-Type": f"multipart/form-data; boundary={boundary}",
			"Content-Length": str(len(body)),
		},
	)
	try:
		with urllib.request.urlopen(req, timeout=600) as resp:
			return json.load(resp)
	except urllib.error.HTTPError as e:
		detail = e.read().decode("utf-8", "replace")
		raise RuntimeError(f"HTTP {e.code}: {detail}") from e


def main() -> None:
	token = read_token()
	changelog = Path("CHANGELOG.md").read_text(encoding="utf-8") if Path("CHANGELOG.md").exists() else " "
	if not changelog.strip():
		changelog = " "

	print("Fetching CurseForge game versions...")
	versions = api_get(token, "/api/game/versions")
	type_list = api_get(token, "/api/game/version-types")
	types = {int(vt["id"]): vt for vt in type_list}
	idx = build_version_index(versions)
	print(f"Loaded {len(versions)} version entries")

	missing = []
	for loader, mc, _ in TARGETS:
		jar = DIST / f"{MOD_ID}-{MOD_VERSION}-{loader}+{mc}.jar"
		if not jar.is_file():
			missing.append(str(jar))
	if missing:
		raise SystemExit("Missing jars:\n" + "\n".join(missing))

	print(f"Uploading {len(TARGETS)} files to project {PROJECT_ID}")
	print("Order: Fabric -> Forge -> NeoForge; oldest MC first within each loader\n")

	for i, (loader, mc, additional) in enumerate(TARGETS, 1):
		jar = DIST / f"{MOD_ID}-{MOD_VERSION}-{loader}+{mc}.jar"
		game_names = [mc, *additional, LOADER_NAME[loader], "Server"]
		game_version_ids = pick_version_ids(idx, types, game_names)

		if loader == "fabric":
			relations = [
				{"slug": "fabric-api", "type": "requiredDependency"},
				{"slug": "architectury-api", "type": "requiredDependency"},
			]
		else:
			relations = [{"slug": "architectury-api", "type": "requiredDependency"}]

		metadata = {
			"changelog": changelog,
			"changelogType": "markdown",
			"displayName": jar.name,
			"gameVersions": game_version_ids,
			"releaseType": "release",
			"relations": {"projects": relations},
		}

		print(f"[{i}/{len(TARGETS)}] {jar.name}")
		print(f"  versions: {game_names}")
		print(f"  ids: {game_version_ids}")
		result = multipart_upload(token, PROJECT_ID, jar, metadata)
		print(f"  OK -> {result}")
		time.sleep(1.5)

	print("\nAll uploads finished.")


if __name__ == "__main__":
	main()
