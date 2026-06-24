package net.ridez.horror.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

// Unknown how to make you take longer to eat.
public class ModFoodProperties {
    public static final FoodProperties placeholderfood = new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.UNLUCK,400),0.90f).build();



}
