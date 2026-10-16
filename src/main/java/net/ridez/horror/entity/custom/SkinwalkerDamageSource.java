package net.ridez.horror.entity.custom;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

// A Skinwalker's hit. Each hit picks one of its death messages at random (lang: death.attack.horrormod.skinwalker
// and the variants below), shown if that hit is the one that kills.
public class SkinwalkerDamageSource extends DamageSource {
    private static final String[] VARIANTS = {"", ".will_it_fit", ".why_doesnt_it_fit", ".glitched"};
    private final String variant;

    public SkinwalkerDamageSource(Holder<DamageType> pType, Entity pSkinwalker) {
        super(pType, pSkinwalker);
        this.variant = VARIANTS[pSkinwalker.getRandom().nextInt(VARIANTS.length)];
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
        Entity skinwalker = this.getEntity();
        return Component.translatable("death.attack." + this.type().msgId() + this.variant, pLivingEntity.getDisplayName(),
                skinwalker != null ? skinwalker.getDisplayName() : Component.empty());
    }
}
