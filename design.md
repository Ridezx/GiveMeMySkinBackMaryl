# Design: Give Me My Skin Back, Maryl

This is the source of truth for what the mod should be. If code and this doc disagree, ask which one is right; don't silently "fix" either. Items marked **Open** are undecided: ask, don't invent.

## Theme and tone

The mod is about substance abuse and recovery. **Skin** is a person's way of coping with the world: it can protect you well or badly, and not every skin works for every person. Losing your skin means being exposed, so even a loving touch hurts. People who are struggling push others away out of shame, or out of fear that their substance will be taken away and things will hurt more. At its core, the story is about **needing to talk to someone**, and about recovery being slow rather than an overnight miracle.

Tone:
- Psychological dread, body horror, entity horror.
- Jump scares are **very scarce**.
- **No comedy.** Every name, tooltip and piece of text should stay serious.

The drug references are deliberate and handled as symbolism: they're never glamorized and never played for laughs. Any new text written for the mod (tooltips, messages, names) must match this tone.

## Story

An unnamed man is the only one who speaks. Maryl's responses are implied (shown in parentheses).

> Give me my skin back, Maryl.
> I feel…
> I feel… exposed.
> *(Maryl smiles)*
> You shouldn't smile, Maryl. I know you only mean the best, but…
> *(Maryl reaches out)*
> No…don't touch.
> I don't know if it will hurt.
> I can only think it would.
> *("Take your time.")*
> You tell me you will wait?
> Until when? When you give me back my skin?
> *(Unknown response: could be affirmative or not answered)*
> …
> …
> …
> Could you at least look away?
> I'm indecent.
> *(Unknown response: could look away, could stay staring)*
> …
> …
> …
> Maryl, if I said I'm sorry, would you give me back my skin?
> *("Would you mean it?")*
> Yes, I would mean it.
> I just want…my skin.
> *("I…don't know")*
> I'm sorry, Maryl. I won't hide again, just…
> Give me my skin.
> …
> *(Maryl gives him some skin.)*
> This isn't my skin, Maryl.
> You've lied to me.
> WHERE IS MY SKIN, MARYL!
> *(Maryl brings his skin to her mouth.)*
> No…you can't...
> THAT'S MY SKIN, MARYL!
> PLEASE, I BEG… I BEG.
> Don't eat my skin. I need it.
> *(Maryl takes a giant bite of skin.)*
> …
> The tears, they burn my…my…
> I do not know what they burn, but I feel it.
> I feel a tingling.
> It burns…warm?

### What each part means

1. **"Give me my skin back… You shouldn't smile."** His first attempt to give up his skin (the substance), which amounts to quitting cold turkey. Withdrawal hasn't hit yet, but he knows it's coming. Maryl smiles because him being without drugs right now looks like progress to her, ignoring that cold turkey will only make it worse.
2. **"No…don't touch."** Maryl reaches out, both to embrace him and to touch his heart and give him strength. He fears it will hurt, and that he'll scare her away. He needs time, not an impossible overnight recovery.
3. **"Take your time." / "Until when?"** How long Maryl will wait is ambiguous, and that is his fear: how long is too long? Her unspoken answer has three readings, and **these define the three endings**:
   - "As long as you need" → **good ending**
   - "As long as I can" → **ambiguous ending**
   - "Right now" → **bad ending**
4. **"If I said I'm sorry…"** Bargaining. He wants his old, "working" skin back, but that's the wrong answer: the old skin would make things worse. He needs to shed the skin that doesn't work and find new skin.
5. **"This isn't my skin."** Maryl gives him a piece of *her* skin: what works for her should work for him, right? No. It stands for broken promises and broken trust, and it blends into anger. (The grief stages are intentionally out of order.)
6. **"Don't eat my skin."** The horror beat. Maryl biting the skin has two readings: (1) she destroys his skin so he can't go back, or (2) she starts taking the drug herself and falls into the same pit.
7. **"It burns…warm?"** Either way he feels warmth. In reading 1, depression gives way to acceptance. In reading 2, he's happy to share his skin with the woman he loves, even though it does them no good.

## Game structure

- Two variants are planned: **open world** (building this first) and **set path**.
- Core mod first; cutscenes and dimensions are later and acknowledged as ambitious.
- All entities are normal mobs. **Open:** a boss (none decided yet).
- **Open:** how the three endings are triggered in open world.

## Items

