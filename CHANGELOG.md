## Visuality: Reforged 2.2.0

### Support policy

* This is a one-time compatibility release for Minecraft 1.20.1 Forge. The branch remains end-of-life and is not part of active maintenance.

### Fixed

* Keep malformed particle-emitter JSON files unchanged, log a clear error, and retain the last valid runtime configuration instead of silently replacing files with defaults (#25).
* Reduce the default soul-sand and soul-soil ambient particle rate and add an optional per-entry `interval` setting (#13).
* Make armor sparkles independent of humanoid renderer models, so they work with model-replacing resource packs and renderer mods; select directly from configured armor that is actually worn (#23, #36).
* Add `#namespace:tag` block selectors and use Forge common ore tags so compatible modded ores, including GTCEu ores, can sparkle (#24).
* Apply resource-pack emitter JSON without a `conditions` object instead of skipping it.

### Build and publishing

* Target Minecraft 1.20.1 with Forge 47.4.22.
* Add Mod Publish Plugin 2.1.1 with CurseForge and Modrinth client-only metadata and an opt-in `-PpublishDryRun=true` validation mode.
* Upgrade the Gradle wrapper to 8.14.3 and ForgeGradle to 6.0.54.
