# Testing and evidence

## Accepted development baseline

The runtime baseline is 0.1.0-alpha.28 on Minecraft 1.21.1 / Java 21 / NeoForge 21.1.252. The owner accepted gameplay, alpha.26 visuals, alpha.28 audio volume and compatibility in the development configuration. A public beta candidate may have different version/licence/branding metadata; its final packaging checks are recorded when assembled.

| Historical check | Recorded result |
| --- | --- |
| Alpha.28 boundary checks | 100,014 passed |
| Alpha.28 transformed-frame checks | 100,002 passed |
| Alpha.28 package inspection | 28,397 passed |
| Alpha.28 standalone packaged server GameTests | All 27 required tests passed |
| Alpha.23 optional packaged suites | 30 Sable, 26 Create and 26 combined-rope tests passed |

Alpha.28 compilation used pinned existing game artifacts. Optional fixtures were not all rerun for alpha.28. Alpha.23 fixture versions were Sable 2.0.6, Create 6.0.10, Farmer's Delight 1.3.4, Supplementaries 4.0.1, Quark 4.1-487, Moonlight 3.7.2 and Zeta 1.1-40. These are historical test versions, not a promise for every later release.

Real dedicated multiplayer gameplay testing is deferred for the initial beta. Headless server automation does not replace real-player testing. Later NeoForge 21.1 versions are permitted by metadata but not claimed as tested.

## Reproduce checks from source

Run from the project root with JDK 21; Windows users replace `./gradlew` with `gradlew.bat`:

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runJarGameTestServer
```

`build` includes boundary/frame checks. `runGameTestServer` uses development classes; `runJarGameTestServer` loads the packaged mod. Check the Minecraft log for all required tests passing and confirm the packaged run actually loaded the runtime JAR. A Gradle exit code alone is insufficient evidence.

## Optional fixture runs

```sh
./gradlew runSableJarGameTestServer
./gradlew runCreateJarGameTestServer -PcreateFixtureDirectory=/absolute/path/to/create-fixtures
./gradlew runRopeCompatJarGameTestServer -PropeFixtureDirectory=/absolute/path/to/rope-fixtures
```

The Sable task downloads the configured released fixture. Supply compatible NeoForge 1.21.1 Create and its required dependencies for the Create task; supply Farmer's Delight, Supplementaries, Moonlight, Quark and Zeta for the combined-rope task. Fixture JARs are external inputs copied only into isolated `build` runs, not release deliverables.

Inspect the final runtime JAR with `scripts/inspect_package.py`; its sources companion must be beside it. See [build instructions](../BUILDING.md). Record the actual candidate version/hash and which checks were run. Keep manual client appearance/audio checks distinct from automation and older accepted evidence.
