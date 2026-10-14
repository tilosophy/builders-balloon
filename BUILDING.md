# Building Builder's Balloon

## Toolchain

Minecraft 1.21.1, Java/JDK 21 and NeoForge 21.1.252. Gradle 9.2.1 and ModDevGradle 2.0.146 are pinned; use the included wrapper. Sable common 2.0.6 and Companion common 1.6.0 are compile-only dependencies. Internet access is needed for the initial dependency/game-artifact setup. Optional mod implementations are not bundled.

From the project root:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The runtime and sources JARs are written to `build/libs`. Install only the runtime JAR. Both distributions include project LICENSE and NOTICE in META-INF. Version/licence settings live in `gradle.properties`; a release version is selected during candidate preparation.

## Development runs

Use `./gradlew runClient` or `./gradlew runServer` (on Windows replace `./gradlew` with `gradlew.bat`). Review Minecraft's EULA before operating a server; this project does not accept it automatically. Generated worlds, dependency caches and fixtures are outside the source distribution.

See [testing](docs/TESTING.md) for packaged GameTests and optional fixtures, and the [player guide](docs/PLAYER-GUIDE.md) for server configuration.

## Authored resource generators

Existing resources are included, so generation is unnecessary for an ordinary build. To intentionally regenerate authored resources, use Python 3 and run, in order:

```sh
python3 scripts/generate_anchor.py
python3 scripts/generate_balloon.py
python3 scripts/generate_bollard.py
python3 scripts/generate_metal.py
```

These selected generators use Python's standard library. Inspect resulting changes before committing; texture/geometry changes require in-game visual review. Runtime Minecraft texture references are resolved by the game. Private sound-editing masters and third-party fixture JARs are not needed for normal source builds and are not included.

## Package inspection

With Python 3, inspect a runtime JAR and its same-basename `-sources.jar` companion in the same directory:

```sh
python3 scripts/inspect_package.py build/libs/<runtime-jar-name>.jar
```

Use a concrete filename in place of the placeholder. This checks packaged metadata, resources, audio hashes and source coverage. For MIT candidates it also verifies the embedded LICENSE. Public single-JAR inspection does not require private development archives.