| In game | Texture / looks like | Represents | How you get it | Function | Status |
|---|---|---|---|---|---|
| Rotted Pelt | Rotted human skin | Rotten human skin | Drop from Hollows (1–3). Skinwalkers drop regular Pelt instead | Shapeless: 4 Rotted Pelt + Scalpel → 1 Pelt. Edible: 4 hunger, 2 saturation, Nausea 5s + Poison 2s | Item + recipe done; drops wait on Hollows |
| Pelt | Human skin | Human skin | Shapeless: 4 Rotted Pelt + Scalpel | Edible: 12 hunger, 14.4 saturation. Stacks to 64 | Done |
| Graft | Small piece of skin / skin graft | Skin graft | Shapeless: 1 Pelt + Scalpel → 2 Graft | Edible: 6 hunger, 8 saturation | Done |
| Cloth Strap | Tourniquet | Tourniquet | Shaped: 3 wool (any color) in a row + 1 wool under the rightmost (rotated L, either direction) | Stops bleeding. **Open:** exact mechanic | Recipe done; no function yet |
| Heroine | Female action figure | Heroin | Scattered through the world | **Open** | Registered |
| Dust | Dust | Cocaine, and dust buildup in old places | Smelt a Sweet Berry Bush item (furnace or smoker) | Crafting; Sandman trigger | Recipes done |
| Dust Ball | Dust ball | Toy Gun ammo / packed cocaine | 2×2 Dust; or 1 Dust Brick → 4 | Toy Gun ammo; 1 → 4 Dust | Recipes done |
| Crank | Jack-in-the-box | Methamphetamine | Found in the world; traded by Walter. Not craftable | Trade good | Registered |
| Speedy Car | Toy Lamborghini in its box | Speed (still in its packet) | Found in the world. Not craftable | Placeable like a player head, faces whoever placed it, own 3D model | Placeable; placeholder model (red box, yellow front) until the real model |
| Salt | Salt | Salt | Smelt a water bucket; the furnace keeps the empty bucket (needs custom code; vanilla would destroy it) | Straw ammo | Recipe done (furnace only; bucket stays in the input slot) |
| Toy Gun | An old western 30-30 lever-action rifle in bright plastic colors | — | Found in the world, rare (in mod structures, once they exist). **Not craftable** | Works like a crossbow: hold to load (3s), stays loaded, use again to fire. Dust Ball ammo only. Fires at near-hitscan speed (15.0, vs. crossbow arrow 3.15); 16 damage (8 hearts) per hit; the Dust Ball bursts and can't be picked up. 150 durability, 1 per shot | Done; texture needed; not obtainable in survival yet |
| Straw | Straw | Straw | Shaped: 3 bamboo in a vertical line | Works like a crossbow: hold to load (0.5s), stays loaded, use again to fire. Unbreakable, not stackable. Fires Salt at snowball speed with arrow drop: no damage, knockback only, can't be picked up. An early defense item; **Salt only** | Done; Skinwalker salt effect waits on Skinwalkers |
| Holy Text | Golden book | Any holy text | Shaped: Book surrounded by 8 Gold Ingots | In off hand: Luck I + Strength I. Not stackable | Done |
| Her Favorite Dress | **Open** | **Open** | Trade from Gustav (5 Dust Bricks) | **Open:** what it does | Item exists; texture needed |
| Scalpel | Scalpel | Scalpel | Shaped: Iron Ingot and Stick diagonal (either direction) | Tool, not stackable, 238 durability (same as shears). In its crafting recipes it stays in the grid and loses 1 durability per craft; breaks at 0. Weapon: 1 damage, 4 attack speed | Done; texture needed |
| Sweet Berry Bush | Vanilla bush texture | The bush itself | Break a sweet berry bush with any shears (including modded shears) | Smelts into Dust; plants a young bush | Done |
| Grape Hugs | Styrofoam cup with purple inside | Lean | Found in the world. Not craftable | Drinkable (drinking animation and sound), restores 2 hunger bars (4 points), 0.8 saturation | Done; not obtainable in survival until structures/loot exist |
| Aspiraton | Hardened blood shard | Blood. The name is an **intentional misspelling**, a play on "aspiration" (drawing fluid from a body); never "correct" it | Mining an Aspiraton Block (2–4, up to 9 with Fortune); 1 Aspiraton Block → 9 | 9 → Aspiraton Block | Done |
| Aspiraton Drop | Shaped like lime dye, deep dried-blood red | Dried blood | Mining Veinseep / Deepslate Veinseep (2–4, glowstone-style) | **Open** | Registered; texture needed |
| Chisel | — | — | **Open** recipe | Right-click stone / deepslate / end stone → brick version | Done |

