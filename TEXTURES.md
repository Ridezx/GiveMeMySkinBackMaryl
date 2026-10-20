# Textures

What still needs art, and what each texture should look like. Item textures are **16×16 PNG**. Until a file exists, the game shows the purple-and-black "missing texture" squares.

Put each file at the path given, under `src/main/resources/assets/horrormod/textures/`. The file name must match exactly, because the item's model already points to it.

## Needed now

These items are already in the game and are waiting for art.

| Item | File | What it should be | Recommendation |
|---|---|---|---|
| Mytoma | `block/mytoma.png` | Very dark red hardened blood | A **generated placeholder** is in place (mottled near-black red) so it isn't a missing texture; replace it with real art. Hardened/cracked, maybe with a faint glossy or veined look like dried blood |

## Done

| Item / block | File |
|---|---|
| Pelt | `item/humanskin.png` |
| Toy Gun (held, 3D) | `item/toy_gun_held.png` + `models/item/toy_gun_held.json` (from `art/tools/toy_gun_model.py`): the rifle built from blocks after the reference photo, flat colour swatches |
| Toy Gun scope overlay | `misc/toy_gun_scope.png` (256×256, from `art/tools/item_textures.py`): the round view through the toy scope, smoky brown at the rim, thin crosshair and a red dot |
| Pile of Berries | `item/berry_pile.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Speedy Car | `block/speedy_car.png` + `models/block/speedy_car.json`: a toy supercar in a windowed display box (first version, from `art/tools/speedy_car_texture.py`) |
| Aspiraton Drop | `item/aspiraton_drop.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Rotted Pelt | `item/rotted_pelt.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Graft | `item/graft.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Scalpel | `item/scalpel.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Holy Text | `item/holy_text.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Salt | `item/salt.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Straw | `item/straw.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Toy Gun | `item/toy_gun.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Heroine | `item/heroine.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Crank | `item/crank.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Her Favorite Dress | `item/her_favorite_dress.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Grape Hugs | `item/grape_hugs.png` (first version, from `art/tools/item_textures.py`; paint over it freely) |
| Cloth Strap | `item/tourniquet.png` |
| Aspiraton | `item/aspiraton.png` |
| Dust | `item/dust.png` |
| Dust Ball | `item/dust_ball.png` |
| Chisel | `item/chisel.png` |
| Veinseep | `block/veinseep.png` |
| Deepslate Veinseep | `block/veinseep_deepslate.png` |
| Aspiraton Block | `block/aspiraton_block.png` |
| Dust Bricks | `block/dustbricks.png` |
| Avolition | `block/avolition.png` |
| Magic Block | `block/magic_block.png` |
| Nutrinon | `block/nutrinon_still.png` + `block/nutrinon_flowing.png`, animated (first version, from `art/tools/fluid_textures.py`). The Nutrinon Bucket draws the fluid automatically |
| Tar | `block/tar_still.png` + `block/tar_flowing.png`, animated (first version, from `art/tools/fluid_textures.py`). The Tar Bucket draws the fluid automatically |
| Tactical Vest | `item/tactical_vest.png` (first version, from `art/tools/tactical_textures.py`; paint over it freely). Worn: `models/armor/tactical_layer_1.png` (vanilla's 64x32 armor layout; the vest over the body, no sleeves) |
| Tactical Boots | `item/tactical_boots.png` (first version, same script). Worn: the lower legs of `models/armor/tactical_layer_1.png` |
| Bleeding (effect icon) | `mob_effect/bleeding.png`, 18x18 (first version, same script) |
| Sweet Berry Bush | Uses vanilla's `sweet_berry_bush_stage3` texture. Optional: draw `item/sweet_berry_bush.png` and point the model at it if you want a custom look |


## Later

These things are in the design but not in the game yet.

| Thing | What it should be | Notes |
|---|---|---|
| Straw charging/loaded frames | The Straw while loading, and while loaded | Optional. Like the crossbow: `straw_pulling_0` to `_2` plus `straw_charged`. Without them the Straw looks the same at every stage; the player's arm pose and the tooltip show when it's loaded |
| Gustav, Walter, Frank | Traders | **Needed now** for the placeholder: `entity/gustav.png`, `entity/walter.png`, `entity/frank.png`, 64×64 in the **vanilla villager texture layout** (start from a copy of the villager skin). Until then they show the missing texture. Once their real models are designed (e.g. in Blockbench), they get new textures for those models |
| Skinwalker | The Rake: a gaunt, pale human head and stolen human skin stretched over a dog's frame (deep narrow chest, tucked waist, long thin arms, dog hind legs). The skin is visibly not its own: loose, draped, sliding off | **First texture done** (bald variation): `src/main/resources/assets/horrormod/textures/entity/skinwalker.png` (256x256, box UV), painted by `art/tools/skinwalker_texture.py` and linked into `art/skinwalker_blockout.bbmodel`, so it shows in Blockbench and can be painted over there. `skinwalker_glowmask.png` holds just the eyes; GeckoLib draws them glowing in the dark. `skinwalker_eye_glow.png` (also from the script) is the eyes plus a faint glow on the socket walls, added on top as light; black there adds nothing. Re-running the texture script overwrites hand painting. In game it uses this model and texture (GeckoLib) |
| Hollow | Mob | **First version done**: `textures/entity/hollow.png` (and the model), painted with the model by `art/tools/hollow_model.py` (re-running it overwrites hand painting) |
| Odie | The Hat Man: tall man in a long dark coat, striped shirt, dark tie, face in shadow under a wide-brimmed hat | **First texture done**: `textures/entity/odie.png`, painted with the model by `art/tools/odie_model.py` (re-running it overwrites hand painting) |
| Sandman | A ghost of a drug-squad officer: a head of cold blue fire burning to black, Haze's kit (vest, jodhpurs, boots) | **Done**: `textures/entity/sandman.png` and `sandman_glow.png` (the glow, added on top as light; not a GeckoLib glowmask, which would erase the texture under it), painted with the model by `art/tools/sandman_model.py` (re-running it overwrites hand painting) |
| Angel | Mob | **First version done**: `textures/entity/angel.png` (and the model), painted with the model by `art/tools/angel_model.py` (re-running it overwrites hand painting) |
| Playroom Exit | Block | **First version done**: `block/playroom_exit.png` (animated, 12 frames: near-black with deep red-purple veins crawling and pulsing, a rot eating into the house), drawn by `art/tools/playroom_textures.py` |
| Painted Sky, Painted Sun, Painted Cloud, Painted Leaves, Dark Painted Leaves, Painted Bark, Painted Grass | Blocks | **First version done**: `block/painted_*.png` (flat colour with soft brushstrokes, so they read as painted: the Child's Playroom's sky ceiling and the trees on its walls), drawn by `art/tools/playroom_textures.py` |
