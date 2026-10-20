package net.ridez.horror.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.ridez.horror.entity.custom.DustBallProjectile;
import net.ridez.horror.item.ModItems;

import java.util.List;
import java.util.function.Consumer;

// Rare, uncraftable weapon, a toy rifle that holds one Dust Ball:
// - R (the "Reload Toy Gun" key) reloads it: 3 seconds of loading, then a Dust Ball from the inventory goes in.
//   Switching away from it stops the reload; the ball is only used up once it's in.
// - Right-click looks down its scope (zoomed in, with the scope overlay, see client/ToyGunClient); right-click
//   again to stop. Reloading, firing or putting it away also ends it.
// - Left-click fires, from the hip (wildly inaccurate) or scoped (dead on): the Dust Ball flies fast enough to act
//   almost like hitscan, with a muzzle flash, a puff of dust and a hard recoil kick that pushes the shooter back
//   and shakes their view. 1 durability per shot. Holding the gun, left-click and right-click never swing, attack, break blocks or use them.
// The keys and clicks reach the server through ModNetwork; the loaded/reloading/scoped state is kept on the item,
// so every client can animate it (client/WeaponAnimations).
public class ToyGunItem extends Item {
    public static final int RELOAD_TICKS = 60;
    // Blocks per tick; a crossbow arrow is 3.15. Projectiles check their whole path each tick, so this can't skip
    // through targets: anything within ~15 blocks is hit the same tick it's fired.
    private static final float SHOT_VELOCITY = 15.0F;
    // Spread when fired from the hip (vanilla's inaccuracy units: a bow shot is 1): wild, about 13 degrees either way
    // on average. Through the scope it goes exactly where the crosshair is.
    private static final float HIP_FIRE_INACCURACY = 30.0F;
    private static final String LOADED = "Loaded", RELOAD_START = "ReloadStart", SCOPED = "Scoped";

    public ToyGunItem(Properties pProperties) {
        super(pProperties);
    }

    // ---------------------------------------------------------------- state (kept on the item)
    private static CustomData data(ItemStack pStack) {
        return pStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
    }

    public static boolean isLoaded(ItemStack pStack) {
        return data(pStack).copyTag().getBoolean(LOADED);
    }

    public static boolean isReloading(ItemStack pStack) {
        return data(pStack).contains(RELOAD_START);
    }

    // How far through the reload it is (0-1), or -1 if it isn't reloading.
    public static float reloadProgress(ItemStack pStack, Level pLevel, float pPartialTick) {
        if (!isReloading(pStack)) {
            return -1.0F;
        }
        long start = data(pStack).copyTag().getLong(RELOAD_START);
        return Math.min(1.0F, Math.max(0.0F, (pLevel.getGameTime() - start + pPartialTick) / RELOAD_TICKS));
    }

    private static void set(ItemStack pStack, boolean pLoaded, long pReloadStart) {
        CustomData.update(DataComponents.CUSTOM_DATA, pStack, tag -> {
            tag.putBoolean(LOADED, pLoaded);
            if (pReloadStart >= 0) {
                tag.putLong(RELOAD_START, pReloadStart);
            } else {
                tag.remove(RELOAD_START);
            }
        });
    }

    private static void setScoped(ItemStack pStack, boolean pScoped) {
        if (data(pStack).contains(SCOPED) != pScoped) {
            CustomData.update(DataComponents.CUSTOM_DATA, pStack, tag -> {
                if (pScoped) {
                    tag.putBoolean(SCOPED, true);
                } else {
                    tag.remove(SCOPED);
                }
            });
        }
    }

