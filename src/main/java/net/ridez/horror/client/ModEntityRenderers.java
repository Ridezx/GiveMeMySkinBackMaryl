package net.ridez.horror.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.ModEntities;

@Mod.EventBusSubscriber(modid = HorrorMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Draws each projectile as its item (Salt, Dust Ball), like snowballs and eggs.
        event.registerEntityRenderer(ModEntities.salt_projectile.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.dust_ball_projectile.get(), ThrownItemRenderer::new);

        event.registerEntityRenderer(ModEntities.skinwalker.get(), SkinwalkerRenderer::new);

        event.registerEntityRenderer(ModEntities.gustav.get(), context -> new TraderRenderer(context, "gustav"));
        event.registerEntityRenderer(ModEntities.walter.get(), context -> new TraderRenderer(context, "walter"));
        event.registerEntityRenderer(ModEntities.frank.get(), context -> new TraderRenderer(context, "frank"));
    }
}
