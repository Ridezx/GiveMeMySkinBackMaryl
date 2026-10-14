package net.ridez.horror.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

// A trader mob (Gustav, Walter, Frank) built like the wandering trader: right-click to trade, all trades
// available from the start, no levels. Each trade can be used a limited number of times and restocks
// once per Minecraft day. Never despawns.
public class TraderEntity extends AbstractVillager {
    private static final long TICKS_PER_DAY = 24000L;
    private final Supplier<List<MerchantOffer>> trades;
    private long lastRestockDay = -1;

    public TraderEntity(EntityType<? extends TraderEntity> type, Level level, Supplier<List<MerchantOffer>> trades) {
        super(type, level);
        this.trades = trades;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(5, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Mob.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (pHand == InteractionHand.MAIN_HAND) {
                pPlayer.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide()) {
                this.restockIfNewDay();
                if (!this.getOffers().isEmpty()) {
                    this.setTradingPlayer(pPlayer);
                    this.openTradingScreen(pPlayer, this.getDisplayName(), 1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(pPlayer, pHand);
    }

    // Checked when a player opens the trades, so it works no matter how time passed (sleeping, /time set).
    private void restockIfNewDay() {
        long day = this.level().getDayTime() / TICKS_PER_DAY;
        if (day != this.lastRestockDay) {
            this.lastRestockDay = day;
            this.getOffers().forEach(MerchantOffer::resetUses);
        }
    }

    @Override
    protected void updateTrades() {
        this.getOffers().addAll(this.trades.get());
    }

    // XP orbs per trade, like the wandering trader.
    @Override
    protected void rewardTradeXp(MerchantOffer pOffer) {
        if (pOffer.shouldRewardExp()) {
            int xp = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), xp));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putLong("LastRestockDay", this.lastRestockDay);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.lastRestockDay = pCompound.getLong("LastRestockDay");
    }
}
