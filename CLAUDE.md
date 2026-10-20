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
- `item/custom/`: custom item classes (e.g. `ChiselItem`).
- `item/ModFoodProperties.java`: food definitions.
- `item/ModCreativeModeTabs.java`: the mod's own creative tabs, the only tabs the mod's items are in (none go in vanilla tabs).
- `client/`: client-only code (e.g. `ModSplashes`, the title-screen splash picker; `HorrorTitleScreen`, the mod's title screen, which replaces the vanilla one through `ScreenEvent.Opening`; `HorrorScreens`, the title screen's look everywhere else: its art behind every menu outside a world, and the world loading, saving and portal screens, through `ScreenEvent.Render.Pre`/`BackgroundRendered`).
- `art/tools/item_textures.py`: draws the first versions of the item textures that were missing (16×16 pixel-art grids; Toy Gun, Heroine and Straw at 32×32, drawn from shapes; Rotted Pelt and Graft made from the Pelt picture). Run: `python3 art/tools/item_textures.py [names]`; it overwrites those files, so pass just the names to redo.
- `art/tools/fluid_textures.py`: paints the animated Nutrinon and Tar textures (`textures/block/<fluid>_still.png` 16×16 frames, `_flowing.png` 32×32 frames, plus `.mcmeta`); the fluid types draw them untinted.
- `art/tools/speedy_car_texture.py`: paints `textures/block/speedy_car.png`; its docstring lists the texture regions the hand-written `models/block/speedy_car.json` uses (the box, its window and the car inside).
- `art/tools/title_screen_art.py`: draws the title screen art (`textures/gui/title/name.png`, the mod name in dried-blood red, from `art/fonts/IMFellEnglish-Italic.ttf`, OFL licence alongside; `odie.png`, Odie's model tipping his hat, from `odie_model.py`; `blood_0..4.png`, the menu buttons' blood smears, each a strip of frames: the hover flowing in, then a sheen looping). Run: `python3 art/tools/title_screen_art.py` (needs Pillow; about a minute); re-run after changing Odie's model.
- `loot/`: global loot modifiers (`AddItemModifier` adds a drop, `LimitItemModifier` caps one; registered in `ModLootModifiers`).
- `mixin/`: mixins, listed in `src/main/resources/horrormod.mixins.json` (loaded via `build.gradle` and the `--mixin.config` arg in `.vscode/launch.json`).
- `entity/ModEntities.java`: entity registry (projectiles, traders) and their attributes; `entity/ModTrades.java`: each trader's trades; `entity/custom/`: entity classes. Renderers go in `client/` (`ModEntityRenderers`).
- `fluid/`: fluids (`ModFluids`; `TarFluidType`/`TarFluid`, `NutrinonFluidType`/`FlowingNutrinonFluid`). The liquid block is in `ModBlocks`, the bucket in `ModItems`.
- `worldgen/`: custom world generation features (`TarPitFeature`, registered in `ModFeatures`). Data-driven worldgen is in `data/horrormod/worldgen/` and added to biomes by `data/horrormod/forge/biome_modifier/`.
- `sound/ModSounds.java`: the mod's sound events; what each plays is in `assets/horrormod/sounds.json` (vanilla sound files as placeholders for now; put recordings in `assets/horrormod/sounds/` and point the entry at them).
- `recipe/`: custom recipe types (`ToolDamagingShapelessRecipe` for recipes that use up Scalpel durability).
- `art/tools/skinwalker_anim.py`: generates the Skinwalker blockout rig and every animation in `art/skinwalker_blockout.bbmodel` (plus the reference sheet), and the files the game loads: `assets/horrormod/geo/entity/skinwalker.geo.json` and `animations/entity/skinwalker.animation.json` (GeckoLib format; never edit those by hand either). Edit poses/animations there, never by hand in the file. Run: `python3 art/tools/skinwalker_anim.py art /tmp` (needs Pillow). `art/tools/check_anims.py` checks every animation as Blockbench plays it (`check [names]`: ground contact and joint snaps; `preview names [view] [out.png]`: filmstrips; `rest names`: how far each body part ends above the ground). `art/tools/check_run.py <tools dir> <bbmodel>` checks `run_standing` in detail. Blockbench quirk: animation keyframe X/Y rotations are inverted vs editor rotations (the script handles it).
- `art/tools/skinwalker_texture.py`: paints the Skinwalker texture (`textures/entity/skinwalker.png`, plus `skinwalker_glowmask.png` for the glow layer and `skinwalker_eye_glow.png`, the eyes and socket glow drawn on top as added light) from the model's cubes. Running it overwrites any hand painting on those files; the model generator only links the PNG, so regenerating animations is safe.
- Skinwalker in game: `entity/custom/SkinwalkerEntity.java` (a GeckoLib `GeoEntity`: picks its animation in `animate`; one-off actions are started on the server through the synced `ACTION`/`ACTION_COUNT` data, not `triggerAnim`), `entity/custom/SkinwalkerHearing.java` (hears blocks placed/broken), `entity/custom/SkinwalkerCommand.java` (`/skinwalker sleep`, for testing), `entity/custom/SkinwalkerDamageSource.java` (picks its random death message), `client/SkinwalkerModel.java` and `client/SkinwalkerRenderer.java` (scale, glowing eyes, no death tilt). Its hits use the `horrormod:skinwalker` damage type (`data/horrormod/damage_type/`, in the vanilla `bypasses_armor`/`bypasses_enchantments` tags); its spawn biomes are the `horrormod:skinwalker_spawns_in` biome tag.
- Odie in game: `entity/custom/OdieEntity.java` (watching, waving, leaving, crumbling; never saved), `entity/custom/OdieSpawner.java` (when and where he appears, and `/odie appear` for testing), `client/OdieRenderer.java`, `client/OdieFlash.java` (the black "Careful." flash, drawn over the HUD by the client-only `mixin/GuiMixin`, since Forge 1.21 has no HUD event). `art/tools/odie_model.py` generates his `geo/entity/odie.geo.json`, `animations/entity/odie.animation.json`, `textures/entity/odie.png` and `art/odie.bbmodel` (a Blockbench file with all of it, for viewing; edit the script, not those files; run `python3 art/tools/odie_model.py [preview dir]`).
- Angel in game: `entity/custom/AngelEntity.java` (its own flying, not the vanilla AI: the synced `STATE` (`HOVER`, `MOVING`, `DESCENDING`, `OPENING`, `OPEN`, `CLOSING`, `LEAVING`) picks the animation; building near it is heard through `BlockEvent.EntityPlaceEvent`; a Dust Ball or salt hit makes it flee (`LEAVING`, away from the shooter); a Holy Text `ItemEntity` on the ground brings it down to open), `client/AngelRenderer.java` (full brightness; tips the model by the entity's pitch round the ring's middle, except while standing open). Sounds `angel_wings`/`angel_open`/`angel_close` (placeholders). The portal doesn't lead anywhere yet.
- Angel model: `art/tools/angel_model.py` generates `geo/entity/angel.geo.json`, `animations/entity/angel.animation.json` (main: `hover`, `fly`, `ascend`, `descend`, `open`, `opened`, `close`; on their own controllers: `eye_open`/`blink` (the `eye_blink` bone) and `spin` (the ring's rotation)), `textures/entity/angel.png` (cutout feather tips) and `art/angel.bbmodel` (for viewing; edit the script; run `python3 art/tools/angel_model.py [preview.png]`). The wings are built straight out; every main animation gives their spread. Opening scales `ring` and `portal` up and `eye` to nothing, folds the wings behind (`FOLDED`), moving their roots out with the widening ring so they stay on its rim, and moves `body` so the lowest point is on the ground (it stands like a mirror); the white `portal` disc is scaled to 0 in every other main animation.
- Child's Playroom (the Angel's dimension): `dimension/ChildsPlayroom.java` (its key, building the room the first time anyone goes in, `sendIn`/`sendBack` remembering where the player came from in their persistent data), `data/horrormod/dimension/childs_playroom.json` and `dimension_type/childs_playroom.json` (an empty flat world; skylight, fixed midday, ambient light 0). The room's ceiling is `painted_sky`/`painted_cloud` (`block/custom/PaintedSkyBlock`: drawn solid, but skylight passes through it), so the yard gets full, even skylight and roofed spaces are dark; the walls are painted trees (`painted_leaves`, `painted_leaves_dark`, `painted_bark`, `painted_grass`), laid out in `ChildsPlayroom.wallAt`/`tree`; the yard's ground in `groundAt` (flat in the back `HOUSE_DEPTH` blocks for the house), `onPath`, `plantAt`. The built room's version is kept in the dimension's saved data (`horrormod_playroom`); raising `ROOM_VERSION` clears and rebuilds the whole room (so once the house is in, rebuild only what changed). The painted sun (`painted_sun`, at `SUN_X`/`SUN_H` on the back wall) lights the painted trees: `wallAt` shades each crown by its surface normal against the way to the sun. `dimension/PlayroomRules.java`: survival players can't break blocks there. `dimension/PlayroomCommand.java`: `/playroom enter|leave` (operators). Rebuilding the room clears down to `CLEAR_BELOW` under the floor (past the basement). The dollhouse is the user's Litematica schematic `data/horrormod/playroom/dollhouse.litematic`, read and placed by `dimension/PlayroomHouse.java` (at `CORNER`, the house's bottom-back-west corner; its bats skipped; the Playroom Exit put back in the basement wall with stone behind it). To change the house: drop in a new export of the same size and corner, and raise `ChildsPlayroom.ROOM_VERSION`. `PlayroomRules` also sends anyone who falls below the room back where they came from. `export/` (ignored by git) holds the house as vanilla structure pieces for loading in other worlds. The way out is `block/custom/PlayroomExitBlock.java` (`playroom_exit`, its animated texture, and the painted blocks' textures, from `art/tools/playroom_textures.py`). The Angel sends players in from its open portal (`AngelEntity.takeThroughPortal`).
- Holy places: `worldgen/HolyPlaces.java` (`near`/`inside`: in the grounds of a structure in the `horrormod:holy_places` tag, empty until the Mecca Black Box exists). Sandmen keep the peace near them (`SandmanEntity.nearHoly`), Skinwalkers won't go in (`SkinwalkerEntity.isHoly`).
- Hollow in game: `entity/custom/HollowEntity.java` (harmless wanderer: strolls, ignores players, no knockback, no hurt reaction except salt, which curls it up (the synced `CURL` state: curled for 45 s after the last hit, then `UNCURLING`; a low hitbox while curled); a lethal Dust Ball gives the `DUST_DEATH` shaking death; animations picked in `animate`, the walk's speed matched to its real speed, a client-side `twitch` now and then; body kept 60 ticks for the falling-apart death), `client/HollowRenderer.java` (no head tracking, no death tilt). No natural spawning yet (spawn egg only). Sounds `hollow_ambient`/`hollow_death`/`hollow_scream` (placeholders). Loot `loot_table/entities/hollow.json` (Rotted Pelt).
- Hollow model: `art/tools/hollow_model.py` generates `geo/entity/hollow.geo.json`, `animations/entity/hollow.animation.json` (`idle`, `walk`, `twitch`, `death`, `death_dust`, `death_curled`, `curl`, `curled`, `uncurl`; the upper body on its hunched `STANCE`, the legs solved by IK from where each foot goes (planted feet flat and still on the ground; the walk covers `WALK_SPEED` blocks a tick), the arms hung by their own weight, and in `death` the fall is run to find when each piece (forearm, upper arm, head, torso, jaw) first touches the ground; it breaks off then and is placed in model space from there, falling under gravity, flopping flat or rolling, never into the ground), `textures/entity/hollow.png` (cutout: the rips and holes in its clothes are transparent) and `art/hollow.bbmodel` (for viewing; edit the script, not those files; run `python3 art/tools/hollow_model.py [preview.png [head_preview.png]]`). Unlike the older model scripts, it names a cube's faces by where they are (`nz`, `px`, ...) and paints each texel from its 3D point on the model, mapped the way GeckoLib lays the texture on each face (the .geo.json `west` face is on the +x side).
- Sandman in game: `entity/custom/SandmanEntity.java` (targets, the gun, the strike; animations picked in `animate` from the synced `ACTION`/`ACTION_COUNT` and `AIMING` data, plus a looping `flicker` and the eyes (`eyes_open`, a `blink` every 15-25 s, `squint` while `isAggressive`) on their own controllers), `SandmanBullet.java` (its shot: a Dust Ball subclass; player damage by armor tier, Bleeding, knockback), `SandmanSpawner.java` (villages, pillager outposts and mansions, and Sandmen sent after players with a big record), `event/SandmanRecordEvents.java` (a player's record, points in `SandmanEntity.RECORD` in their persistent data, kept through death; the points per offence and the open-world threshold are `SandmanEntity.RECORD_*`), `client/SandmanRenderer.java` (her glow, `sandman_glow.png`, drawn as added light by its own layer, since GeckoLib's `AutoGlowingGeoLayer` erases the base texture under every non-empty glowmask pixel; fading as she dies; her rifle is modelled in, on the `gun` bone, and held by IK-posed arms in the script). Each trade with Gustav, Walter or Frank adds to the player's record (`TraderEntity.notifyTrade`). Tags: `horrormod:contraband` (item), `horrormod:sandman_hunts` (entity type), `horrormod:sandman_watches` (structure). `art/tools/sandman_model.py` generates `geo/entity/sandman.geo.json`, `animations/entity/sandman.animation.json`, `textures/entity/sandman.png`, `sandman_glow.png` and `art/sandman.bbmodel` (with the animations; for viewing; edit the script, not those files; run `python3 art/tools/sandman_model.py [preview.png [head_preview.png]]`).
- Sandman gear and Bleeding: `effect/` (`ModEffects`, `BleedingEffect`), `item/ModArmorMaterials.java` (the `tactical` material), `item/custom/TacticalVestItem.java` (its pockets live in the vest's `CONTAINER` data), `menu/` (`ModMenus`, `TacticalVestMenu`), `client/TacticalVestScreen.java` and `client/TacticalVestClient.java` (the B key; opening goes through `network/ModNetwork`'s `VestAction`), `event/TacticalGearEvents.java` (the vest's damage halving), `item/custom/ClothStrapItem.java` (the tourniquet). The boots' Tar effect is in `fluid/TarFluidType.move`. Damage types `horrormod:sandman_shot` and `horrormod:bleeding` (in `data/horrormod/damage_type/`, tagged in `data/minecraft/tags/damage_type/`). `art/tools/tactical_textures.py` draws the vest, boots, worn armor and Bleeding icon textures.
- `item/custom/ModCrossbowItem.java`: base for crossbow-style weapons (the Straw): ammo, load time (applied via `mixin/CrossbowItemMixin`), shot sound, firing particles. Its first- and third-person animations are in `client/WeaponAnimations.java`, applied through the client-only `mixin/ItemInHandRendererMixin` and `mixin/PlayerRendererMixin`, because vanilla handles every crossbow with fixed code before Forge's item-animation hooks are reached.
- Toy Gun: `item/custom/ToyGunItem.java` (its own weapon, not a crossbow: loaded/reloading state in the item's custom data, reload and fire on the server), `network/ModNetwork.java` (the mod's network channel: the client's reload key, right-click scope and left-click fire messages), `client/ToyGunClient.java` (the Reload key, left-click, scope zoom and the overlay `textures/misc/toy_gun_scope.png`, drawn under the HUD by `mixin/GuiMixin`), animations in `client/WeaponAnimations.TOY_GUN` through Forge's item hooks. Its model (`models/item/toy_gun.json`, Forge's `separate_transforms` loader) shows a 3D rifle in the hands (`toy_gun_held.json` + `textures/item/toy_gun_held.png`, generated by `art/tools/toy_gun_model.py`) and the flat pixel-art icon (`toy_gun.png`) in the inventory, on the ground and in item frames.

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

Forge 1.21 runs on Mojang's names in real installs too (`reobf = false`), so mixins need no refmap. To check the built jar outside dev: install a Forge 1.21-51.0.33 server (`java -jar forge-1.21-51.0.33-installer.jar --installServer <dir>`), put `build/libs/horrormod-*.jar` and the GeckoLib jar in its `mods/`, accept the EULA and run it with `nogui`; client-only code must never load there.

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