Test items `placeholderfood` and `placeholderfuel` exist only for testing.

## Blocks

| Block | Function | Status |
|---|---|---|
| Veinseep / Deepslate Veinseep | Drops like glowstone: 2–4 Aspiraton Drop, Fortune adds more but caps at 4, Silk Touch drops the block. Surrounds the blood veins (see World generation) | Done; generates like diamond ore |
| Aspiraton Block | Crafted from 9 Aspiraton (3×3). Drops 2–4 Aspiraton; Fortune I 3–6, Fortune II 5–8, Fortune III always 9; Silk Touch drops the block | Done |
| Mytoma | Very dark red hardened Nutrinon. Forms wherever Nutrinon touches water (like lava + water making obsidian). Hardness 6 (between deepslate and obsidian), needs a stone pickaxe or better, drops itself | Done; placeholder texture |
| Dust Bricks | Building block; 2×2 Dust Ball; 1 → 4 Dust Ball | Done |
| Avolition | Pure black block for void-like rooms and cutscene spaces. Not meant to be mined (very high strength); drops itself if someone does | Done |
| Nutrinon | A bright red **liquid**, the blood in the veins. Flows like water and can be swum in (and drowned in), but swimming is slower than water and faster than lava. Two sources never make a new one, so it can't be farmed endlessly. Flowing Nutrinon sheds dark red droplets; still Nutrinon has no particles. Picked up and placed with a Nutrinon Bucket | Done; placeholder look (tinted water textures) |
| Tar | A **liquid** that behaves exactly like lava (flow, spread, movement, fog, bucket) but gives no light and never burns or damages. Picked up and placed with a Tar Bucket | Done; placeholder look (tinted water textures). Found in tar pits (see World generation) and some structures; tar pits not built yet |
| Magic Block | Quest turn-in spot (see Magic Block quests) | Exists; quest system not started |
| Speedy Car | Placeable display block (see Items) | Done; placeholder model |

### Vanilla change

- Breaking a **sweet berry bush with shears** drops the Sweet Berry Bush item, on top of the normal berry drops. Done with a Forge global loot modifier (not a loot table override), so it stacks with other mods' changes.

## Magic Block quests

The unnamed man gives the player quests to bring him items. The player chooses which items to hand over and which to keep, and hands them over by tossing them onto the Magic Block, where he takes them.

**Open:** what the quests are, what rewards they give, and whether the player's choices feed into the three endings.

The current code turns dropped Pelt into Graft on the Magic Block. Treat this as placeholder behavior from before this design.

## Mobs

| Mob | Concept | Behavior | Drops | Status |
|---|---|---|---|---|
| Skinwalker | Based on **The Rake**: a dog that stole a human's skin and is still learning to be human. A gaunt human head and skin on a dog's frame: deep narrow chest, tucked waist, long thin arms that double as front legs, and **dog hind legs** (knee forward, hock back, up on its toes) even when standing upright. 4.5 blocks tall standing. The stolen skin hangs loose (a hood over the back of the head, a drape down its back). That skin doesn't work for it, so it wants to take yours and see if it fits. **On all fours** (from the user's annotated photo): arms straight down under the shoulders; torso and chest rising in one line from the hip to the neck, head on top; the femur running from the hip (the low point of a 'V') up and back to the knee; the long tibia dropping straight down from the knee. About 2.5 blocks tall in game. When moving on all fours its limbs spread out for balance. It fits 3-block-high spaces but still not 2-block doorways. Later: switching to all fours in confined spaces needs the animations plus a smaller hitbox | 70% hunt players on sight; the rest ignore them until hurt. 100 health; normal attacks do only 25% damage, so players should run. Hits for 13 (6.5 hearts). Chases at ~6.4 blocks/s: faster than a sprinting player on flat ground, just under a sprint-jumping one. When scared (hit by a player and rolls "flee", or 3 Salt hits) it runs at hyperspeed (~26 blocks/s) away from that player for 15 seconds. When a player hurts it: screams, then flees or fights (50/50). 3 Salt hits: screams and flees. 2nd Dust Ball hit: dies instantly. A first Dust Ball hit has a 10% chance to enrage it instead of the flee/fight roll: it freezes in place staring at the shooter and screams for 1.5 seconds (it can still be shot meanwhile), then charges them at ~16 blocks/s for 5 seconds. Too tall for doorways. Spawns rarely in the dark anywhere in the overworld, always alone | 1–3 Pelt (+Looting) | Done; placeholder look (scaled enderman, no texture) and placeholder enderman sounds. **Open:** exact spawn rules (biomes, rarity) |
| Hollow | Someone who chose the wrong skin too many times | Takes damage but no knockback; does not react to damage at all | 1–3 Rotted Pelt | Not started |
| Odie | The Grim Reaper, personification of death | Never attacks; only watches | **Open** | Not started |
| Sandman | Police | Appears when the player carries too much Dust. If he catches you, you're sent to the Torture dimension. Cares only about your past dealings, not your current state | **Open** | Later |
| Angel | Biblically accurate angel | Touching it sends you to the Child's Playroom | **Open** | Later |

