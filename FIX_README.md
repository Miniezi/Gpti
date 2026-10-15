# LittleTiles × Spore Material Fix — EXPERIMENTAL

Minecraft 1.20.1 Forge / LittleTiles 1.6.0-pre161 / Spore 2.2.0j / MINI Additions 1.4.0.

This proof-of-concept Mixin overrides LittleTiles block destroy speed using the weakest breakable constituent material; all-bedrock blocks return -1. It does not yet intercept mob code paths that ignore destroy speed. **Build success does not prove the fix works in Minecraft.** Back up the world before testing.

Go to **Actions → Build Forge mod → latest run → Artifacts** for the compiled jar *if the workflow passes*. If it fails, read the Actions build log and do not install a jar. Never replace the original LittleTiles mod.

This repository branch is isolated from main. No Spore or LittleTiles binary files are redistributed.
