from pathlib import Path
import json
import urllib.request

def token():
	for line in Path(".env").read_text(encoding="utf-8").splitlines():
		if line.strip().startswith("CURSEFORGE_TOKEN="):
			v = line.split("=", 1)[1].strip().strip('"').strip("'")
			return v
	raise SystemExit("no token")

t = token()
headers = {"X-Api-Token": t, "User-Agent": "aps"}
with urllib.request.urlopen(urllib.request.Request("https://minecraft.curseforge.com/api/game/version-types", headers=headers), timeout=60) as r:
	types = {vt["id"]: vt for vt in json.load(r)}
with urllib.request.urlopen(urllib.request.Request("https://minecraft.curseforge.com/api/game/versions", headers=headers), timeout=60) as r:
	data = json.load(r)

names = [
	"1.16.5","1.17","1.17.1","1.18","1.18.1","1.18.2","1.19","1.19.1","1.19.2","1.19.3","1.19.4",
	"1.20","1.20.1","1.20.2","1.20.3","1.20.4","1.20.5","1.20.6",
	"1.21","1.21.1","1.21.2","1.21.3","1.21.4","1.21.5","1.21.6","1.21.7","1.21.8","1.21.9","1.21.10","1.21.11",
	"26.1","26.1.1","26.1.2",
]
for name in names:
	cands = [v for v in data if v.get("name") == name]
	print(name)
	for v in cands:
		tid = v["gameVersionTypeID"]
		tinfo = types.get(tid, {})
		print(f"  id={v['id']} typeID={tid} typeName={tinfo.get('name')} typeSlug={tinfo.get('slug')}")
