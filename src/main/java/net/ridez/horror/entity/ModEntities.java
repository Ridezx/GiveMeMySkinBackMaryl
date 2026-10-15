package net.ridez.horror.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.ridez.horror.HorrorMod;
import net.ridez.horror.entity.custom.DustBallProjectile;
import net.ridez.horror.entity.custom.SaltProjectile;
import net.ridez.horror.entity.custom.SkinwalkerEntity;
import net.ridez.horror.entity.custom.TraderEntity;

import java.util.List;
import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HorrorMod.MOD_ID);

    public static final RegistryObject<EntityType<SaltProjectile>> salt_projectile = ENTITY_TYPES.register("salt_projectile",
            () -> EntityType.Builder.<SaltProjectile>of(SaltProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("salt_projectile"));
    public static final RegistryObject<EntityType<DustBallProjectile>> dust_ball_projectile = ENTITY_TYPES.register("dust_ball_projectile",
            () -> EntityType.Builder.<DustBallProjectile>of(DustBallProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("dust_ball_projectile"));

    // 2.5x the player's height. Too tall for doorways, so houses are safe.
    public static final RegistryObject<EntityType<SkinwalkerEntity>> skinwalker = ENTITY_TYPES.register("skinwalker",
            () -> EntityType.Builder.<SkinwalkerEntity>of(SkinwalkerEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 4.5f).clientTrackingRange(10).build("skinwalker"));

    public static final RegistryObject<EntityType<TraderEntity>> gustav = registerTrader("gustav", ModTrades::gustav);
    public static final RegistryObject<EntityType<TraderEntity>> walter = registerTrader("walter", ModTrades::walter);
    public static final RegistryObject<EntityType<TraderEntity>> frank = registerTrader("frank", ModTrades::frank);

    // Villager-sized for now; resize once their custom models exist.
    private static RegistryObject<EntityType<TraderEntity>> registerTrader(String name, Supplier<List<MerchantOffer>> trades) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<TraderEntity>of(
                        (type, level) -> new TraderEntity(type, level, trades), MobCategory.CREATURE)
                .sized(0.6f, 1.95f).clientTrackingRange(10).build(name));
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(skinwalker.get(), SkinwalkerEntity.createAttributes().build());
        event.put(gustav.get(), TraderEntity.createAttributes().build());
        event.put(walter.get(), TraderEntity.createAttributes().build());
        event.put(frank.get(), TraderEntity.createAttributes().build());
    }

    // Natural spawns follow the usual hostile-mob rules: on solid ground, in the dark.
    private static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(skinwalker.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener(ModEntities::registerAttributes);
        eventBus.addListener(ModEntities::registerSpawnPlacements);
    }
}
