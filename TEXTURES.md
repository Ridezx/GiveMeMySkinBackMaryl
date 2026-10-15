# Textures

What still needs art, and what each texture should look like. Item textures are **16×16 PNG**. Until a file exists, the game shows the purple-and-black "missing texture" squares.

Put each file at the path given, under `src/main/resources/assets/horrormod/textures/`. The file name must match exactly, because the item's model already points to it.

## Needed now

These items are already in the game and are waiting for art.

| Item | File | What it should be | Recommendation |
|---|---|---|---|
| Aspiraton Drop | `item/aspiraton_drop.png` | Shaped like lime dye, deep dried-blood red | Trace the lime dye silhouette and recolor it to dark, slightly brown reds so it reads as *dried* rather than fresh blood |
| Rotted Pelt | `item/rotted_pelt.png` | Rotted human skin | Start from the Pelt art (`humanskin.png`) so they read as the same thing, then shift it toward sickly yellow-green and gray with a few dark holes |
| Graft | `item/graft.png` | A small piece of human skin (skin graft) | A smaller, neater square or oval cut from the Pelt palette, with stitch marks or a clean cut edge, so it's clearly "cut from a Pelt" |
| Scalpel | `item/scalpel.png` | A scalpel | Drawn diagonally, with the blade at the top-right and the handle at the bottom-left, like vanilla tools (the model holds it like a sword). Thin silver blade, slightly darker metal handle |
| Holy Text | `item/holy_text.png` | A golden book | Use the vanilla book's shape with a gold cover and a small symbol on the front (keep the symbol generic, since it stands for any holy text) |
| Salt | `item/salt.png` | Salt | A small pile of white crystals with light gray shading. Sugar is white powder, so make the grains chunkier and more cube-shaped to tell them apart |
| Straw | `item/straw.png` | A drinking straw | A thin diagonal tube. The shot it fires uses the **Salt** texture, so Salt's art shows up in flight too. Optional charging/loaded frames are under Later |
| Toy Gun | `item/toy_gun.png` | An old western 30-30 lever-action rifle in bright plastic colors | Drawn diagonally like the Scalpel (the model holds it like a tool). Keep the classic silhouette (long barrel, lever loop under the grip) but use flat toy colors, e.g. a bright red or blue body, a yellow lever, and an orange tip on the barrel like real toy guns have. Its shots use the Dust Ball texture |
| Heroine | `item/heroine.png` | A female action figure | A small standing figure in a heroic pose, slightly worn or faded so it feels old and secondhand rather than new |
| Crank | `item/crank.png` | A jack-in-the-box | A box with the crank handle on the side and the lid shut, so the shape alone hints at what's inside |
| Her Favorite Dress | `item/her_favorite_dress.png` | A dress (Gustav's trade item) | Up to you: you haven't described it yet. A simple dress silhouette, maybe a little faded or stained to match the tone |
| Mytoma | `block/mytoma.png` | Very dark red hardened blood | A **generated placeholder** is in place (mottled near-black red) so it isn't a missing texture; replace it with real art. Hardened/cracked, maybe with a faint glossy or veined look like dried blood |
| Grape Hugs | `item/grape_hugs.png` | A styrofoam cup with purple inside | A white cup slightly wider at the top, seen from a little above so the purple drink shows at the rim. A faint purple drip down the side reads well at 16×16 |
| Speedy Car | `block/speedy_car.png` + a 3D model | A toy Lamborghini in its box | It's a placed block now, and the inventory shows the same 3D model, so it needs a block texture rather than a flat item icon. Right now it's a placeholder red box with a yellow front (vanilla concrete). The real model can be made in Blockbench; the front face of the box should point north |

## Done

| Item / block | File |
|---|---|
| Pelt | `item/humanskin.png` |
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
| Nutrinon | Uses vanilla's water textures tinted bright red. Optional: draw `block/nutrinon_still.png` and `block/nutrinon_flowing.png` (animated, like water's) and point `NutrinonFluidType` at them. The Nutrinon Bucket draws the fluid automatically |
| Tar | Uses vanilla's water textures tinted near-black. Optional: draw `block/tar_still.png` and `block/tar_flowing.png` (animated, like lava's) and point `TarFluidType` at them. The Tar Bucket draws the fluid automatically, so it needs no texture of its own |
| Sweet Berry Bush | Uses vanilla's `sweet_berry_bush_stage3` texture. Optional: draw `item/sweet_berry_bush.png` and point the model at it if you want a custom look |

`placeholderfood` and `placeholderfuel` are test items and don't need art.

## Later

These things are in the design but not in the game yet.

| Thing | What it should be | Notes |
|---|---|---|
| Straw charging/loaded frames | The Straw while loading, and while loaded | Optional. Like the crossbow: `straw_pulling_0` to `_2` plus `straw_charged`. Without them the Straw looks the same at every stage; the player's arm pose and the tooltip show when it's loaded |
| Gustav, Walter, Frank | Traders | **Needed now** for the placeholder: `entity/gustav.png`, `entity/walter.png`, `entity/frank.png`, 64×64 in the **vanilla villager texture layout** (start from a copy of the villager skin). Until then they show the missing texture. Once their real models are designed (e.g. in Blockbench), they get new textures for those models |
| Skinwalker | The Rake: a gaunt, pale human head and stolen human skin stretched over a dog's frame (deep narrow chest, tucked waist, long thin arms, dog hind legs). The skin is visibly not its own: loose, draped, sliding off (hood over the back of the head, drape down the back) | **Needs a model + texture.** A starting rig is in `art/skinwalker_blockout.bbmodel` with two animations (`rest_on_fours`, `walk_on_fours`); `art/skinwalker_reference.png` shows standing, resting on all fours and moving on all fours from three angles. In game it's still the scaled-up enderman placeholder until the finished model is exported |
| Hollow | Mob | Entity texture + model |
| Odie | The Grim Reaper; only watches | Entity texture + model |
| Sandman, Angel | Mobs | Later |
