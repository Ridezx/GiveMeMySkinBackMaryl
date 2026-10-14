package net.ridez.horror.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

// Unknown how to make you take longer to eat.
public class ModFoodProperties {
    public static final FoodProperties placeholderfood = new FoodProperties.Builder().nutrition(10).saturationModifier(0.8f)
            .effect(new MobEffectInstance(MobEffects.UNLUCK,400),0.90f).build();

    // Saturation = nutrition * modifier * 2.
    // 12 hunger, 14.4 saturation (same as a golden carrot).
    public static final FoodProperties pelt = new FoodProperties.Builder().nutrition(12).saturationModifier(0.6f).build();
    // 6 hunger, 8 saturation.
    public static final FoodProperties graft = new FoodProperties.Builder().nutrition(6).saturationModifier(8f / 12f).build();
    // 4 hunger, 2 saturation, plus 5 seconds of Nausea and 2 seconds of Poison.
    // 4 hunger (2 bars), 0.8 saturation (a honey bottle's saturation modifier).
    public static final FoodProperties grape_hugs = new FoodProperties.Builder().nutrition(4).saturationModifier(0.1f).build();
    public static final FoodProperties rotted_pelt = new FoodProperties.Builder().nutrition(4).saturationModifier(0.25f)
            .effect(new MobEffectInstance(MobEffects.CONFUSION, 100), 1.0f)
            .effect(new MobEffectInstance(MobEffects.POISON, 40), 1.0f).build();



}