Current mob focus: **Skinwalker, Hollow, Odie.**

### Weapon effects on Skinwalkers

- **Dust Ball (Toy Gun):** the second Dust Ball hit on a Skinwalker kills it instantly, on top of the normal damage.
- **Salt shot:** a Skinwalker hit 3 times screams and runs away.

### Skinwalker animations

Made in Blockbench on the rig in `art/skinwalker_blockout.bbmodel`. Head-tracking (looking at the player) and matching the walk/run cycle speed to its movement speed are done in code, not keyframed.

**Done**
- [x] Resting on all fours, with a slow breathing bob (`rest_on_fours`, 3 s loop)
- [x] Walking on all fours (`walk_on_fours`)
- [x] Walking while standing (`walk_standing`, 1.2 s loop)
- [x] Resting while standing, with the same breathing (`rest_standing`, 3 s loop)
- [x] Alert while standing: perks up over 0.25 s, then holds perfectly still (`alert`)
- [x] Alert on all fours: chest and head lift, limbs stay planted (`alert_on_fours`)
- [x] Look around after an alert, searching for the sound: starts and ends on the alert pose, snaps the head to one side and holds listening with a head tilt, snaps to the other side and holds, glances up, settles (`look_around`, 4 s loop, linear snaps)
- [x] Look around on all fours (`look_around_on_fours`; neck and head turn, chest stays still so the arms stay planted)
- [x] Running on all fours: a gallop (front legs reach together, hind legs drive together, back flexes and stretches, brief airborne moments), for chasing and the enraged charge (`run_on_fours`, 0.5 s loop)
- [x] Panicked flee: the gallop made frantic. Lower and flatter, head ducked and jerking side to side as if glancing back, longer and wilder strides with the front legs landing almost together, deeper back flex, bigger airborne moments, jerky wobble (`flee_on_fours`, 0.4 s loop)
- [x] Standing → crawling (`stand_to_crawl`)
- [x] Crawling → standing (`crawl_to_stand`)
- [x] Climbing up pillars (so players can't escape by building up) (`climb_up`)
- [x] Climbing down / dropping from the top (`climb_down`)
- [x] Climbing over the top onto a ledge, ending on all fours (`climb_over_ledge`)
- [x] Climbing over the top onto a ledge, ending standing (`climb_over_ledge_to_stand`)
- [x] Jumping / leaping on all fours (gaps, and lunging at the player) (`leap`)
- [x] Jumping / leaping while standing (`leap_standing`)
- [x] Running while standing: upright sprint on its dog legs (`run_standing`)
- [x] Panicked flee while standing: the standing sprint made frantic (`flee_standing`)
- [x] Swimming / wading (water and tar pits) (`swim`)
- [x] Lying down to sleep, on all fours: like a dog, turns a full circle (in the animation it turns on the spot; the game code moves it around the circle), then the rear sinks and it settles onto its belly, laying its head down on its side in front of its hands, hind legs close together out flat behind (`lie_down_on_fours`, 3.6 s)
- [x] Lying down to sleep, standing: lowers itself into a sit (knees up, forearms over the knees), stays a moment, then tips over onto its side like a person, an arm reaching out to catch it (`lie_down_standing`, 2.4 s)
- [x] Sleeping on all fours: the dog pose with its neck stretched out and its head lying on its side on the ground in front of its hands; only the chest rises and falls, very slowly (`sleep_on_fours`, 6 s loop)
- [x] Sleeping after lying down standing: on its side, head resting on its cheek, knees drawn up with the legs stacked together, slow deep breaths (`sleep_standing`, 4 s loop)
- [x] Waking from the dog pose onto all fours: pushes up off its belly (`wake_on_fours_to_on_fours`)
- [x] Waking from the dog pose to standing: pushes up onto all fours, then rears up through the forward bend (`wake_on_fours_to_standing`)
- [x] Waking from its side onto all fours: rolls up into a sit, leans onto its hands, drops to hands and knees, lifts its hips (`wake_standing_to_on_fours`)
- [x] Waking from its side to standing: rolls up into a sit, leans forward and pushes up onto its feet (`wake_standing_to_standing`)

**Movement**

Every movement has a standing and an on-all-fours version. Climbing (up, down) and swimming are their own body positions (flat against a wall / floating), so one version serves both stances.

All movement animations are done.

**Idle and state**

It sleeps in the stance it was in when it chose to sleep. When it wakes, the game picks the stance it ends up in at random (not based on how it slept), so there's a wake-up for each sleeping pose into each stance. The dog-style circling before lying down is movement done in game code (the AI walks it around a small circle while the walk animation plays, so the hitbox follows it), not baked into the animation.

All idle and state animations are done.

**Combat and reactions**
- [ ] Scream
- [ ] Enraged wind-up: the 1.5-second freeze before the Toy Gun charge (leaning toward the player, tensed)
- [ ] Punch (attack while standing)
- [ ] Slap or bite (attack while crawling)
- [ ] Hurt flinch (so its tanky 25%-damage hits still read as hits)
- [ ] Death, standing: the basic death when a player kills it with a sword, bow or any other normal attack
- [ ] Death, on all fours: the basic death, crawling version
- [ ] Dust Ball death, standing: when the killing blow is a Dust Ball (Toy Gun), it shakes violently, then dies
- [ ] Dust Ball death, on all fours: the shaking death, crawling version

Which death plays depends on what killed it (Dust Ball or anything else) and which stance it was in.

**Character**
- [ ] Sniffing: head low, tracking a scent (still a dog underneath)
- [ ] Head tilt: the curious dog tilt, unsettling on a human face
- [ ] Adjusting the stolen skin: tugging or resettling skin that doesn't fit

## Traders

Gustav, Walter and Frank are **their own trader mobs** (like the wandering trader), not villager professions, so they can have completely different models. Right-click to trade. All trades are available from the start, with no levels. Each trade can be used 12 times, then restocks once per Minecraft day. They never despawn. Each trade gives a few XP orbs.

Status: done, using the vanilla villager model as a placeholder with each trader's own texture slot. Spawn eggs exist. **Open:** how they appear in the world (wandering in like the wandering trader, living in structures, etc.).

**To do:** make the model textures for all three traders (Gustav, Walter, Frank). Some will look very different from villagers, so they need their own models as well as textures; see TEXTURES.md.

| Trader | Symbolizes | Trades (player gives → player gets) |
|---|---|---|
| Gustav | Lil Peep | 4 Dust Ball → 4 Emerald · 1 Dust Bricks → 5 Emerald · 1 Dust → 5 Coal · 1 Dust → 3 Steak · 5 Dust Bricks → Her Favorite Dress |
| Walter | Walter White | 1 Emerald → 2 Dust · 20 Emerald → 10 Dust Ball · 8 Emerald → 1 Dust Bricks |
| Frank | Frank Lucas | 10 Emerald → Crank · 10 Emerald → Heroine · 10 Emerald → Speedy Car · 10 Iron Ingot → Cloth Strap |

**Balance rule:** Dust, Dust Balls (4 Dust) and Dust Bricks (16 Dust) convert freely by crafting, so compare prices per Dust. Walter charges 0.5 emeralds per Dust on everything; Gustav pays at most ~0.33. Walter must always charge more per Dust than Gustav pays, or players can loop emeralds endlessly.

## World generation

- **Blood veins (Aspiraton):** done. The world is a layer of skin and Aspiraton is its blood veins: endless, winding, round tubes running mostly sideways between y -56 and 48 (tapering off at the top and bottom of that band). From the middle out: a channel about 6–7 blocks across, wide enough to swim through, holding **Nutrinon** (all still source blocks) whose level rises and falls along the vein: some stretches brim-full, others half-full, others a shallow stream with air above (about 40% Nutrinon overall); a ~1-block wall of Aspiraton Block, and a ragged outer layer of Veinseep (Deepslate Veinseep in deepslate), 0–2.5 blocks thick with bare gaps, like pieces flaking off. Spacing is tuned so a spectator sees roughly 3–7 veins at a time (simulated over several seeds). Veins cut through natural rock and ores, and are also built straight through caves, water and lava, so they stay sealed and never spill: in a cave you see a bare Aspiraton tube (the Veinseep flakes only form in rock). Structures are left alone. Code: `worldgen/AspiratonVeinFeature.java`; spacing, layer sizes and depth are constants at the top.
- **Tar pits:** done. Vanilla's surface lava pools, filled with Tar and lined with mud, in biomes tagged `#forge:is_swamp` (swamp, mangrove swamp, and modded swamps that use the tag; never jungles). About 1 attempt per 40 chunks of swamp; many attempts fail in swamps because pools won't generate touching water. The rim is decorated for an eerie feel: bare coarse dirt, dead bushes, bone blocks (some half-buried), the odd cobweb, a skeleton skull on about half of pits, and 1–2 leafless dark oak trees. Still Tar gives off drifting ash and wisps of smoke. Tar also appears in some generated structures (later). **Later:** ambient sound near the pits (you'll make the sounds).
- **Veinseep patches:** done, an exact copy of diamond ore generation (same sizes, counts and depths) using Veinseep / Deepslate Veinseep, in every overworld biome.
- **Open:** biomes (planned, not decided).

## Structures

- **Dilapidated Building:** five floors, one grief stage each:
  1. Denial: mirrors that don't reflect
  2. Anger: everything broken
  3. Bargaining: Skin Loom Room; people on the walls, skin sprayed out
  4. Depression: room full of Tar blocks that slow you down
  5. Acceptance: the man sits in a chair with no skin; you get the mission to help him find skin
- **Bathroom:** a standalone building that is just a bathroom.
- **Mecca Black Box:** a holy site where people search for skin. Higher Skinwalker spawn rate.
- **Bus:** school bus and city transit variants. Inside are many dead Skinwalkers.

## Dimensions (later)

- **Torture dimension:** white padded walls. The only way out is death or spending enough time inside. Reached via the Sandman.
- **Child's Playroom:** a return to innocent childhood. Plays a cutscene of a mother giving the child the Heroine doll, saying it will protect you from all you wish it to. Reached by touching an Angel; the portal is a pure white orb in the sky that looks like an angel.
- **Avolition:** a completely black realm. **Open:** how it's reached.

## Cutscenes (later)

- **Sandman:** a Skinwalker finds skin, dust flies out of him, and the Sandman shows up just as he finds salvation. "Sorry, kid…I just can't go back." The Skinwalker tries to run and is ripped apart in front of the player. The Sandman then turns on the player as "associated."

## Title screen

- **Custom main menu screen:** to do. The whole title screen gets a custom look to match the mod (background, layout, logo). **Open:** what it looks like.

### Splash texts

Done: `assets/minecraft/texts/splashes.txt` **replaces** the vanilla list, and `client/ModSplashes.java` blocks vanilla's holiday splashes and the rare "<name> IS YOU", so only these lines ever show. Each line is styled with `§` codes (chosen per line by the user; see the file).

- My eczema is really killing me…
- Maryl said I needed to grow thicker skin.
- What are you wearing?
- Is that your birthday suit?
- I'm sorry.
- WHERE IS MY SKIN, MARYL!
- Don't eat my skin. I need it.
- This skin doesn't fit me.

## Priorities

1. **Items:** the Straw (Salt) and Toy Gun (Dust Ball) are done; the rest of the item list. Then the villagers Walter, Gustav and Frank.
2. **World generation:** Veinseep patches, tar pits and Aspiraton blood veins done. Biomes still to decide.
3. **Mobs:** Skinwalker, Hollow, Odie.
4. Later: structures, biomes, the Sandman and Angel, dimensions, cutscenes, the set-path variant.

## Open questions and conflicts

1. **What Aspiraton Drop is used for.**
2. **"Found around the world"** (Heroine, Crank, Speedy Car, Grape Hugs): chest loot, structure loot, mob drops, or placed in the world?
3. **What makes Sandman spawn:** how much Dust counts as "too much"?
4. **Recipe for the Chisel.**
