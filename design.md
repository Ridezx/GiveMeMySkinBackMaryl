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
| Rotted Pelt | Rotted human skin | Rotten human skin | Drop from Hollows (1–3). Skinwalkers drop regular Pelt instead | Shapeless: 4 Rotted Pelt + Scalpel → 1 Pelt. Edible: 4 hunger, 2 saturation, Nausea 5s + Poison 2s | Done (dropped by Hollows) |
| Pelt | Human skin | Human skin | Shapeless: 4 Rotted Pelt + Scalpel | Edible: 12 hunger, 14.4 saturation. Stacks to 64 | Done |
| Graft | Small piece of skin / skin graft | Skin graft | Shapeless: 1 Pelt + Scalpel → 2 Graft | Edible: 6 hunger, 8 saturation | Done |
| Cloth Strap | Tourniquet | Tourniquet | Shaped: 3 wool (any color) in a row + 1 wool under the rightmost (rotated L, either direction); traded by Frank | A tourniquet: used (right-click) while Bleeding, it's tied on and the bleeding stops; one strap each time | Done |
| Heroine | Female action figure (flying, fist up, cape; faded) | Heroin | Scattered through the world; traded by Frank | Contraband: Sandmen hunt anyone carrying it (in the `horrormod:contraband` tag) | Item and texture done; only obtainable from Frank |
| Dust | Dust | Cocaine, and dust buildup in old places | Smelt a Sweet Berry Bush item (furnace or smoker) | Crafting; contraband (Sandmen chase anyone carrying any) | Recipes done |
| Dust Ball | Dust ball | Toy Gun ammo / packed cocaine | 2×2 Dust; or 1 Dust Brick → 4 | Toy Gun ammo; 1 → 4 Dust | Recipes done |
| Crank | Jack-in-the-box | Methamphetamine | Found in the world; traded by Frank. Not craftable | Trade good | Item and texture done; only obtainable from Frank |
| Speedy Car | Toy Lamborghini in its box | Speed (still in its packet) | Found in the world. Not craftable | Placeable like a player head, faces whoever placed it, own 3D model | Done: a red printed display box with a clear window, the orange-red toy supercar on a stand inside; only obtainable from Frank |
| Salt | Salt | Salt | Smelt a water bucket; the furnace keeps the empty bucket (needs custom code; vanilla would destroy it) | Straw ammo | Recipe done (furnace only; bucket stays in the input slot) |
| Toy Gun | An old western 30-30 lever-action rifle in bright plastic colors | — | Dropped by Sandmen killed by a player (15%, unloaded and worn); found in the world, rare (in mod structures, once they exist). **Not craftable** | A toy rifle holding one Dust Ball. **R** (rebindable: Controls → Reload Toy Gun) reloads it: 3 seconds, then a Dust Ball from the inventory goes in (switching items cancels the reload; the ball is only used once it's in). **Right-click** looks down its scope (zoomed in, round scope overlay); right-click again to stop (reloading, firing or switching items also ends it). **Left-click** fires, from the hip or scoped: near-hitscan speed (15.0, vs. crossbow arrow 3.15); 16 damage (8 hearts) per hit; the Dust Ball bursts and can't be picked up. Fired from the hip it's wildly inaccurate (about 13° of spread); through the scope it hits exactly where the crosshair is. A shot has a hard recoil: it knocks the shooter off the scope, pushes them back 0.25 blocks, kicks their aim up about 14° (and a few degrees to one side) and shakes their view hard. Holding it, left-click never swings or breaks blocks, and right-click doesn't use or place anything. 150 durability, 1 per shot | Done (checked in game; recoil and hip-fire spread retuned since). First person: at the hip when empty, raised and pointing forward once loaded; the reload rolls the gun over, shoves the Dust Ball in twice and works the lever; firing kicks it up and back with a muzzle flash (flames, sparks, smoke, a puff of dust) and a placeholder cap-gun pop; hidden while scoped. Other players see it too: shouldered with both hands, the reload's shoves and lever, the recoil |
| Straw | Straw | Straw | Shaped: 3 bamboo in a vertical line | Works like a crossbow: hold to load (0.5s), stays loaded, use again to fire. Unbreakable, not stackable. Fires Salt at twice snowball speed (3.0, just under a crossbow arrow's 3.15) with arrow drop: no damage, knockback only, can't be picked up. An early defense item; **Salt only** | Done. A spitball shooter: loading brings it up in front of the chest and the free hand comes across to drop a pinch of salt in (first person); it only goes up to the mouth once it's really loaded (when the button is let go, as with a crossbow), then runs from the mouth to the crosshair so its far end is the sight; firing makes a llama-spit sound with spit and salt grains out of the end, a little puff, then it's lowered. Other players see it too: in third person the arm raises the straw to the chest with the free arm crossing over, then to the mouth |
| Holy Text | Golden book | Any holy text | Shaped: Book surrounded by 8 Gold Ingots | In off hand: Luck I + Strength I. Not stackable | Done |
| Her Favorite Dress | A faded pink dress on a hanger, stained (first-version texture) | **Open** | Trade from Gustav (5 Dust Bricks) | For a quest to do with Gustav (to be designed) | Item and texture done; the quest isn't made yet |
| Scalpel | Scalpel | Scalpel | Shaped: Iron Ingot and Stick diagonal (either direction) | Tool, not stackable, 238 durability (same as shears). In its crafting recipes it stays in the grid and loses 1 durability per craft; breaks at 0. Weapon: 1 damage, 4 attack speed | Done |
| Sweet Berry Bush | Vanilla bush texture | The bush itself | Break a sweet berry bush with any shears (including modded shears) | Smelts into Dust; plants a fully grown bush (with berries), since it's the whole plant. Shapeless: Sweet Berry Bush + Scalpel → Pile of Berries | Done |
| Pile of Berries | A heap of red berries | Berries cut from a bush | Shapeless: Sweet Berry Bush + Scalpel (the Scalpel loses 1 durability) | Crafts into 2 Red Dye (shapeless, on its own) | Done |
| Grape Hugs | Styrofoam cup with purple inside | Lean | Found in the world. Not craftable | Drinkable (drinking animation and sound), restores 2 hunger bars (4 points), 0.8 saturation. Contraband: Sandmen hunt anyone carrying it | Done; not obtainable in survival until structures/loot exist |
| Aspiraton | Hardened blood shard | Blood. The name is an **intentional misspelling**, a play on "aspiration" (drawing fluid from a body); never "correct" it | Mining an Aspiraton Block (2–4, up to 9 with Fortune); 1 Aspiraton Block → 9 | 9 → Aspiraton Block | Done |
| Aspiraton Drop | Shaped like lime dye, deep dried-blood red | Dried blood | Mining Veinseep / Deepslate Veinseep (2–4, glowstone-style) | A quest item: given to the unnamed man (a Magic Block quest) | Item and texture done; the quest isn't made yet |
| Chisel | — | — | **Open** recipe | Right-click stone / deepslate / end stone → brick version | Works; not craftable until it has a recipe |
| Tactical Vest | A black plate-carrier bulletproof vest with mag pouches and a little blue screen (first-version textures) | The Sandman's kit | Dropped by Sandmen killed by a player: 25% (Looting raises it) | Chestplate with diamond's protection and toughness. While worn: half damage from Skinwalkers' hits and from Sandmen's shots, and four pockets of extra storage (described in its tooltip while Shift is held), opened with **B** (rebindable: Controls → Open Tactical Vest); what's in them stays in the vest wherever it goes (no vests, bundles or shulker boxes in the pockets). Not stackable. Will be used in a quest | Done (to check in game) |
| Tactical Boots | Black laced combat boots with heavy soles (first-version textures) | The Sandman's kit | Dropped by Sandmen killed by a player: 12% (Looting raises it) | Boots with diamond's protection and toughness. While worn: Tar doesn't slow you (you walk through it as on dry ground). Not stackable. Will be used in a quest | Done (to check in game) |


Item textures marked "first version" in TEXTURES.md were drawn by `art/tools/item_textures.py` and can be painted over.

## Blocks

| Block | Function | Status |
|---|---|---|
| Veinseep / Deepslate Veinseep | Drops like glowstone: 2–4 Aspiraton Drop, Fortune adds more but caps at 4, Silk Touch drops the block. Surrounds the blood veins (see World generation) | Done; generates like diamond ore |
| Aspiraton Block | Crafted from 9 Aspiraton (3×3). Drops 2–4 Aspiraton; Fortune I 3–6, Fortune II 5–8, Fortune III always 9; Silk Touch drops the block | Done |
| Mytoma | Very dark red hardened Nutrinon. Forms wherever Nutrinon touches water (like lava + water making obsidian). Hardness 6 (between deepslate and obsidian), needs a stone pickaxe or better, drops itself | Done; placeholder texture |
| Dust Bricks | Building block; 2×2 Dust Ball; 1 → 4 Dust Ball | Done |
| Avolition | Pure black block for void-like rooms and cutscene spaces. Not meant to be mined (very high strength); drops itself if someone does | Done |
| Nutrinon | A bright red **liquid**, the blood in the veins. Flows like water and can be swum in (and drowned in), but swimming is slower than water and faster than lava. Two sources never make a new one, so it can't be farmed endlessly. Flowing Nutrinon sheds dark red droplets; still Nutrinon has no particles. Picked up and placed with a Nutrinon Bucket | Done; own animated textures (first version) |
| Tar | A **liquid** that behaves exactly like lava (flow, spread, movement, fog, bucket) but gives no light and never burns or damages. Picked up and placed with a Tar Bucket | Done; own animated textures (first version). Found in tar pits (see World generation); some structures later |
| Magic Block | Quest turn-in spot (see Magic Block quests) | Exists; quest system not started |
| Speedy Car | Placeable display block (see Items) | Done |

### Vanilla change

- Breaking a **sweet berry bush with shears** drops the Sweet Berry Bush item, plus at most 1 sweet berry (vanilla would drop up to 3), so replanting and shearing the bush can't farm berries quickly. Done with a Forge global loot modifier (not a loot table override), so it stacks with other mods' changes.

## Magic Block quests

The unnamed man gives the player quests to bring him items. The player chooses which items to hand over and which to keep, and hands them over by tossing them onto the Magic Block, where he takes them.

**Open:** what the quests are, what rewards they give, and whether the player's choices feed into the three endings.

The current code turns dropped Pelt into Graft on the Magic Block. Treat this as placeholder behavior from before this design.

## Mobs

| Mob | Concept | Behavior | Drops | Status |
|---|---|---|---|---|
| Skinwalker | Based on **The Rake**: a dog that stole a human's skin and is still learning to be human. A gaunt human head and skin on a dog's frame: deep narrow chest, tucked waist, long thin arms that double as front legs, and **dog hind legs** (knee forward, hock back, up on its toes) even when standing upright. 4.5 blocks tall standing. There will be several variations of the Skinwalker; the first is the bald one: a bald, rounded, pale white human head with a skull for a face (huge eye sockets with eyes that glow in the dark, a flat nose that is just a hole, bare upper teeth) and skin only on its jaw. That skin doesn't work for it, so it wants to take yours and see if it fits. **On all fours** (from the user's annotated photo): arms straight down under the shoulders; torso and chest rising in one line from the hip to the neck, head on top; the femur running from the hip (the low point of a 'V') up and back to the knee; the long tibia dropping straight down from the knee. About 2.5 blocks tall in game. When moving on all fours its limbs spread out for balance. On all fours it fits 3-block-high spaces; for lower or narrower gaps it stoops, creeps or crawls (see Getting to its prey). Its hitbox matches its form: 0.9 wide and 4.5 tall standing, 2.4 tall on all fours, 0.9 tall crawling on its belly, 0.9 wide and 1.9 tall creeping, 0.9 wide and 2.9 tall stooping, 1 tall asleep. **On its belly** (from the Cave Dweller mod): body flat on the ground, dragging itself along by its arms, hand over hand, the hind legs trailing limp and straight behind, so it fits 1-block-high gaps and 1x1 holes (nothing sticks out to the sides). **Creeping** (1-wide, 2-high gaps): low on all fours, back level just under the ceiling, elbows and knees tucked in under it. **Stooping** (1-wide, 3-high gaps): up on its hind legs but folded over, knees deeply bent, back level just under the ceiling, the long arms hanging with the knuckles brushing the floor | 70% hunt players on sight; the rest ignore them until hurt. 18 health; normal attacks do only 25% damage (six critical hits of a netherite sword kill it), so players should run. Hits for a flat 8 (4 hearts) whatever armor (or none) the target wears, on any difficulty; a hit on a raised shield knocks it down for 5 seconds like an axe. Iron golems attack it, and it kills one in three hits (each of the first two takes a third of the golem's health, the third finishes it, however much it has healed). Chases at ~6.4 blocks/s: faster than a sprinting player on flat ground, just under a sprint-jumping one. When scared (hit by a player and rolls "flee", or 3 Salt hits) it runs at hyperspeed (~26 blocks/s) away from that player for 15 seconds. The first time a player hurts it: screams, then flees or fights (50/50); later hits only make it flinch and turn on whoever hit it, so repeated hits don't keep it screaming. 3 Salt hits: screams and flees. 2nd Dust Ball hit: dies instantly. A first Dust Ball hit has a 10% chance to enrage it instead of the flee/fight roll: it freezes in place staring at the shooter and screams for 1.5 seconds (it can still be shot meanwhile), settles into its run for half a second, then charges them at ~16 blocks/s for 5 seconds. Too tall for doorways. Spawns rarely in the dark, only at night and only in dense forests (dark forest, forest, birch and old-growth birch forest, taiga, snowy taiga, old-growth pine and spruce taiga: the `horrormod:skinwalker_spawns_in` biome tag), never in open biomes, the Nether or the End, always alone; spawn eggs work any time | 1–3 Pelt (+Looting) | Done as its own mob (it does not replace or change Endermen). In game it uses its own model, texture and animations (GeckoLib), with glowing eyes (drawn as added light, like Enderman eyes, with a faint glow on the socket walls); the sounds are still placeholder enderman sounds. It spawns standing or on all fours (50/50) and does everything in that form: hunting, fighting and running. Calm, it ambles: slowly (about 2 blocks a second) and only now and then (on average every 20 seconds); after 2 minutes it changes to its other form, and 5 minutes after that (7 minutes in all) it lies down and sleeps (on all fours it first walks a small circle like a dog); the timer starts over once it wakes. It hears players walking or running within 30 blocks (crouching players only within 10) and blocks being placed or broken within 30 blocks (even by a crouching player), asleep or awake. Woken by a sound, it gets up into either form at random (hit in its sleep, it gets up in the form it slept in; it only stands up where there's headroom); then (or straight away if it was awake) it goes on alert and looks around for the sound. It has to see a player to get angry, keeping them in sight for a full 1.5 seconds: a hunter (70%) then goes after them, the rest calm down. While it chases a player it screams every 3 to 7 seconds (placeholder enderman scream). If it gets stuck anyway (on a tree, a gap it doesn't fit, or can't find a way through) for 0.8 seconds, it gets lower: standing it drops to all fours, on all fours (or stooping) it creeps, creeping it gets down on its belly and crawls, like the Cave Dweller. It gets back onto all fours once there's been room for 3 seconds, or straight away when the one it's after is within 4 blocks with room around them (if they're hiding in a low tunnel, it stays down and goes in after them). To get at something above it (out of a pit it's trapped in, up a pillar, up a 1x1 shaft or through a hole in a ceiling) it climbs the walls like a spider. For testing, `/skinwalker sleep` (operators) makes every loaded Skinwalker lie down and sleep after 5 more seconds of whatever it was doing. If it can't find what it heard within 5 seconds, it runs off from the sound for a few seconds, then calms down. If it's still around when the sun comes up, it drops whatever it's doing (waking up first if asleep), runs off away from the nearest player at its normal run, and disappears once no player can see it and it's at least 24 blocks from everyone (or more than 64 blocks away). It walks, runs and flees in its current form with the animation speed matched to how fast it moves; punches standing, slaps or bites on all fours; screams, flinches, winds up and dies with the matching animations (it holds still while it screams, flinches or gets down onto all fours or back up, so it never slides along mid-animation; the body stays until its death animation is over); swims in deep water (deeper than about half its height, at most 1.2 blocks; shallower water it wades through), and never lies down to sleep in water (`/skinwalker sleep` waits until it's out; water reaching a sleeping one wakes it); its eyes dim while it sleeps; and while idle now and then sniffs, tilts its head or tugs at its skin. Animations blend into each other over 4 ticks. **Getting to its prey:** when there's no way to whoever it's after at its size, it looks for one in its low forms, the tallest that gets there: stooping (a 1-wide, 3-high tunnel), creeping (2-high) or on its belly (a 1x1 hole). It goes to where the way gets too small, gets down into that form there and goes through; inside, it rises to the tallest form there's room for (from its belly to creeping in a 2-high stretch, to stooping in a 3-high one) and back onto all fours once it's out. Stooping it moves at about 2 blocks a second, creeping about 1.6, on its belly about 0.6. It never climbs in a low form, and its one-off animations (screams, flinches, attacks) don't play in them. With no way in at all for 15 seconds it gives up on them and goes back to roaming, leaving them alone for 30 seconds unless they hurt it. **Holy places** (the `horrormod:holy_places` structure tag, their grounds included): it never goes into one. It won't wander or follow a sound in, and a player who runs into one is safe: it stops chasing them, and leaves them be for 30 seconds and for as long as they stay inside. It never spawns inside one. **Knocking:** roaming, if it comes across a closed door within 5 blocks it walks up to it, kneels (on all fours it lifts a front hand) and knocks three times at about door-handle height, then stays down a few seconds with its head cocked, listening, and speaks through the door (`knock_standing` / `knock_on_fours`, 5.4 s; placeholder sounds: wood taps for the knocks, a deep slowed villager voice for the speech, to be replaced with a "Hello? Are you in there?" recording; both are in `assets/horrormod/sounds.json`). Then it waits by the door for 10 to 20 seconds, staring at it; footsteps it hears while knocking or waiting don't send it searching or running off. At most one knock a minute and not the same door again for 5 minutes. Made but not used yet (no behaviour for them): leaping, climbing over ledges. Its death messages are picked at random per hit: "X was torn apart by Skinwalker", "I wonder if X's skin will fit this time...", "X's skin doesn't fit WHY DOESN'T IT FIT!" and "X was ripped apart by" a glitching "Skinwalker". **Open:** spawn rarity |
| Hollow | Someone who chose the wrong skin too many times: basically a Skinwalker, but in skin gone rotten (like the Rotted Pelt). From the user's reference picture: a tall, gaunt corpse of a man (about 2 blocks) in what's left of his clothes. A sunken, skull-like face with empty black eye sockets under a heavy brow, bare cheekbones, yellowed teeth with gaps, the left cheek rotted through to the jawbone and back teeth; stringy hair hanging down the back of the head and over the brow. Sickly grey-green skin, blotched with rot, bruised, veined and dotted with raw sores. An olive field jacket hanging open, frayed and holed, its hem in rags, over a stained red shirt torn across the ribs; khaki trousers yellowed with grime and mud, the right leg torn open at the knee and short over a bony shin with the bone showing, the left ragged over the boot; heavy cracked leather work boots; long thin arms, the right sleeve ripped away from a forearm rotted to the bone, ending in big bony hands clenched into fists that hang to the knees. Its jaw is hinged under the ears inside the cheeks, so when it opens only the chin and lower teeth drop, showing a dark mouth with a tongue. Walks slowly, trudging around holy places like the Mecca Black Box (once structures exist). Its stance: hunched forward, hips cocked, the head lolling to one side, the jaw hanging open, the long arms hanging limp in front of it by their own weight (the rotted right one twisted in), swinging loose behind its movements, the knees never straight | Harmless: trudges slowly about (setting off about every 8 seconds, at the walk animation's own pace) and pays players no attention at all, never attacking or following. 30 health (15 hearts). Takes damage but no knockback, and doesn't react to it at all (no flinch, no hurt sound, no running), except to salt: hit with salt from the Straw it screams and curls up into a ball on the ground (on its side, knees to its chest, its fists over its head, trembling), not moving, until 45 seconds after the last hit on it (a hit while it's down starts the 45 seconds over); then it slowly gets back up (3 s: onto its knees, then to its feet). While down it stays put: it doesn't turn or get pushed. Killed while curled up, it falls apart where it lies (`death_curled`: the head comes off and rolls away, then the forearms, the upper arms, the torso rolling onto its front, the jaw; the legs go slack). Killed by a Dust Ball (the Toy Gun's, or a Sandman's shot) it shakes violently for a second before falling apart. Now and then, standing, it twitches. Sandmen hunt it (`horrormod:sandman_hunts`). Placeholder sounds: a slowed zombie groan now and then, and a skeleton/zombie death for falling apart. **Spawning:** none yet, only its spawn egg; it will come with the holy places it haunts (the Mecca Black Box) | 1–3 Rotted Pelt (+Looting) | Done (to check in game): entity, spawn egg. Model, texture and animations done (`art/tools/hollow_model.py`; dark colours, the skin a dim dead green): `idle` (feet planted flat, swaying in its hunch, shallow breaths, the head lolling, the jaw working, the arms and fists swinging limply after the body), `walk` (the trudge, 1.6 s a cycle: heavy steps on the good left leg, the stiff right leg dragged after it with its toe scraping the ground, the body dropping onto the right leg each time it takes the weight; each foot on the ground stays flat and planted, moving back at the speed the body goes forward, about 0.028 blocks a tick, so in game the animation's speed is scaled to its real speed; the arms swinging loose and late), `twitch` (a spasm now and then while idle: the head snaps aside twice, the jaw clacks, the arms are flung and swing loose until they settle) and `death` (2.2 s, one quick, smooth fall in which it breaks apart where the ground hits it: the knees buckle and it drops onto them, then it tips forward from the knees, faster and faster, its arms hanging; as each fist hits the ground that forearm snaps off at the elbow and flops flat, then each upper arm, pressed into the ground, breaks off at the shoulder; it lands on its face and the head snaps off and rolls away, the jaw coming loose when it stops; the torso comes down, breaks away at the waist and settles face down; the legs straighten out flat on the ground behind it, the boots pointed. Every piece lands on the ground and stays on it). `death_dust` (the same, after a second of violent shaking: the head jerking, the jaw chattering, the arms flailing), `curl` (dropping into a ball, 0.7 s), `curled` (trembling, looped) and `uncurl` (3 s). No attack animation, and no hurt animation |
| Odie | **The Hat Man**, the sleep paralysis figure: a tall, thin man in a long dark overcoat, a pale striped shirt and dark tie, his face lost in shadow under a wide-brimmed hat (after the user's reference photo). A singular entity: only ever one in the world | Never attacks and can't be hurt or touched. He comes for a player carrying a lot of Dust (180 counted as Dust: a Dust Ball is 4, a Dust Brick 16), rarely (a 1-in-8 chance every 5 seconds once everything fits) and at most once every 10 minutes per player, in the morning, evening or at night but never around midday (10 am-2 pm). He stands on high ground: a treeless hilltop or ridge at y 100 or higher, 45-95 blocks from the player, in sight of them (every spot in that ring is searched, so if there's anywhere he can stand, he's found), watching them (resting animation: slow breaths, faint sway). When he appears, the player thinks to themself in chat, as if they'd said it (only they see it): "I...I feel watched." **Looked at** (the player's crosshair on him or within about 7° of him for 2 seconds in a row, nothing in the way): he takes off his hat, holds it to his chest, waves slowly and puts the hat back on, then turns round (a smooth half turn over 1.4 seconds, his head leading) and walks straight away from the player at about 2 blocks a second: gone once the player can't see him, or, if he's still in view after 15 blocks, he dissolves into the air. **Chased** while leaving (the player closes in 6 blocks, or within 35): he darkens into a shadow, then crumbles into black dust on the air and dissipates (4 seconds). **Within 30 blocks**: he's gone instantly and the player's screen goes black for 0.5 seconds with "Careful." in white in the middle. After 5 minutes unseen, or at midday, he leaves on his own. Never saved with the world. For testing, `/odie appear` (operators) brings him straight away (still needs a summit) | None | Done (checked in game: he appears, waves and leaves as designed). Own model, texture and animations (`idle`, `wave`, `turn_around`, `walk_away`; GeckoLib), made by `art/tools/odie_model.py`, which also writes `art/odie.bbmodel` to view him in Blockbench. His model also stands on the title screen, tipping his hat. **Open:** whether he stays a 3D model or becomes a flat image |
| Sandman | A police officer / DEA agent: the drug squad. A ghost in Haze's kit (from Deadlock): a head of cold fire in the shape of one flame swept back by the wind, pale blue at its base burning to black, with dark veins through it, two glowing eyes and two flame horns; black top half (shirt, plate-carrier vest), deep blue trousers, combat boots; solid, her edges glowing faint blue. She blinks every 15-25 seconds, and her eyes narrow when she goes after someone, focused on them. A player's height. Armed with a Toy Gun. Merciless: drug users are to be killed, whatever their situation; anyone holding any amount at all is to be arrested | Neutral until provoked or until it finds contraband. Chases and attacks any player carrying anything in the `horrormod:contraband` item tag (Dust, Dust Ball, Dust Bricks, Crank, Heroine, Speedy Car, Grape Hugs), a loaded Toy Gun, or any of these in a Tactical Vest's pockets; however little, it's enough (Dust Balls kept as Toy Gun ammo count). A player's **record** counts their offences in points: every trade with Gustav, Walter or Frank adds 1, attacking a Sandman 2 (once per fight, not per hit), being caught with contraband 5, killing a Sandman 10. Killing a Skinwalker (doing the Sandmen's work) takes 5 off (never below 0). **Later**, with the arrest: getting out of the Torture dimension adds 10, and dying in there wipes the record clean (see Dimensions). With any record at all, every Sandman hunts them from then on, whether they still carry anything or not, even after they die; any bad action ever is met with punishment, and turning their life around changes nothing. The bigger the record, the harder they're sought: from 10 points, Sandmen are sent after them anywhere out in the open world (on open ground 20-36 blocks away, coming straight for them), more often the bigger it gets: at 10 points about every 7 minutes, rising to about every minute at 50 points; one at a time, two at once from 50 points. Creative and spectator players are ignored. Kills on sight whatever is in the `horrormod:sandman_hunts` entity tag: Skinwalkers (hurt by her, a Skinwalker screams, then flees or fights her, 50/50, in its form; two shots kill it, as two Dust Balls do; awake and not fighting one, a Skinwalker that sees a Sandman within 24 blocks runs from her with its flee run, checking where she is every half second, and keeps running while she's that close), pillagers, and Hollows once they're added to the tag (they're drug users too). 18 hearts (three critical hits of a netherite sword). Every 10-14 seconds it stops, raises its Toy Gun and fires: a skeleton's aim but a much faster shot (3.5 vs an arrow's 1.6). A shot does a set amount by the player's armor, which doesn't otherwise soften it: none 5 hearts, leather or chainmail 4.5, iron or gold 4, diamond 3.5, netherite 3 (mixed sets in between by each piece's share), and knocks the player back unless their whole set is netherite. Anything shot starts **Bleeding** (30 seconds). Up close it strikes with the gun's stock (1 heart, every 2.5 seconds). On patrol she walks slowly and sets off only now and then (about every 20 seconds). Around holy places (the `horrormod:holy_places` structure tag: in their grounds or within 24 blocks of them) she keeps the peace: she starts nothing there and drops any fight with a mob; a player she was already chasing she follows in, and a player who hurts her there she goes after. On patrol she doesn't head into them, though she'll cross their edges (she doesn't keep away the way a Skinwalker does). She never spawns inside one. Opens wooden doors; a closed iron door in its way (between it and its target) it shoots apart (the door drops). Spawns any time of day in villages (on the ground among the villagers' homes, at most 2) and sometimes in the grounds of pillager outposts and woodland mansions, outside their buildings (the `horrormod:sandman_watches` structure tag, at most 1); despawns like other mobs once no one is near. Dies by collapsing onto its front and fading away in blue flame. Placeholder sounds (the Toy Gun's pop for its shot). **Later:** being 'killed' by a Sandman becomes an arrest (see Dimensions) | Tactical Vest (25%), Tactical Boots (12%), its Toy Gun (15%: unloaded and worn, with 15-60% of its durability left); Looting raises all three | Done (to check in game): model, texture and animations made by `art/tools/sandman_model.py` (her rifle is part of the model, held like a SWAT officer: at low ready it lies diagonally across her body, the butt at her right chest, the right hand on the grip and the left low on the forend; idle at low ready, walk, run, aim, shoot, strike, hurt, death, and the flame's flicker), entity, spawning, combat, drops |
| Angel | A 'biblically accurate' angel, after the user's drawing but with only one eye: a great pale stone ring, ridged and grooved, hanging in the air with one huge almond eye in its middle (a white with faint red veins, a dark red iris, a slit pupil, pale lids round it), and three long feathered wings on each side (the upper pair raised, the middle level, the lower pair lowered, layered one behind another; grey-white at the root darkening to near black at the long flight-feather tips). Inside the ring, hidden until it opens, a white disc: the portal to its dimension. About 1.5 blocks across the ring, 5-6 from wingtip to wingtip | Always turns to look at the nearest player (tipping its ring up or down to look at them while it flies). It hangs high in the air, 30-50 blocks above the ground (each Angel its own height), and always higher than everything within 30 blocks to its sides (at least 4 above the highest ground around), so it can't be jumped onto from a mountain. Nothing hurts it. Drawn in its own light, as bright at night as by day. A player building up toward it (placing blocks within 12 blocks of it across and no more than 16 below it) makes it fly off 20 blocks away from them; after it has moved away three times it flies off into the sky and disappears. Shot at with the Toy Gun or the Straw (hit by a Dust Ball or salt), it flees: it flies off away from whoever shot it, rising into the sky, and disappears. A Holy Text thrown on the ground brings it down (within 48 blocks of it across): it comes down onto the book and takes it (if someone picks the book up first, it goes back up), and there, where the book was, it widens its ring and shows the white portal, standing on the ground like a standing mirror with its wings folded behind it, and a player jumps into it to go to its dimension (the Child's Playroom). It stays open for 25 seconds, then closes and flies away | None | Done (to check in game): the mob and all of the above including the portal taking players into the Child's Playroom, spawn egg only (no natural spawning yet), placeholder sounds (phantom wing flaps, a beacon's activate and deactivate). Model, texture and animations (`art/tools/angel_model.py`): `hover` (the wings beating slowly in a wave from top to bottom, the whole of it rising and falling with them), `fly` (hard beats, leaning into it), `ascend` (fast, powerful beats, rising into the sky), `descend` (gliding down, wings spread and held up), `open` (the eye shrinks away, the ring widens 1.55x and the white portal spreads to fill it, the wings, staying fixed to the ring's rim as it widens, swing round behind it, the upper and middle pairs folding back and the lower pair angling down behind as a stand, and it settles onto the ground like a standing mirror; nothing goes below the ground), `opened` (the portal shimmering, the folded wings stirring), `close` (back again, lifting off the ground); and on their own controllers `blink` (the eye squeezing shut and open; resting state `eye_open`) and `spin` (the ring slowly turning). **Next:** its dimension (the Child's Playroom) and the portal taking players there; where and how it appears in the world |
| Best Warden (boss, planned) | The boss of the Torture dimension. Named as a play on Roy Best, the warden known as the most notorious in Colorado history: a strict disciplinarian who used painful, degrading punishments inside and outside the prison walls, among them the "Old Gray Mare", a wooden sawhorse inmates were bent over, tied down on and flogged with a leather strap (the device that later led to his removal), and who made gay prisoners wear dresses and push rocks. The body of a fat policeman, looking like Warden from Deadlock, under a large cowboy hat | Fought in his arena in the Torture dimension. Attack: he throws his cowboy hat, which bounces off the arena's walls. (Other attacks and his arena: to be designed.) | **Open** | Planned, not started |

All the planned mobs exist (the Skinwalker, Odie, the Sandman and the Hollow); Sandmen hunt Hollows (they're in the `horrormod:sandman_hunts` tag).

### Effects

| Effect | What it does | Cured by | Status |
|---|---|---|---|
| Bleeding | Half a heart every 4 seconds, whatever the armor. 30 seconds from a Sandman's shot (15 seconds on a Skinwalker) | A Cloth Strap (tied on as a tourniquet), milk, or anything else that clears effects | Done (first-version icon) |

### Weapon effects and animations

The Straw is a crossbow underneath (hold to load, use again to fire); the Toy Gun is its own weapon (R reloads, left-click fires). Both have been checked in game and tuned from testing; the latest changes (the Straw's loading, the Toy Gun's recoil and hip-fire spread) still need a look:
- **Straw (spitball):** first person, loading brings the straw up in front of the chest, upright, and the free hand (if empty) comes across and drops a pinch of salt into its top end; only once it's really loaded (vanilla loads it when the button is let go) does it go up to the mouth; loaded, it runs from the mouth (bottom middle of the screen) to just under the crosshair, so its far end is the sight; firing makes a llama-spit sound, spit and salt grains fly out of the end with a little puff, then it's lowered. Third person: the arm raises the straw to the chest while loading (the free arm crossing over to drop the salt in), and to the mouth once loaded.
- **Toy Gun:** R reloads (3 s), right-click looks down the scope (right-click again to stop), left-click fires. Hip fire is wildly inaccurate, scoped fire dead on. A shot knocks the shooter off the scope, pushes them back a quarter of a block, kicks their aim up about 14° and a little aside and shakes their view hard. First person: at the hip when empty, raised and pointing forward once loaded; the reload rolls the gun over to show the loading port, shoves the Dust Ball in twice and works the lever; firing kicks it up and back with a muzzle flash (flames, sparks, smoke, a puff of dust) and a placeholder cap-gun pop (a pitched-up firework sound); hidden while scoped (the scope overlay shows). Third person: shouldered with both hands, with the reload's shoves and lever and the recoil.
- Other players see the third-person animations, including the recoil and the straw being lowered after a shot.

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
- [x] Crawling on its belly (1-block gaps, 1x1 holes): body flat, dragging itself along by its arms (each hand reaches far ahead, in front of its own shoulder, plants and pulls while the other reaches), the hind legs trailing limp and straight behind, toes pointing back; nothing sticks out past its shoulders (`crawl_prone`, 0.6 s loop)
- [x] Creeping (1-wide, 2-high gaps): low on all fours with the back level just under 1.9 blocks, elbows pointing back and knees tucked under, the head low and forward, stepping in diagonal pairs (`creep`, 0.9 s loop; still: `rest_creep`, breathing)
- [x] Stooping (1-wide, 3-high gaps): on its hind legs folded over, knees deeply bent, back level just under 2.9 blocks, the head pushed forward and the arms hanging, knuckles near the floor, swinging against the legs (`stoop`, 1.1 s loop; still: `rest_stoop`)
- [x] Changing between any two of standing, all fours, stooping, creeping and on its belly (`<from>_to_<to>`: `stand`, `crawl`, `stoop`, `creep`, `prone`; 0.8 s, 0.9 s between standing and all fours)
- [x] All fours → belly (`crawl_to_prone`, 0.8 s) and belly → all fours (`prone_to_crawl`, 0.8 s)

**Movement**

Every movement has a standing and an on-all-fours version. Climbing (up, down) and swimming are their own body positions (flat against a wall / floating), so one version serves both stances.

All movement animations are done.

**Idle and state**

It sleeps in the stance it was in when it chose to sleep. When it wakes, the game picks the stance it ends up in at random (not based on how it slept), so there's a wake-up for each sleeping pose into each stance. The dog-style circling before lying down is movement done in game code (the AI walks it around a small circle while the walk animation plays, so the hitbox follows it), not baked into the animation.

All idle and state animations are done.

**Combat and reactions**

In Blockbench the animations are sorted into folders: Idle, Movement, Sleep, Combat, Reactions, Death, Character. Fingers clench into fists for the punch and spread into claws for the scream, wind-up and slap.
- [x] Scream, standing: draws a breath, then leans in with the face forward and slightly up, the jaw dropping wide open (it slides down and forward like a real jaw, clear of the neck), arms flung out with the claws spread, trembling with the scream; then settles (`scream_standing`, 2.1 s)
- [x] Scream, on all fours: the head thrust forward with the face level and the jaw wide open, claws spread, trembling (`scream_on_fours`, 2.1 s)
- [x] Enraged wind-up, standing: the 1.5-second freeze before the Toy Gun charge: the arms go up overhead, then sweep down into claws reaching toward the target as it screams, dropping into a tense crouch, shaking harder as it builds; then the scream ends, the jaw closes and it settles into the first frame of `run_standing`, ready to chase (`enraged_windup_standing`, 2 s: the 1.5 s freeze plus 0.5 s settling into the run)
- [x] Enraged wind-up, on all fours: head forward and level, jaw wide open, hind legs gathered under it ready to spring, shaking harder as it builds; then settles into the first frame of `run_on_fours` (`enraged_windup_on_fours`, 2 s)
- [x] Punch (attack while standing): the right fist clenches and cocks back by the chin as the hips and shoulders turn away, then the body pivots through and the arm drives straight out; the left arm swings back as follow-through; the feet stay planted (`punch`, 0.85 s)
- [x] Slap (attack while crawling): the right arm comes up high with the elbow bent, the hand up by the head, then slaps down and across at chest height (`slap_on_fours`, 0.6 s)
- [x] Bite (attack while crawling): gathers, leaps forward with the head level and the jaw wide open, snaps it shut as it lands, settles back (`bite_on_fours`, 0.6 s). Slap and bite are both made so the game can pick between them
- [x] Hurt flinch, standing: the whole body recoils, knocked back a step with the knees dipping, chest and head thrown back, arms jerking up; recovers (`hurt_standing`, 0.45 s)
- [x] Hurt flinch, on all fours: shoved back, the body twisting and the front legs buckling, the head jerked aside (`hurt_on_fours`, 0.45 s)
- [x] Death, standing (sword, bow or any other normal attack): reels back, the knees buckle, falls forward onto its face (`death_standing`, 2 s)
- [x] Death, on all fours: the hind legs give out, the chest goes down, it rolls onto its side (`death_on_fours`, 1.8 s)
- [x] Dust Ball death, standing: shakes violently the whole time while it slowly collapses (a hunched crouch, then its knees, then onto its side), keeps shaking on the ground for about a second and a half, then goes still (`dust_death_standing`, 5.6 s)
- [x] Dust Ball death, on all fours: shaking the whole time, the hind legs give out, it sinks onto its belly and rolls onto its side, shakes on the ground for about a second and a half, then goes still (`dust_death_on_fours`, 5.5 s)

Which death plays depends on what killed it (Dust Ball or anything else) and which stance it was in.

**Character**

The bald variation has no loose skin to adjust, so "adjusting the skin" works on its own skin (the jaw, the only place skin is left on its head, and its forearm), plus a dog shake.
- [x] Sniffing, standing: a hunched stoop with the head low and pushed forward, taking quick little sniffs, turning its head to follow the scent and back (`sniff_standing`, 2.4 s loop)
- [x] Sniffing, on all fours: the head down near the ground, sweeping slowly side to side with quick sniffs, like a dog tracking a scent (`sniff_on_fours`, 2.8 s loop)
- [x] Head tilt, standing: the slow curious dog tilt to one side, a little twitch, a snap to the other side, back (`head_tilt_standing`, 2.6 s)
- [x] Head tilt, on all fours: the same tilt with the head raised (`head_tilt_on_fours`, 2.6 s)
- [x] Adjusting the skin, standing: pinches the skin of its jaw and tugs it twice (the head leaning into its hand), pinches and pulls at the skin of its other forearm, then rolls its shoulders as if settling into the skin (`adjust_skin_standing`, 3.1 s)
- [x] Adjusting the skin, on all fours: lifts a front hand to tug at its jaw, puts it back down, then shakes its whole body like a wet dog, as if the skin had slipped (`adjust_skin_on_fours`, 2.9 s)
- [x] Knocking on a door, standing: kneels (the hocks to the ground, the chest bent forward) so its fist is at door-handle height, raps three times with its knuckles, then stays kneeling with its head cocked, listening, for a few seconds and gets up (`knock_standing`, 5.4 s; the knocks land at 1.1, 1.35 and 1.6 s)
- [x] Knocking on a door, on all fours: a front hand comes up off the ground and raps three times at door-handle height, then the same long listening tilt (`knock_on_fours`, 5.4 s)

## Traders

Gustav, Walter and Frank are **their own trader mobs** (like the wandering trader), not villager professions, so they can have completely different models. Right-click to trade. All trades are available from the start, with no levels. Each trade can be used 12 times, then restocks once per Minecraft day. They never despawn. Each trade gives a few XP orbs.

Every trade adds a point to the player's Sandman record (see the Sandman).

**Planned (not built):** talking to the traders. Right-clicking one gives a choice, **Trade** or **Talk**. Talk opens a conversation: dialogue (to be written) through which the player can learn about the world.

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
- **Mecca Black Box:** a holy site where people search for skin. Higher Skinwalker spawn rate. A holy place: put it in the `horrormod:holy_places` structure tag (Sandmen keep the peace around holy places, Skinwalkers won't go in, Hollows haunt them).
- **Bus:** school bus and city transit variants. Inside are many dead Skinwalkers.

## Dimensions (later)

- **Torture dimension** (name to be decided): white padded walls; where the Sandmen take you. Its boss is the Best Warden (see Mobs), fought in his arena. **The arrest:** if a Sandman 'kills' you, you don't die; you're sent here. You arrive in a cell with none of your items: they aren't lost, they're held, and you get them all back when you leave. There are three ways out:
  - **Serve your time:** stay in the dimension for 20 minutes.
  - **Escape:** find the exit room. Making it out adds 10 points to your Sandman record.
  - **Die:** you go back to your respawn point, and your items are dropped where the Sandman 'killed' you, as with a normal death. Dying in there cleanses you: your Sandman record is wiped clean (the only way to clear it).

  The dimension is a maze of halls and rooms with dead ends, full of Sandmen walking set patrol routes. You start by getting out of your cell, and then there are three ways to try for the exit:
  - **Fight your way out with your fists.** It goes badly, since Sandmen take nearly any hit and are nearly impossible to kill bare-handed.
  - **Run for it.** It probably fails in the maze.
  - **Sneak.** Move slowly through the halls, use hiding spots and crawl through air vents.

  The order the rooms are laid out in and the routes the Sandmen patrol change every time you're sent there. (Not built yet: only planned.)
- **Child's Playroom:** a return to innocent childhood. Reached through the Angel: brought down by a thrown Holy Text, it opens its ring onto a white portal and stays still, and a player jumps in.
  - **The place:** one large boxed-in room made to look like the outdoors: grassy ground rolling in low hills (up to ~4 blocks; kept flat along the back for the house, at the arrival spot and by the walls), a dirt path winding from the arrival spot to the house, patches of grass and flowers; the walls painted with trees against the sky (grass along the bottom, trees of all heights and sizes with smaller ones behind, sky showing between them, clouds high up) and a painted sun high on the back wall, to the right above the house, which lights every painted tree: each crown is light where it faces the sun and dark where it faces away, the ceiling painted as Minecraft's sky with its blocky clouds. Lit like a sunny day, evenly, with no shadows in the yard; the house's inside is dark. A Victorian dollhouse (from the user's picture: pink and cream, a round turret with a pointed shingled roof on the left, gabled roofs with fretwork trim, two-storey porches with railings, bay windows, a white picket fence in front) stands against its back wall. The yard in front is littered with slides, monkey bars, toys and the like. The inside of the house is nothing like the outside: old and broken, like a haunted house.
  - **Ways out:** a portal in the house's basement that looks like it's infecting the inside of the house, or a slide in the outside wall: jumping into either sends you back to the Overworld (to where you went in).
  - **The Heroine doll** is in chests in the house: a quest item you have to go and get.
  - **Cutscene (later):** a mother giving the child the Heroine doll, saying it will protect you from all you wish it to.
  - **Rules:** players in survival can't break anything in it.
  - **Built so far:** the dimension (`horrormod:childs_playroom`: empty apart from the room, always midday), the Angel's portal taking a player in (arriving by the front wall, facing the back), the room itself (97x97 blocks, 64 high, painted as the outdoors; its painted-sky ceiling lets daylight through, so the yard is lit evenly by the sky and anything roofed over is dark; built the first time anyone goes in, and rebuilt over an older version), the Playroom Exit block (the infected-looking portal, animated; walking or falling into it sends you back where you came from) for both ways out, and **the dollhouse**: the user's own build (started from a code-built version of the photo, then reworked by hand in a Fabric world with Litematica and WorldEdit: textured walls, a shingled roof of purpur, tuff, granite and blackstone, vines, rot, dust piled in the rooms), saved as `data/horrormod/playroom/dollhouse.litematic` and placed as it is (blocks, chests and their contents; its saved bats left out). The Playroom Exit is put back in the basement's far wall, with solid stone behind it. A player who still somehow drops into the void under the room is sent back where they came from instead of dying. **The third floor** is to be a boss arena: the ladder up to it is unfinished, and three chests in the house (in the basement, downstairs and upstairs) each hold one ladder for the player to find and place to finish it. **The Heroine doll** will drop from that boss (not yet built); for now it's in no chest. **Still to build:** the playground in the yard, the slide into the wall; any changes the user wants to the house.
  - **Same in every world:** the room, its hills, path, plants, house and painted walls don't depend on the world's seed; everyone arrives at the same spot (by the front wall, facing the house). For testing, `/playroom enter` and `/playroom leave` (operators).
- **Avolition:** a completely black realm. **Open:** how it's reached.

## Cutscenes (later)

- **Sandman:** a Skinwalker finds skin, dust flies out of him, and the Sandman shows up just as he finds salvation. "Sorry, kid…I just can't go back." The Skinwalker tries to run and is ripped apart in front of the player. The Sandman then turns on the player as "associated."

## Title screen

- **Custom main menu screen:** done (checked in game) (`client/HorrorTitleScreen.java`, shown instead of the vanilla title screen). A near-black screen; the normal Minecraft logo with the splash text at the top left; the menu (Singleplayer, Multiplayer, Mods, Options, Quit) as smears of dried blood from the bottom left edge, each its own shape, running well past its label and wiped out to the right; the one under the pointer (or picked with the keyboard) flows: it brightens to a wet red, creeps a little further out, its drips run down and a sheen keeps gliding along it, then it settles back when the pointer leaves; two pale support-link boxes at the bottom right (Discord, Ko-fi: placeholders that say "Link coming soon" until the URLs are filled in, `DISCORD_URL`/`KOFI_URL`); and Odie (his own model) standing toward the right, as tall as the screen, tipping his hat to the player, dimly lit and fading into the dark at his feet, his face lost in shadow under the brim. GIVE ME BACK MY SKIN MARYL sits in the bottom middle, patchy like old ink, in a dried-blood red, in IM Fell English Italic slanted far over; below it, Forge's version lines and Mojang's credits link, small. (The very first title screen opens before Forge passes events on, so it's swapped out on the first tick.) Art made by `art/tools/title_screen_art.py`. **Open:** the Discord and Ko-fi links.
- **Menu backgrounds:** done (to check in game) (`client/HorrorScreens.java`). Every menu outside a world (Singleplayer, the multiplayer warning and server list, Options, Mods and the rest, other mods' menus too) has the title screen's art behind it instead of the vanilla panorama: the near-black and Odie, dimmed a little so the menu reads over him. Menus opened inside a world keep the blurred world behind them.
- **Loading and saving screens:** done (to check in game) (`client/HorrorScreens.java`). The screens while a world opens and closes (reading the world, preparing it, loading terrain, saving) and while going through a Nether or End portal have the title screen's look instead of the vanilla panorama: the same near-black, the mod's name in dried-blood red in the middle, the message under it in pale text, and while the world is being prepared a thin line of blood filling up under it (in place of the vanilla chunk map).

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

1. **Mobs:** the Skinwalker, Odie, the Sandman and the Hollow are done (the Hollow spawns only from its egg until the holy places exist).
2. **Traders:** their own models and textures (Gustav, Walter, Frank), and how they appear in the world.
3. **Item functions** still open (see Still needed).
4. **The Angel** is done; its dimension, the **Child's Playroom**, is in progress (the dimension, the way in and out and the room exist; next the dollhouse and the playground). Later: structures (which also make the "found in the world" items obtainable), biomes, the Sandman's arrest and the Torture dimension, other dimensions, cutscenes, the set-path variant.

## Done so far

- **Items:** every item in the list exists with a texture and its recipe or source; the Straw and Toy Gun work, with their animations and effects. Pile of Berries and the sheared sweet berry bush changes added.
- **Blocks:** all blocks, both liquids (with their own animated textures) and the Speedy Car display box.
- **World generation:** Aspiraton blood veins, Veinseep patches, tar pits (swamp and mangrove swamp only).
- **Skinwalker:** model, texture, every animation, and its full behaviour (forms, sleep, hearing, sight, hunting, fleeing, finding its way through tunnels and holes by stooping, creeping or crawling, giving up when there's no way in, knocking on doors, climbing, leaving at sunrise, forest-only night spawning, flat 8 damage, shield breaking, iron golems, death messages).
- **Odie:** model, animations and his whole appearance (summits, being looked at, the wave, leaving, crumbling, the "Careful." flash).
- **Hollow:** model, texture, animations (idle, the trudging walk, twitch, falling apart on death) and the mob (harmless, slow, unmoved by damage, hunted by Sandmen, drops Rotted Pelt); spawn egg only for now.
- **Sandman:** model, animations and full behaviour (spawning, contraband, hunting Skinwalkers and pillagers, the gun, Bleeding), with the Tactical Vest and Tactical Boots it drops.
- **Traders:** Gustav, Walter and Frank trade, with placeholder villager looks.
- **Title screen** (Odie tipping his hat, the blood-smear menu) and splash texts.
- The mod runs on a dedicated server (tested with the built jar on a real Forge server).

## Still needed

**To check in game**
- The latest weapon changes: the Straw's loading (the free hand, rising to the mouth only once loaded), faster salt, the Toy Gun's recoil, hip-fire spread and scope toggle.
- The Skinwalker's new tunnel forms (stooping, creeping, the belly drag), giving up on blocked-off players, knocking (kneeling, waiting by the door) and its slower calm wandering.
- The new title screen (Odie, the blood buttons and their hover flow).
- The world loading and saving screens, the portal screens, and the art behind the other menus.
- Odie's "I...I feel watched." and how reliably he finds a summit.
- The Sandman's fix for her see-through texture (her glow is now drawn as light on top), her blinking and narrowed eyes, Skinwalkers running from her, the record points and being sought in the open world.
- The Hollow in game: its look (the cutout holes in its clothes), the walk keeping its feet planted, the twitch, falling apart on death, the shaking Dust Ball death, curling up from salt and getting back up.
- The Sandman: its look in game (the glow, the rifle in its hands at low ready and raised to aim), its animations, spawning in villages and at pillager outposts and mansions, chasing contraband carriers, shooting, Bleeding, the Cloth Strap tourniquet, the Tactical Vest's pockets (B) and protection, the Tactical Boots in Tar.
- The new item textures, the Speedy Car model, and the Nutrinon and Tar textures (and whether flowing liquid still shows a checkered texture).
- The title screen at every GUI scale.

**To build**
- **Hollow:** its natural spawning, around holy places (the Mecca Black Box) once structures exist; its own sounds.
- **Odie:** whether he stays a 3D model.
- **Magic Block quests** (the quests, their rewards, and whether they feed the endings).
- **Trader models and textures**, and how traders appear in the world.
- **Trader conversations:** the Trade or Talk choice, and the dialogue (info on the world).
- **Quests** for Her Favorite Dress (Gustav) and the Aspiraton Drop (given to the unnamed man).
- **Quests** that use the Tactical Vest and Tactical Boots.
- **Obtainable in survival:** Grape Hugs (no source yet); Heroine, Crank and Speedy Car (Frank only). Waits on structures/loot.
- **Skinwalker extras:** its own sounds (it uses enderman sounds now); behaviour for the leap and ledge-climbing animations; scream, flinch and attack animations in its low forms (stooping, creeping, on its belly); spawn rarity; other variations besides the bald one.
- **Sounds:** the Toy Gun's shot (placeholder pop), the Skinwalker's knock and its voice through the door (placeholders in `assets/horrormod/sounds.json`; the voice is meant to be a "Hello? Are you in there?" recording), ambient sound near tar pits.
- **Art:** Mytoma (placeholder texture); optional Straw loading frames.
- **Title screen:** the Discord and Ko-fi links.
- **Sandman extras:** its own sounds (the shot is the Toy Gun's placeholder pop); the arrest (being 'killed' by one sends you to the Torture dimension instead of dying).
- **Record screen** (idea, not to build yet): a way to see your Sandman record, e.g. a key that opens a screen showing it (how many points, what's on it, how hard you're being sought).
- **Child's Playroom:** the playground, the slide exit; the third-floor boss (it drops the Heroine doll); later the cutscene.
- **Angel:** its natural spawning.
- **Best Warden** (the Torture dimension's boss): model, arena, attacks (the bouncing cowboy hat), once the dimension is built.
- **Later:** structures, biomes, dimensions (Torture, Child's Playroom, how Avolition is reached), cutscenes, endings, the set-path variant.

## Open questions and conflicts

1. **What Aspiraton Drop is used for.**
2. **"Found around the world"** (Heroine, Crank, Speedy Car, Grape Hugs): chest loot, structure loot, mob drops, or placed in the world?
3. **Recipe for the Chisel.**