    // ---------------------------------------------------------------- reloading and firing (server)
    // R pressed: starts reloading if it's empty, not already reloading, and there's a Dust Ball to load.
    public static void startReload(ServerPlayer pPlayer) {
        ItemStack stack = pPlayer.getMainHandItem();
        if (!(stack.getItem() instanceof ToyGunItem) || isLoaded(stack) || isReloading(stack) || findAmmo(pPlayer).isEmpty()) {
            return;
        }
        setScoped(stack, false);                             // no scoping while reloading
        set(stack, false, pPlayer.level().getGameTime());
        pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.CROSSBOW_LOADING_START.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static ItemStack findAmmo(Player pPlayer) {
        if (pPlayer.getAbilities().instabuild) {
            return new ItemStack(ModItems.dust_ball.get());
        }
        for (int i = 0; i < pPlayer.getInventory().getContainerSize(); i++) {
            ItemStack stack = pPlayer.getInventory().getItem(i);
            if (stack.is(ModItems.dust_ball.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (pLevel.isClientSide() || !(pEntity instanceof Player player)) {
            return;
        }
        if (!pIsSelected) {
            setScoped(pStack, false);                        // put away: no longer looking down the scope
        }
        if (!isReloading(pStack)) {
            return;
        }
        if (!pIsSelected) {
            set(pStack, false, -1);                          // put away mid-reload: the reload's off
            return;
        }
        long ticks = pLevel.getGameTime() - data(pStack).copyTag().getLong(RELOAD_START);
        if (ticks == RELOAD_TICKS / 2) {
            pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (ticks >= RELOAD_TICKS) {
            ItemStack ammo = findAmmo(player);
            boolean loaded = !ammo.isEmpty();
            if (loaded && !player.getAbilities().instabuild) {
                ammo.shrink(1);
            }
            set(pStack, loaded, -1);
            if (loaded) {
                pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    // Left-click: fires the loaded Dust Ball.
    public static void fire(ServerPlayer pPlayer) {
        ItemStack stack = pPlayer.getMainHandItem();
        if (!(stack.getItem() instanceof ToyGunItem) || !isLoaded(stack) || isReloading(stack)) {
            return;
        }
        ServerLevel level = pPlayer.serverLevel();
        boolean scoped = isScoping(pPlayer);
        set(stack, false, -1);
        setScoped(stack, false);                             // the kick knocks the eye off the scope
        DustBallProjectile ball = new DustBallProjectile(level, pPlayer);
        Vec3 look = pPlayer.getViewVector(1.0F);
        ball.shoot(look.x, look.y, look.z, SHOT_VELOCITY, scoped ? 0.0F : HIP_FIRE_INACCURACY);
        level.addFreshEntity(ball);
        // placeholder: a firework's pop, pitched up like a cap gun, until the mod has its own sounds
        level.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                1.0F, 1.5F + pPlayer.getRandom().nextFloat() * 0.2F);
        muzzleFlash(level, pPlayer);
        stack.hurtAndBreak(1, pPlayer, EquipmentSlot.MAINHAND);
    }

    // A burst of small flames and sparks out of the barrel, then smoke and a puff of dust.
    private static void muzzleFlash(ServerLevel pLevel, Player pPlayer) {
        Vec3 look = pPlayer.getViewVector(1.0F);
        Vec3 right = look.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        double side = pPlayer.getMainArm() == HumanoidArm.RIGHT ? 0.28 : -0.28;
        Vec3 muzzle = pPlayer.getEyePosition().add(look.scale(1.3)).add(right.scale(side)).subtract(up.scale(0.22));
        for (int i = 0; i < 6; i++) {
            double speed = 0.08 + pPlayer.getRandom().nextDouble() * 0.12;
            pLevel.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 0, look.x, look.y, look.z, speed);
        }
        pLevel.sendParticles(ParticleTypes.SMALL_FLAME, muzzle.x, muzzle.y, muzzle.z, 6, 0.05, 0.05, 0.05, 0.02);
        pLevel.sendParticles(ParticleTypes.CRIT, muzzle.x, muzzle.y, muzzle.z, 5, 0.05, 0.05, 0.05, 0.25);
        pLevel.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 6, 0.06, 0.06, 0.06, 0.02);
        pLevel.sendParticles(ParticleTypes.DUST_PLUME, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.01);
    }

    // ---------------------------------------------------------------- the scope (right-click toggles it)
    // Right-click: looks down the scope, or stops. Run on the server (ModNetwork) and, so it responds at once, on
    // the shooter's own game too; not while reloading.
    public static void toggleScope(Player pPlayer) {
        ItemStack stack = pPlayer.getMainHandItem();
        if (stack.getItem() instanceof ToyGunItem && !isReloading(stack)) {
            setScoped(stack, !data(stack).contains(SCOPED));
        }
    }

    // The shooter's own game, firing: off the scope straight away (the server does the same in fire).
    public static void leaveScope(Player pPlayer) {
        if (pPlayer.getMainHandItem().getItem() instanceof ToyGunItem) {
            setScoped(pPlayer.getMainHandItem(), false);
        }
    }

    public static boolean isScoping(LivingEntity pEntity) {
        ItemStack stack = pEntity.getMainHandItem();
        return stack.getItem() instanceof ToyGunItem && data(stack).contains(SCOPED);
    }

    // ---------------------------------------------------------------- left-click does nothing else
    @Override
    public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack pStack, LivingEntity pEntity) {
        return true;                                         // no swing
    }

    // Loading, reloading and firing change the item's data; that shouldn't count as switching items (which would
    // dip the first-person hand and cut the animations short).
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack pOldStack, ItemStack pNewStack, boolean pSlotChanged) {
        return pSlotChanged || !pOldStack.is(pNewStack.getItem());
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltip, TooltipFlag pFlag) {
        pTooltip.add(isLoaded(pStack) ? Component.translatable("tooltip.horrormod.toy_gun.loaded").withStyle(ChatFormatting.RED)
                : Component.translatable("tooltip.horrormod.toy_gun.empty").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(pStack, pContext, pTooltip, pFlag);
    }

    // First- and third-person animation (client only).
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> pConsumer) {
        pConsumer.accept(net.ridez.horror.client.WeaponAnimations.TOY_GUN);
    }
}
