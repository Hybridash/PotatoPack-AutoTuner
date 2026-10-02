# PotatoPack AutoTuner

A Fabric mod for Minecraft 1.21.1. **The first time you launch the game, it checks your PC and picks video settings that fit it**, so new players never have to dig through Video Settings.

It's made for [PotatoPack](https://github.com/Hybridash/PotatoPack) but works in any Fabric 1.21.1 instance.

## What it checks

| Part | How |
|---|---|
| GPU | The graphics card name your driver reports (Intel HD, GTX 1050, RTX 3060, Apple M1, etc.) |
| CPU | How many threads your processor has |
| RAM | How much memory the PC has, and how much the launcher gave Minecraft |

Each one gets a score, and **the weakest part decides the profile**, because that's what holds your FPS back.

## The profiles

| Profile | Render distance | Graphics | Clouds | Particles | Picked for |
|---|---|---|---|---|---|
| Potato | 4 | Fast | Off | Minimal | No real GPU driver, 2-core CPUs, 3 GB RAM or less |
| Low | 6 | Fast | Off | Decreased | Old Intel/AMD integrated graphics, 4 GB RAM, or under 1.6 GB given to Minecraft |
| Medium | 10 | Fancy | Fast | All | Iris Xe, GTX 1050–1660, Apple M1/M2, 8 GB RAM |
| High | 16 | Fancy | Fancy | All | RTX cards, RX 5000+ cards, 16 GB+ RAM |

It also sets simulation distance, smooth lighting, biome blend and entity distance to match.

## It only runs once

After the first launch it **never touches your settings again**. If you change render distance yourself, it stays changed. The settings it replaced are backed up, so you can always undo.

## Commands

| Command | What it does |
|---|---|
| `/autotune` | Shows what it detected and what it recommends |
| `/autotune auto` | Applies the recommended profile again |
| `/autotune potato` / `low` / `medium` / `high` | Applies that profile |
| `/autotune undo` | Puts back the settings you had before AutoTuner changed them |

To turn off the first-launch tuning completely, set `"enabled": false` in `config/autotuner.json`.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Download `autotuner-x.x.x.jar` from [**Releases**](../../releases) and put it in `mods`.

It's client-side only. Servers don't need it.

## Building

```
./gradlew build
```

The jar ends up in `build/libs/`.

To ship an update, bump `mod_version` in `gradle.properties` and push. GitHub Actions builds it and publishes a release automatically.
