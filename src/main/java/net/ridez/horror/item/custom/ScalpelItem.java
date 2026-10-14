package net.ridez.horror.item.custom;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

// Weapon: 1 damage per swing with a very fast attack cooldown.
// Crafting tool: in this mod's horrormod:tool_damaging_shapeless recipes it stays in the grid and loses
// 1 durability per craft (see ToolDamagingShapelessRecipe).
public class ScalpelItem extends Item {
    public ScalpelItem(Properties pProperties) {
        super(pProperties);
    }

    // Adds nothing to the player's base 1 damage and 4 attacks/second, so it hits for 1 with no real cooldown.
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 0.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }
}
