# Media provenance and limits

[Deutsch](Bildnachweise) · **English**

![Actual Flux connections for the charging network, including the virtual battery Plug.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-flux-connections.png)

This Wiki uses real, unmodified Minecraft captures. Different image types establish different things; a configuration screenshot alone does not prove a long-running gameplay mechanic.

## New gameplay captures, 13 November 2026

Capture client: published addon **0.1.3-beta.2**, Minecraft 1.21.1, NeoForge 21.1.231, Java 21, Mekanism 10.7.19.85, KFF 5.9.0, original Flux Networks 8.0.0 and JEI 19.39.0.372. Window 1280 × 800, GUI scale 2, German game language.

A temporary separate capture mod populated a **new flat test world** with networks, machines, inventories and initial energy, then opened native interfaces. The fixture is not part of the product or release JAR. No personal worlds were opened.

- Overview: actual machine and Flux test setup.
- Crafting table: actual recipe evaluation with the battery in its result slot.
- Settings/network list: real server-side battery data and private networks owned by the capture player.
- Smelter: actual raw iron processing produced iron ingots, checked by the capture run.
- Energy cube: full battery in the correct item charging slot; the run checked positive energy received by Flux storage.
- Flux connections: actual registered virtual Plug. Full storage may mean zero throughput at capture time.
- Server configuration: actual NeoForge configuration page for the test world, with chunk loading deliberately allowed.

## Existing UI check images

`battery-switches.png` and `battery-validation.png` come from the release client checks and use synthetic networks and initial values. The checks activate real widgets and verify draft retention without a complete gameplay connection. These images illustrate options and validation respectively.

## What these images do not establish

No two-player permission acceptance, actual chunk-unload/return, multi-hour modpack test or autonomous power plant. Automated FakePlayer/server checks and documentation captures are not presented as completed manual acceptance. The release remains a beta.

The [media inventory](https://github.com/TeutonStudio/FluxBatterie/blob/master/docs/wiki-media-coverage.json) records mapping, origin and limits.
