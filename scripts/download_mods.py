import json, pathlib, urllib.request
base="https://api.modrinth.com/v2/project/"
def grab(project, match, dest):
    req=urllib.request.Request(base+project+"/version",headers={"User-Agent":"LittleSporeMaterialFix/0.1"})
    with urllib.request.urlopen(req,timeout=40) as r: versions=json.load(r)
    valid=[v for v in versions if "1.20.1" in v.get("game_versions",[]) and "forge" in v.get("loaders",[])]
    if match: valid=[v for v in valid if match in v.get("version_number","") or match in v.get("name","")]
    if not valid: raise RuntimeError(f"No matching Forge version: {project} {match}")
    version=valid[0]
    jar=next((f for f in version["files"] if f["filename"].endswith(".jar") and f.get("primary")),None)
    if jar is None: jar=next(f for f in version["files"] if f["filename"].endswith(".jar"))
    print(project,version["version_number"],jar["filename"])
    urllib.request.urlretrieve(jar["url"],dest)
grab("littletiles","pre161","libs/LittleTiles.jar")
grab("creativecore","2.12.","libs/CreativeCore.jar")
