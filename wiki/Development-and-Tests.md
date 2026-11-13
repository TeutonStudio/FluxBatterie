# Development and tests

[Deutsch](Entwicklung-und-Pruefungen) · **English**

![Actual smelting operation supplements the automated evidence.](https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/assets/screenshots/wiki-powering-machine.png)

Published baseline: **0.1.3-beta.2**, commit `1a9e690`. The implementation uses Kotlin with Kotlin for Forge. The load-time patch extends only the required original Flux classes in memory; no replacement Flux JAR is distributed.

## Build locally

From the project directory with Java 21:

```sh
python3 scripts/bootstrap-dependencies.py
./gradlew build
./gradlew runGameTestServer -PpackagedAddon
python3 scripts/test-restart.py
python3 scripts/test-server.py
python3 scripts/test-loader.py
python3 scripts/test-client.py
python3 scripts/verify-distribution.py
```

The wrapper uses Gradle 9.2.1, ModDevGradle 2.0.141 and Kotlin 2.1.21. The addon is generated in `build/libs/`. `-PpackagedAddon` runs the built JAR; development/test fixture classes do not replace the required Flux JAR. In IntelliJ, use the checked-in **Flux Battery Client** Gradle configuration or `runClient`.

## Release checks actually executed

| Check | Result |
|---|---|
| JUnit | 3 passed |
| Dedicated Server | 24/24 passed |
| GameTests with compatible synthetic Mixin/coremod fixtures | 24/24 passed |
| Restart in two separate JVMs | 26/26 passed in each |
| Wrong version, modified original, required patch conflict | Expected startup rejection |
| Client | German/English, search, pagination, switches, validation and real resizing to 320 × 240 GUI pixels |
| Distribution | One addon, correct metadata/licences and valid JSON |

The real gameplay capture above adds an operating smelter which produced ingots. A capture fixture arranged the scene; it was not built manually in survival. The charging example additionally checked energy actually stored in the Flux storage.

[Evidence and hashes](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/evidence/release-0.1.3-beta.2.txt) · [Architecture](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/architecture.md) · [Outstanding gameplay acceptance](https://github.com/TeutonStudio/FluxBatterie/blob/v0.1.3-beta.2/docs/release-acceptance.md)

Documentation captures change neither the mod version nor the release JAR. See [Media provenance](Media-Provenance) for capture details.
