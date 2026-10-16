# HorrorMod

A horror-themed Minecraft mod ("Give me back my skin, Maryl").

**Read [design.md](design.md) before building anything.** It has the story, tone, and the spec for every item, block, mob and villager, plus current priorities and open questions.

## Tech stack (pinned — do not mix APIs from other versions)

| | |
|---|---|
| Minecraft | **1.21** (exactly 1.21, not 1.21.1+ or 1.20.x) |
| Mod loader | **Minecraft Forge 51.0.33** (`net.minecraftforge.*`). **Not NeoForge, not Fabric.** |
| Java | **21** (Temurin at `/usr/lib/jvm/temurin-21-jdk`; the system Java 25 will not work) |
| Mappings | **Parchment 2024.07.28-1.21** on top of Mojang official names |
| Build | ForgeGradle 6, Gradle 8.7 (wrapper) |
| Animation | **GeckoLib 4.5.8** (`geckolib-forge-1.21`, the last GeckoLib build for 1.21 on Forge; `software.bernie.geckolib.*`), a required dependency, declared in `build.gradle` and `mods.toml` |

Versions live in [gradle.properties](gradle.properties). Do not change them unless asked.

Version pitfalls to watch for:
- Use Forge's `DeferredRegister` + `RegistryObject` from `net.minecraftforge.registries`. NeoForge's `DeferredHolder`, `DeferredItem`, `DeferredBlock` and `net.neoforged.*` imports do not exist here.
- Since 1.20.5, item stacks use **data components** instead of NBT, and food uses `FoodProperties.Builder().nutrition(..).saturationModifier(..)`.
- In 1.21, `new ResourceLocation(...)` is gone: use `ResourceLocation.fromNamespaceAndPath(..)`.
- In 1.21, data folders are **singular**: `loot_table/`, `recipe/`, `tags/block/`, `tags/item/` (not `loot_tables`, `recipes`, `tags/blocks`).
- Item models go in `assets/horrormod/models/item/`. The `assets/<modid>/items/` item-definition folder is 1.21.4+ and does not apply.
- Mod metadata is `META-INF/mods.toml`, not `neoforge.mods.toml` or `fabric.mod.json`.

## Identity

- Mod ID: `horrormod` (constant `HorrorMod.MOD_ID`)
- Base package: `net.ridez.horror`
- Main class: [HorrorMod.java](src/main/java/net/ridez/horror/HorrorMod.java)

## Folder conventions

