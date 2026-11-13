# Installation

[Deutsch](Installation) · **English**

![Real test world containing a smelter, energy cube and Flux devices.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-overview.png)

Install the following on the **server and every client**. This Wiki documents the published **0.1.3-beta.2 prerelease**, dated 13 November 2026.

| Component | Supported version |
|---|---|
| Minecraft / Java | 1.21.1 / Java 21 |
| NeoForge | 21.1.231 or later within 21.1 |
| Flux Battery | 0.1.3-beta.2 |
| Flux Networks | original 1.21.1-8.0.0, CurseForge file 6089446 |
| Mekanism | 1.21.1-10.7.19.85; internal mod version 10.7.19 |
| Kotlin for Forge | 5.9.0 for NeoForge; metadata range 5.9 to below 6 |

## Set up

1. Download the addon JAR from the [release](https://github.com/TeutonStudio/FluxBatterie/releases/tag/v0.1.3-beta.2).
2. Put the dependencies and `fluxbattery-0.1.3-beta.2.jar` in `mods`.
3. Remove historical replacement/bridge Flux JARs. Keep the original Flux Networks JAR.
4. Start a test world and follow [Getting started](Getting-Started).

JEI is optional. The capture client uses it, but the addon does not require it. Battery operation needs no Flux Controller. This mod does not require TeutonLib, Create or Sable.

## Upgrade and verify

Update client and server together: the configuration protocol is version 2. Saved battery and world data remain compatible. Back up the world before upgrading.

Use the accompanying `.sha256` file to verify the addon download. Only unmodified Flux Networks 8.0.0 is accepted, with SHA-256 `29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6`. A version, hash or mandatory patch conflict aborts startup with a specific message; see [Troubleshooting](Troubleshooting).

The image shows the real isolated capture client and its test setup. A temporary capture fixture placed the scene. It does not establish compatibility with arbitrary modpacks.
