package net.ridez.horror.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;

// Caps how many of one item existing loot can drop when its conditions match, leaving everything else alone.
// Used instead of overriding vanilla loot tables so other mods' changes to those tables still apply.
public class LimitItemModifier extends LootModifier {
    public static final MapCodec<LimitItemModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(modifier -> modifier.item))
            .and(Codec.INT.fieldOf("max").forGetter(modifier -> modifier.max))
            .apply(instance, LimitItemModifier::new));

    private final Item item;
    private final int max;

    public LimitItemModifier(LootItemCondition[] conditions, Item item, int max) {
        super(conditions);
        this.item = item;
        this.max = max;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        int left = this.max;
        for (ItemStack stack : generatedLoot) {
            if (stack.is(this.item)) {
                int keep = Math.min(stack.getCount(), left);
                stack.setCount(keep);
                left -= keep;
            }
        }
        generatedLoot.removeIf(ItemStack::isEmpty);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends LootModifier> codec() {
        return CODEC;
    }
}
