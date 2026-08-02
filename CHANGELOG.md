## Visuality: Reforged 3.0.0

### Support policy

* Active maintenance is now limited to Minecraft 1.21.1 and 1.21.8 on NeoForge. All other branches are end-of-life.

### Added

* Add a configurable RGB color for slime blob particles under `slime.color` in the client config.
* Add `#namespace:tag` item selectors to entity armor particle-emitter entries while keeping existing item ID entries compatible.
* Add distinct cold and warm feather hit particles for the matching chicken variants on Minecraft 1.21.8.
* Add German, Argentinian Spanish, European Spanish, French, and Polish translations, and translate the new slime color option.

### Changed

* Blend rain water-circle colors using Minecraft's client biome color blending instead of using a single raw biome color.
* Update the emerald particle texture from Fabric Visuality and refresh the normal chicken feather texture on Minecraft 1.21.8.

### Fixed

* Prevent non-living client entities such as Ender Dragon parts and interaction entities from entering the living-entity hit-particle path (#32, #34).
* Keep malformed particle-emitter JSON files unchanged, log a clear error, and retain the last valid runtime configuration instead of silently replacing files with defaults (#25).
* Reduce the default soul-sand and soul-soil ambient particle rate and add an optional per-entry `interval` setting (#13).
* Spawn armor sparkles during living-entity ticks instead of only attempting them when the wearer takes damage (#23, #36).
* Add `#namespace:tag` block selectors and use NeoForge common ore tags so compatible modded ores, including GTCEu ores, can sparkle (#24).
* Apply resource-pack emitter JSON without a `conditions` object instead of skipping it.

### Build and publishing

* Upgrade Mod Publish Plugin to 2.1.1, add an opt-in `-PpublishDryRun=true` validation mode, and declare the project as client-only for CurseForge and Modrinth.
* Upgrade the Gradle wrapper to 9.2.1 and ModDevGradle to 2.0.143.

