"""Update existing CurseForge files to Server-only environment tags."""
from pathlib import Path
import json
import time
import uuid
import urllib.request
import urllib.error

PROJECT_ID = 1633549
ENDPOINT = "https://minecraft.curseforge.com"

# fileId -> (loader, mc, additionalVersions) from ordered upload log
FILES = [
	(8552520, "fabric", "1.16.5", []),
	(8552522, "fabric", "1.17.1", ["1.17"]),
	(8552523, "fabric", "1.18.2", ["1.18", "1.18.1"]),
	(8552524, "fabric", "1.19.2", ["1.19", "1.19.1"]),
	(8552525, "fabric", "1.19.4", ["1.19.3"]),
	(8552526, "fabric", "1.20.1", ["1.20"]),
	(8552527, "fabric", "1.20.4", ["1.20.2", "1.20.3"]),
	(8552528, "fabric", "1.20.6", ["1.20.5"]),
	(8552529, "fabric", "1.21.1", ["1.21"]),
	(8552530, "fabric", "1.21.3", ["1.21.2"]),
	(8552531, "fabric", "1.21.4", []),
	(8552533, "fabric", "1.21.5", []),
	(8552534, "fabric", "1.21.8", ["1.21.6", "1.21.7"]),
	(8552535, "fabric", "1.21.11", ["1.21.9", "1.21.10"]),
	(8552536, "fabric", "26.1.2", ["26.1.1", "26.1"]),
	(8552537, "forge", "1.17.1", ["1.17"]),
	(8552539, "forge", "1.18.2", ["1.18", "1.18.1"]),
	(8552540, "forge", "1.19.2", ["1.19", "1.19.1"]),
	(8552541, "forge", "1.19.4", ["1.19.3"]),
	(8552542, "forge", "1.20.1", ["1.20"]),
	(8552543, "neoforge", "1.20.4", ["1.20.2", "1.20.3"]),
	(8552544, "neoforge", "1.20.6", ["1.20.5"]),
	(8552545, "neoforge", "1.21.1", ["1.21"]),
	(8552547, "neoforge", "1.21.3", ["1.21.2"]),
	(8552548, "neoforge", "1.21.4", []),
	(8552550, "neoforge", "1.21.5", []),
	(8552551, "neoforge", "1.21.8", ["1.21.6", "1.21.7"]),
	(8552554, "neoforge", "1.21.11", ["1.21.9", "1.21.10"]),
	(8552555, "neoforge", "26.1.2", ["26.1.1", "26.1"]),
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


def update_file(token: str, project_id: int, metadata: dict) -> dict:
	boundary = "----APS" + uuid.uuid4().hex
	meta_json = json.dumps(metadata, ensure_ascii=False).encode("utf-8")
	body = b""
	body += f"--{boundary}\r\n".encode()
	body += b'Content-Disposition: form-data; name="metadata"\r\n'
	body += b"Content-Type: application/json; charset=utf-8\r\n\r\n"
	body += meta_json + b"\r\n"
	body += f"--{boundary}--\r\n".encode()

	req = urllib.request.Request(
		f"{ENDPOINT}/api/projects/{project_id}/update-file",
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
		with urllib.request.urlopen(req, timeout=120) as resp:
			raw = resp.read().decode("utf-8", "replace")
			return json.loads(raw) if raw.strip() else {"ok": True}
	except urllib.error.HTTPError as e:
		detail = e.read().decode("utf-8", "replace")
		raise RuntimeError(f"HTTP {e.code}: {detail}") from e


def main() -> None:
	token = read_token()
	versions = api_get(token, "/api/game/versions")
	type_list = api_get(token, "/api/game/version-types")
	types = {int(vt["id"]): vt for vt in type_list}
	idx = build_version_index(versions)

	print(f"Updating {len(FILES)} files to Server-only environment\n")
	for i, (file_id, loader, mc, additional) in enumerate(FILES, 1):
		names = [mc, *additional, LOADER_NAME[loader], "Server"]
		ids = pick_version_ids(idx, types, names)
		metadata = {"fileID": file_id, "gameVersions": ids}
		print(f"[{i}/{len(FILES)}] file {file_id} -> {names}")
		result = update_file(token, PROJECT_ID, metadata)
		print(f"  OK -> {result}")
		time.sleep(0.8)
	print("\nAll environment tags updated.")


if __name__ == "__main__":
	main()
