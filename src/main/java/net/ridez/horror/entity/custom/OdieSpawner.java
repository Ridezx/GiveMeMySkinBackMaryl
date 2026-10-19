package net.ridez.horror.entity.custom;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.block.ModBlocks;
import net.ridez.horror.entity.ModEntities;
import net.ridez.horror.item.ModItems;
import org.jetbrains.annotations.Nullable;

// Brings Odie (OdieEntity) into the world. There's only ever one. He comes for a player carrying a lot of Dust
// (180 counted as Dust: a Dust Ball is 4, a Dust Brick 16), now and then, at most once every 10 minutes per player,
// in the morning, evening or at night (never around midday). He stands on high ground: a treeless hilltop or ridge
// at least 100 blocks up, 45 to 95 blocks from the player, where they could see him from where they are. If there's no such
// place around, he doesn't come.
// For testing, /odie appear (operators) brings him to the player running it straight away (any time, any amount
// of Dust), still on a summit.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID)
public class OdieSpawner {
    private static final int CHECK_INTERVAL = 100;      // ticks between checks (5 seconds)
    private static final int CHANCE = 8;                // 1 in this many checks, once everything else fits
    private static final int COOLDOWN = 12000;          // 10 minutes (half a day) between visits to a player
    private static final int DUST_NEEDED = 180;
    private static final int HIGH_GROUND = 100;
    private static final double MIN_DISTANCE = 45.0, MAX_DISTANCE = 95.0;
    private static final int TRIES = 96;
    private static final int TREE_CLEARANCE = 3;        // no logs or leaves this close
    private static final String LAST_VISIT = "horrormod_odie_last_visit";

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD
                || level.getGameTime() % CHECK_INTERVAL != 0 || isMidday(level) || odieExists(level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || dustCarried(player) < DUST_NEEDED) {
                continue;
            }
            long last = player.getPersistentData().getLong(LAST_VISIT);
            if (last != 0 && level.getGameTime() - last < COOLDOWN) {
                continue;
            }
            if (level.random.nextInt(CHANCE) == 0 && appear(level, player) != null) {
                return;
            }
        }
    }

    // Around midday (10 am to 2 pm), when he doesn't come and leaves if he's out.
    public static boolean isMidday(Level pLevel) {
        long time = pLevel.getDayTime() % 24000L;
        return time >= 4000L && time < 8000L;
    }

    private static boolean odieExists(ServerLevel pLevel) {
        return !pLevel.getEntities(ModEntities.odie.get(), odie -> odie.isAlive()).isEmpty();
    }

    // All the Dust a player carries, counted as Dust.
    public static int dustCarried(Player pPlayer) {
        Item dust = ModItems.dust.get(), ball = ModItems.dust_ball.get(), brick = ModBlocks.dustbricks.get().asItem();
        Inventory inventory = pPlayer.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(dust)) {
                total += stack.getCount();
            } else if (stack.is(ball)) {
                total += stack.getCount() * 4;
            } else if (stack.is(brick)) {
                total += stack.getCount() * 16;
            }
        }
        return total;
    }

    // Puts Odie on a summit within sight of the player, facing them. Null if there's nowhere suitable.
    @Nullable
    public static OdieEntity appear(ServerLevel pLevel, ServerPlayer pPlayer) {
        BlockPos spot = findSummit(pLevel, pPlayer);
        if (spot == null) {
            return null;
        }
        OdieEntity odie = ModEntities.odie.get().create(pLevel);
        if (odie == null) {
            return null;
        }
        double x = spot.getX() + 0.5, z = spot.getZ() + 0.5;
        float yaw = (float) (Mth.atan2(pPlayer.getZ() - z, pPlayer.getX() - x) * Mth.RAD_TO_DEG) - 90.0F;
        odie.moveTo(x, spot.getY(), z, yaw, 0.0F);
        odie.setYHeadRot(yaw);
        odie.yBodyRot = yaw;
        odie.setWatched(pPlayer.getUUID());
        pLevel.addFreshEntity(odie);
        pPlayer.getPersistentData().putLong(LAST_VISIT, pLevel.getGameTime());
        return odie;
    }

    @Nullable
    private static BlockPos findSummit(ServerLevel pLevel, ServerPlayer pPlayer) {
        RandomSource random = pLevel.random;
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = Mth.floor(pPlayer.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(pPlayer.getZ() + Math.sin(angle) * distance);
            if (!pLevel.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int ground = pLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (ground < HIGH_GROUND || pLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) != ground) {
                continue;                                   // too low, or under leaves
            }
            BlockPos feet = new BlockPos(x, ground, z);
            if (isSummit(pLevel, feet) && standable(pLevel, feet) && treeless(pLevel, feet) && inSight(pLevel, pPlayer, feet)) {
                return feet;
            }
        }
        return null;
    }

    // High ground: around him (5 blocks out, in 8 directions) the land doesn't rise more than 2 blocks above where he
    // stands, except in at most one direction (a ridge or shoulder of a mountain counts, not just the very peak).
    private static boolean isSummit(ServerLevel pLevel, BlockPos pFeet) {
        int higher = 0;
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            int x = pFeet.getX() + (int) Math.round(Math.cos(a) * 5), z = pFeet.getZ() + (int) Math.round(Math.sin(a) * 5);
            if (pLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) > pFeet.getY() + 2) {
                higher++;
            }
        }
        return higher <= 1;
    }

    // Solid ground under him and three blocks of open air to stand in (he's tall, and there's the hat).
    private static boolean standable(ServerLevel pLevel, BlockPos pFeet) {
        BlockState below = pLevel.getBlockState(pFeet.below());
        if (!below.isFaceSturdy(pLevel, pFeet.below(), Direction.UP) || !below.getFluidState().isEmpty()) {
            return false;
        }
        for (int dy = 0; dy < 3; dy++) {
            BlockPos p = pFeet.above(dy);
            if (!pLevel.getBlockState(p).getCollisionShape(pLevel, p).isEmpty() || !pLevel.getFluidState(p).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean treeless(ServerLevel pLevel, BlockPos pFeet) {
        for (BlockPos p : BlockPos.betweenClosed(pFeet.offset(-TREE_CLEARANCE, -1, -TREE_CLEARANCE),
                pFeet.offset(TREE_CLEARANCE, 5, TREE_CLEARANCE))) {
            BlockState state = pLevel.getBlockState(p);
            if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                return false;
            }
        }
        return true;
    }

    // The player could see him standing there from where they are now.
    private static boolean inSight(ServerLevel pLevel, ServerPlayer pPlayer, BlockPos pFeet) {
        Vec3 head = new Vec3(pFeet.getX() + 0.5, pFeet.getY() + 2.2, pFeet.getZ() + 0.5);
        return pLevel.clip(new ClipContext(pPlayer.getEyePosition(), head, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                pPlayer)).getType() == HitResult.Type.MISS;
    }

    // ---------------------------------------------------------------- /odie appear (testing)
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("odie")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("appear").executes(context -> appearCommand(context.getSource()))));
    }

    private static int appearCommand(CommandSourceStack pSource) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = pSource.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        level.getEntities(ModEntities.odie.get(), odie -> true).forEach(OdieEntity::discard);
        OdieEntity odie = appear(level, player);
        if (odie != null) {
            // where to look, for testing: compass direction, distance and coordinates
            double dx = odie.getX() - player.getX(), dz = odie.getZ() - player.getZ();
            String[] names = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
            String direction = names[Math.floorMod(Math.round((float) (Math.atan2(-dx, dz) * 4 / Math.PI)), 8)];
            String where = String.format("Odie is watching you from a summit %d blocks %s of you, at %d %d %d.",
                    Math.round(player.distanceTo(odie)), direction, odie.getBlockX(), odie.getBlockY(), odie.getBlockZ());
            pSource.sendSuccess(() -> Component.literal(where), true);
            return 1;
        }
        pSource.sendFailure(Component.literal("Nowhere for him to stand: he needs treeless high ground at y " + HIGH_GROUND
                + " or higher, " + (int) MIN_DISTANCE + "-" + (int) MAX_DISTANCE + " blocks away, in sight of you."));
        return 0;
    }
}