Java (`src/main/java/net/ridez/horror/`):
- `block/ModBlocks.java`: block registry. `registerBlock(..)` also registers the matching `BlockItem`.
- `block/custom/`: custom block classes (e.g. `MagicBlock`).
- `item/ModItems.java`: item registry.
- `item/custom/`: custom item classes (e.g. `ChiselItem`, `FuelItem`).
- `item/ModFoodProperties.java`: food definitions.
- `item/ModCreativeModeTabs.java`: the mod's own creative tabs. Vanilla tab additions are in `HorrorMod.addCreative`.
- `client/`: client-only code (e.g. `ModSplashes`, the title-screen splash picker; `HorrorTitleScreen`, the mod's title screen, which replaces the vanilla one through `ScreenEvent.Opening`).
- `art/tools/title_screen_art.py`: draws the title screen art (`textures/gui/title/name.png`, the faded mod name, from `art/fonts/IMFellEnglish-Italic.ttf`, OFL licence alongside; `skinwalker.png`, the textured Skinwalker model posed peeking in from the right edge). Run: `python3 art/tools/title_screen_art.py` (needs Pillow); re-run after changing the model or its texture.
- `loot/`: global loot modifiers (`AddItemModifier`, registered in `ModLootModifiers`).
- `mixin/`: mixins, listed in `src/main/resources/horrormod.mixins.json` (loaded via `build.gradle` and the `--mixin.config` arg in `.vscode/launch.json`).
- `entity/ModEntities.java`: entity registry (projectiles, traders) and their attributes; `entity/ModTrades.java`: each trader's trades; `entity/custom/`: entity classes. Renderers go in `client/` (`ModEntityRenderers`).
- `fluid/`: fluids (`ModFluids`; `TarFluidType`/`TarFluid`, `NutrinonFluidType`/`FlowingNutrinonFluid`). The liquid block is in `ModBlocks`, the bucket in `ModItems`.
- `worldgen/`: custom world generation features (`TarPitFeature`, registered in `ModFeatures`). Data-driven worldgen is in `data/horrormod/worldgen/` and added to biomes by `data/horrormod/forge/biome_modifier/`.
- `recipe/`: custom recipe types (`ToolDamagingShapelessRecipe` for recipes that use up Scalpel durability).
- `art/tools/skinwalker_anim.py`: generates the Skinwalker blockout rig and every animation in `art/skinwalker_blockout.bbmodel` (plus the reference sheet), and the files the game loads: `assets/horrormod/geo/entity/skinwalker.geo.json` and `animations/entity/skinwalker.animation.json` (GeckoLib format; never edit those by hand either). Edit poses/animations there, never by hand in the file. Run: `python3 art/tools/skinwalker_anim.py art /tmp` (needs Pillow). `art/tools/check_anims.py` checks every animation as Blockbench plays it (`check [names]`: ground contact and joint snaps; `preview names [view] [out.png]`: filmstrips; `rest names`: how far each body part ends above the ground). `art/tools/check_run.py <tools dir> <bbmodel>` checks `run_standing` in detail. Blockbench quirk: animation keyframe X/Y rotations are inverted vs editor rotations (the script handles it).
- `art/tools/skinwalker_texture.py`: paints the Skinwalker texture (`textures/entity/skinwalker.png`, plus `skinwalker_glowmask.png` for the glow layer and `skinwalker_eye_glow.png`, the eyes and socket glow drawn on top as added light) from the model's cubes. Running it overwrites any hand painting on those files; the model generator only links the PNG, so regenerating animations is safe.
- Skinwalker in game: `entity/custom/SkinwalkerEntity.java` (a GeckoLib `GeoEntity`: picks its animation in `animate`; one-off actions are started on the server through the synced `ACTION`/`ACTION_COUNT` data, not `triggerAnim`), `entity/custom/SkinwalkerHearing.java` (hears blocks placed/broken), `entity/custom/SkinwalkerCommand.java` (`/skinwalker sleep`, for testing), `entity/custom/SkinwalkerDamageSource.java` (picks its random death message), `client/SkinwalkerModel.java` and `client/SkinwalkerRenderer.java` (scale, glowing eyes, no death tilt). Its hits use the `horrormod:skinwalker` damage type (`data/horrormod/damage_type/`, in the vanilla `bypasses_armor`/`bypasses_enchantments` tags); its spawn biomes are the `horrormod:skinwalker_spawns_in` biome tag.
- `item/custom/ModCrossbowItem.java`: base for crossbow-style weapons (Straw, Toy Gun); load time applied via `mixin/CrossbowItemMixin`.

Resources (`src/main/resources/`):
- `assets/horrormod/blockstates/`: one blockstate JSON per block
- `assets/horrormod/models/block/` and `models/item/`: models
- `assets/horrormod/textures/block/` and `textures/item/`: PNG textures
- `assets/horrormod/lang/en_us.json`: display names for items, blocks and creative tabs
- `data/horrormod/loot_table/blocks/`: block drops
- `data/horrormod/recipe/`: recipes
- `data/horrormod/loot_modifiers/` + `data/forge/loot_modifiers/global_loot_modifiers.json`: extra drops added to vanilla loot
- `data/minecraft/tags/block/`: `mineable/<tool>.json` and `needs_<tier>_tool.json`, required for `requiresCorrectToolForDrops()` blocks to drop
- `assets/minecraft/texts/splashes.txt`: title-screen splash texts. Replaces the vanilla list; supports `§` formatting codes
- `META-INF/mods.toml`: contains `${...}` placeholders filled in from `gradle.properties` at build time. Leave them as placeholders.

All JSON is hand-written; data generation is not used (`src/generated/resources` is empty).

A new block usually needs: a registry entry, a blockstate, a block model, an item model, a texture, a lang entry, a loot table, mineable/tool-tier tags, and a creative tab entry. A new item needs: a registry entry, an item model, a texture, a lang entry, and a creative tab entry.

Registry names are lowercase `snake_case`, and the Java field name matches the registry name (e.g. `veinseep`).

## Build and test

```bash
./gradlew build        # compile + package; the jar goes to build/libs/
./gradlew runClient    # launch Minecraft with the mod
```

In VSCode: Run and Debug (Ctrl+Shift+D) → **runClient**. [.vscode/launch.json](.vscode/launch.json) is hand-patched to load the mod from `build/sourcesSets/main` instead of `bin/main`. Re-running `./gradlew genVSCodeRuns` overwrites that patch and the game crashes on launch until it's re-applied.

There are no automated tests. "Works" means it compiles with `./gradlew build` and the user checks it in game.

## Rules

- **Maximum compatibility:** this mod will ship in a modpack.
  - Never override vanilla data files (loot tables, recipes) to change vanilla behavior; use Forge global loot modifiers, tags, and events instead.
  - Mixins only when there's no Forge hook. Use `@Inject`, never `@Redirect`/`@Overwrite`, and narrow them to this mod's content where possible.
  - Detect tools by Forge `ToolActions` or tags, not by exact vanilla item, so modded tools work.
- Don't add features I didn't ask for. No extra items, blocks, recipes, refactors or "improvements" beyond the request.
- Run `./gradlew build` after every code change and fix any errors before saying it's done.
- Ask before changing existing registries: renaming, removing or re-IDing an existing block, item or tab breaks existing worlds. Adding new entries is fine.
- Ask before changing versions in `gradle.properties` or editing `build.gradle`.
- Match the existing code style (Forge `DeferredRegister` pattern, lowercase field names).
- When adding a block or item, create all its resource files listed above, and add any texture I still need to draw to [TEXTURES.md](TEXTURES.md) with a description.
- If unsure whether an API exists in Forge 1.21, check the decompiled sources in the Gradle cache rather than guessing from other versions or loaders.
