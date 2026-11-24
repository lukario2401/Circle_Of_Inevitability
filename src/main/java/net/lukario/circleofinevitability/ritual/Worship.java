package net.lukario.circleofinevitability.ritual;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A player's active covenant. Only the server pays costs and advances it. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class Worship {
    private static final String SAVE_KEY = "circle_of_inevitability_worship";
    private static final Identifier HEART_COST = Physique.id("ritual_heart_cost");
    private static final Identifier BLESSING = Physique.id("ritual_blessing");
    private static final int[] FRIENDSHIP_RANKS = {10, 25, 50};
    private static final int[] HEART_GATES = {60, 80, 100};
    static final int MAX_HEARTS = HEART_GATES.length;
    static final int MAX_RANK = FRIENDSHIP_RANKS.length + MAX_HEARTS;
    static final int OFFERING_POINTS = 5;
    static final int BLOOD_POINTS = 3;
    static final int TASK_POINTS = 10;

    private final ServerPlayer player;
    private final CompoundTag data;
    RitualDeity deity;

    Worship(ServerPlayer player) {
        this.player = player;
        data = player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(SAVE_KEY);
        deity = registry(player).getValue(Identifier.tryParse(data.getStringOr("deity", "")));
    }

    static Registry<RitualDeity> registry(ServerPlayer player) {
        return player.registryAccess().lookupOrThrow(RitualDeity.REGISTRY);
    }

    private void save() {
        CompoundTag kept = player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        kept.put(SAVE_KEY, data);
        player.getPersistentData().put(ServerPlayer.PERSISTED_NBT_TAG, kept);
    }

    int friendship() {
        return data.getIntOr("friendship", 0);
    }

    int hearts() {
        return Math.clamp(data.getIntOr("hearts", 0), 0, MAX_HEARTS);
    }

    int rank() {
        int rank = hearts();
        for (int threshold : FRIENDSHIP_RANKS) {
            if (friendship() >= threshold) rank++;
        }
        return rank;
    }

    int limit() {
        return HEART_GATES[Math.min(hearts(), MAX_HEARTS - 1)];
    }

    boolean canEarn() {
        return deity != null && friendship() < limit();
    }

    String nextRank() {
        for (int threshold : FRIENDSHIP_RANKS) {
            if (friendship() < threshold) return "Next blessing at " + threshold + " friendship.";
        }
        if (hearts() == MAX_HEARTS) return "Your covenant is complete.";
        return "Sacrifice one max heart at " + limit() + " friendship for the next rank.";
    }

    void pledge(Holder.Reference<RitualDeity> choice) {
        if (choice.value() == deity) return;
        int returnedHearts = hearts();
        if (deity != null) Physique.remove(player, deity.blessing().attribute(), BLESSING);
        Physique.remove(player, Attributes.MAX_HEALTH, HEART_COST);
        if (returnedHearts > 0) player.heal(2 * returnedHearts);
        deity = choice.value();
        data.putString("deity", choice.key().identifier().toString());
        data.putInt("friendship", 0);
        data.putInt("hearts", 0);
        data.putInt("task_start", deity.task().total(player));
        save();
    }

    private void applyBlessing() {
        int hearts = hearts();
        Physique.keep(player, Attributes.MAX_HEALTH, HEART_COST, -2.0 * hearts,
                AttributeModifier.Operation.ADD_VALUE, deity != null && hearts > 0);
        if (deity != null) {
            var blessing = deity.blessing();
            Physique.keep(player, blessing.attribute(), BLESSING, blessing.amount() * rank(),
                    blessing.operation(), rank() > 0);
        }
    }

    private void earn(int points) {
        data.putInt("friendship", Math.min(limit(), friendship() + points));
        save();
        applyBlessing();
    }

    boolean offer() {
        if (!canEarn()) return false;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.is(deity.offering())) {
                stack.shrink(1);
                player.getInventory().setChanged();
                earn(OFFERING_POINTS);
                return true;
            }
        }
        return false;
    }

    boolean donateBlood() {
        float before = player.getHealth();
        if (!canEarn() || before <= 2 || player.isInvulnerable()) return false;
        player.hurtServer(player.level(), player.damageSources().magic(), 2);
        if (!player.isAlive() || player.getHealth() >= before) return false;
        earn(BLOOD_POINTS);
        return true;
    }

    int taskProgress() {
        int total = deity.task().total(player);
        int baseline = data.getIntOr("task_start", total);
        return Math.clamp((long) total - baseline, 0, deity.task().count());
    }

    boolean claimTask() {
        if (!canEarn() || taskProgress() < deity.task().count()) return false;
        data.putInt("task_start", data.getIntOr("task_start", 0) + deity.task().count());
        earn(TASK_POINTS);
        return true;
    }

    boolean canSacrifice() {
        return deity != null && hearts() < MAX_HEARTS && friendship() >= limit() && player.getMaxHealth() > 2;
    }

    boolean sacrifice() {
        if (!canSacrifice()) return false;
        data.putInt("hearts", hearts() + 1);
        save();
        applyBlessing();
        return true;
    }

    private static void restore(ServerPlayer player) {
        Worship worship = new Worship(player);
        if (!worship.data.contains("deity")) return;
        if (worship.deity == null) {
            // A pack can remove a deity between sessions. Release the old covenant.
            player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).remove(SAVE_KEY);
            player.sendSystemMessage(Component.literal("Your deity is no longer available. You may pledge anew."));
        }
        worship.applyBlessing();
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) restore(player);
    }
}
