package net.ridez.horror.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.item.custom.ModCrossbowItem;
import net.ridez.horror.item.custom.ToyGunItem;
import net.ridez.horror.network.ModNetwork;
import org.lwjgl.glfw.GLFW;

// The Toy Gun's controls on the player's side (see ToyGunItem): R reloads, left-click fires, right-click looks down
// the scope or stops (zoomed in, with the scope overlay drawn under the HUD by mixin/GuiMixin). A shot kicks: the
// shooter is pushed back a quarter of a block, their aim jumps up (and a little aside) and their view shakes hard.
@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ToyGunClient {
    public static final KeyMapping RELOAD = new KeyMapping("key.horrormod.reload", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.horrormod");
    private static final ResourceLocation SCOPE = ResourceLocation.fromNamespaceAndPath(HorrorMod.MOD_ID, "textures/misc/toy_gun_scope.png");
    private static final float SCOPE_ZOOM = 0.3F;
    // The kick: how far back it pushes the shooter (blocks), how far their aim climbs (degrees, over KICK_TICKS)
    // and how long their view shakes.
    private static final double PUSH_BACK = 0.25;
    private static final float KICK_DEGREES = 14.0F, KICK_SIDEWAYS = 4.0F;
    private static final int KICK_TICKS = 3;
    private static final float SHAKE_TICKS = 14.0F;
    private static boolean attackWasDown, useWasDown;
    private static int kickTicksLeft;
    private static float kickSideways;

    @Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Keys {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(RELOAD);
        }
    }

    private static boolean holdingGun(LocalPlayer pPlayer) {
        return pPlayer != null && pPlayer.getMainHandItem().getItem() instanceof ToyGunItem;
    }

    // R reloads; a fresh left-click fires (holding the button down doesn't keep firing as reloads finish); a fresh
    // right-click toggles the scope.
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean attackDown = minecraft.options.keyAttack.isDown();
        boolean useDown = minecraft.options.keyUse.isDown();
        if (player != null && minecraft.screen == null && holdingGun(player)) {
            while (RELOAD.consumeClick()) {
                ModNetwork.sendToServer(ModNetwork.ToyGunAction.RELOAD);
            }
            ItemStack gun = player.getMainHandItem();
            if (attackDown && !attackWasDown && ToyGunItem.isLoaded(gun) && !ToyGunItem.isReloading(gun)) {
                ModNetwork.sendToServer(ModNetwork.ToyGunAction.FIRE);
                ToyGunItem.leaveScope(player);
                ModCrossbowItem.recordShot(player.getUUID(), player.level().getGameTime());   // for the recoil
                kick(player);
            } else if (useDown && !useWasDown) {
                ModNetwork.sendToServer(ModNetwork.ToyGunAction.SCOPE);
                ToyGunItem.toggleScope(player);
            }
        } else {
            while (RELOAD.consumeClick()) {
                // not holding the gun: the key does nothing
            }
        }
        if (player != null && kickTicksLeft > 0) {
            kickTicksLeft--;
            player.setXRot(Math.max(-90.0F, player.getXRot() - KICK_DEGREES / KICK_TICKS));   // the aim climbs
            player.setYRot(player.getYRot() + kickSideways / KICK_TICKS);                    // and jumps aside
        }
        attackWasDown = attackDown;
        useWasDown = useDown;
    }

    // The shot pushes the shooter straight back (the player's game moves its own player, so it's done here): a speed
    // that friction brings to a stop after about PUSH_BACK blocks (less slows to a stop in the air than on the ground).
    private static void kick(LocalPlayer pPlayer) {
        Vec3 look = pPlayer.getViewVector(1.0F);
        Vec3 back = new Vec3(-look.x, 0.0, -look.z);
        if (back.lengthSqr() > 1.0E-4) {
            double drag = pPlayer.onGround() ? 0.6 * 0.91 : 0.91;
            back = back.normalize().scale(PUSH_BACK * (1.0 - drag));
            pPlayer.push(back.x, 0.0, back.z);
        }
        kickTicksLeft = KICK_TICKS;
        kickSideways = (pPlayer.getRandom().nextFloat() * 2.0F - 1.0F) * KICK_SIDEWAYS;
    }

    // The view shakes for a moment after a shot, settling fast.
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !holdingGun(minecraft.player)) {
            return;
        }
        float t = (float) (minecraft.player.level().getGameTime() - ModCrossbowItem.lastShotTime(minecraft.player.getUUID())
                + event.getPartialTick());
        if (t < 0.0F || t >= SHAKE_TICKS) {
            return;
        }
        float k = (1.0F - t / SHAKE_TICKS) * (1.0F - t / SHAKE_TICKS);
        event.setRoll(event.getRoll() + 8.0F * k * Mth.sin(t * 2.7F));
        event.setPitch(event.getPitch() + 4.0F * k * Mth.sin(t * 3.9F + 1.0F));
        event.setYaw(event.getYaw() + 3.0F * k * Mth.sin(t * 3.3F + 2.0F));
    }

    // Holding the gun, left-click is only the trigger and right-click only the scope: no swing, no attack, no
    // breaking, placing or using anything.
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if ((event.isAttack() || event.isUseItem()) && event.getHand() == InteractionHand.MAIN_HAND
                && holdingGun(Minecraft.getInstance().player)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    // Looking down the scope zooms in.
    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (ToyGunItem.isScoping(event.getPlayer())) {
            event.setNewFovModifier(SCOPE_ZOOM);
        }
    }

    // The scope overlay, drawn over the world and under the HUD (like the spyglass's): the round view through the
    // toy scope, black around it.
    public static void renderScope(GuiGraphics pGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson() || !ToyGunItem.isScoping(minecraft.player)) {
            return;
        }
        int width = pGraphics.guiWidth(), height = pGraphics.guiHeight();
        int size = Math.min(width, height);
        int x = (width - size) / 2, y = (height - size) / 2;
        RenderSystem.enableBlend();
        pGraphics.blit(SCOPE, x, y, size, size, 0, 0, 256, 256, 256, 256);
        RenderSystem.disableBlend();
        pGraphics.fill(0, 0, x, height, 0xFF000000);
        pGraphics.fill(x + size, 0, width, height, 0xFF000000);
        pGraphics.fill(x, 0, x + size, y, 0xFF000000);
        pGraphics.fill(x, y + size, x + size, height, 0xFF000000);
    }
}
