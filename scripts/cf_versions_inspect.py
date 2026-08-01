from pathlib import Path
import json
import urllib.request

def token():
	for line in Path(".env").read_text(encoding="utf-8").splitlines():
		if line.strip().startswith("CURSEFORGE_TOKEN="):
			v = line.split("=", 1)[1].strip()
			if (v.startswith('"') and v.endswith('"')) or (v.startswith("'") and v.endswith("'")):
				v = v[1:-1]
			return v
	raise SystemExit("no token")

t = token()
req = urllib.request.Request(
	"https://minecraft.curseforge.com/api/game/versions",
	headers={"X-Api-Token": t, "User-Agent": "aps"},
)
with urllib.request.urlopen(req, timeout=60) as r:
	data = json.load(r)

print("keys", sorted(data[0].keys()))
for name in ["1.20.1", "1.16.5", "Fabric", "Forge", "NeoForge", "1.21", "26.1", "Client", "Server"]:
	c = [v for v in data if v.get("name") == name]
	print("====", name, "count", len(c))
	for v in c:
		print(v)

req2 = urllib.request.Request(
	"https://minecraft.curseforge.com/api/game/version-types",
	headers={"X-Api-Token": t, "User-Agent": "aps"},
)
with urllib.request.urlopen(req2, timeout=60) as r:
	types = json.load(r)
print("==== types ====")
for vt in types:
	if vt.get("name") in ("Minecraft", "Modloader", "Environment", "Java", "Addon"):
		print(vt)
	if "neo" in str(vt).lower() or "forge" in str(vt).lower() or "fabric" in str(vt).lower() or vt.get("slug") in ("minecraft", "modloader", "environment"):
		print(vt)
