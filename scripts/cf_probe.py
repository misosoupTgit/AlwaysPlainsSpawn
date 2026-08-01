from pathlib import Path
import json
import urllib.request
import urllib.error

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

token = read_token()
print("token_len", len(token))

for url in (
	"https://minecraft.curseforge.com/api/game/versions",
	"https://legacy.curseforge.com/api/game/versions",
):
	req = urllib.request.Request(url, headers={"X-Api-Token": token, "User-Agent": "AlwaysPlainsSpawn-uploader"})
	try:
		with urllib.request.urlopen(req, timeout=60) as resp:
			data = json.load(resp)
		print(url, "OK", "count", len(data) if isinstance(data, list) else type(data))
		if isinstance(data, list):
			wanted = {"1.16.5", "1.20.1", "26.1.2", "Fabric", "Forge", "NeoForge", "Server"}
			for v in data:
				if v.get("name") in wanted:
					print(" ", v.get("id"), v.get("name"), v.get("type"), v.get("slug"))
		break
	except Exception as e:
		print(url, "FAIL", e)
		if isinstance(e, urllib.error.HTTPError):
			print(" ", e.read()[:300])
