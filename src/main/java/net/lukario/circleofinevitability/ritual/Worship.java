package net.lukario.circleofinevitability.ritual;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.status.Decree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class Worship {
    private static final String SAVE_KEY = "circle_of_inevitability_worship";
    private static final Identifier HEART_COST = Physique.id("ritual_heart_cost");
    private static final int[] FRIENDSHIP_RANKS = {10, 25, 50};
    private static final int[] HEART_GATES = {60, 80, 100};
    static final int MAX_HEARTS = HEART_GATES.length;
    static final int MAX_RANK = FRIENDSHIP_RANKS.length + MAX_HEARTS;
    static final int MAX_FAVOUR = 100;
    static final int EXALTED_FAVOUR = 50;
    static final int BLOOD_POINTS = 3;
    static final long REVIVAL_COOLDOWN_MS = 2L * 60 * 60 * 1000;
    private static final int SECONDS_PER_FAVOUR = 120;

    final ServerPlayer player;
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

    int friendship() { return data.getIntOr("friendship", 0); }
    int hearts() { return Math.clamp(data.getIntOr("hearts", 0), 0, MAX_HEARTS); }
    int favour() { return data.getIntOr("favour", 0); }
    boolean exalted() { return hearts() == MAX_HEARTS; }
    boolean armed() { return data.getBooleanOr("miracle", false); }
    long revivalWait() { return Math.max(0, data.getLongOr("miracle_ready_at", 0) - System.currentTimeMillis()); }
    private long time() { return player.level().getServer().overworld().getGameTime(); }
    int waitSeconds(String key) { return (int) Math.max(0, (data.getLongOr(key, 0) - time() + 19) / 20); }

    int rank() {
        int rank = hearts();
        for (int threshold : FRIENDSHIP_RANKS) if (friendship() >= threshold) rank++;
        return rank + (exalted() && favour() >= EXALTED_FAVOUR ? 1 : 0);
    }

    int gate() { return HEART_GATES[Math.min(hearts(), MAX_HEARTS - 1)]; }

    int room(RitualAltar altar, boolean task) {
        if (deity == null) return 0;
        if (exalted()) return task && altar.rank() == 3 ? MAX_FAVOUR - favour() : 0;
        return Math.max(0, Math.min(gate(), altar.limit()) - friendship());
    }

    String nextRank() {
        for (int threshold : FRIENDSHIP_RANKS) {
            if (friendship() < threshold) return "Next blessing at " + threshold + " friendship.";
        }
        if (exalted()) return "Complete sanctuary tasks for favour. Rank seven is active at 50 favour.";
        return "Sacrifice one max heart at " + gate() + " friendship for the next rank.";
    }

    void pledge(Holder.Reference<RitualDeity> choice) {
        if (choice.value() == deity) return;
        int returnedHearts = hearts();
        if (deity != null) deity.blessing().apply(player, 0);
        RitualBlessing.clearPulse(player, data);
        Physique.remove(player, Attributes.MAX_HEALTH, HEART_COST);
        if (returnedHearts > 0) player.heal(2 * returnedHearts);
        // Player-wide cooldowns survive changing faith, including returning to the old one.
        var keep = List.of("task_ready", "blood_ready", "offering_ready", "prayer_ready", "miracle_ready_at", "pulse_ready");
        for (String key : List.copyOf(data.keySet())) if (!keep.contains(key)) data.remove(key);
        deity = choice.value();
        data.putString("deity", choice.key().identifier().toString());
        save();
    }

    private void applyBlessing() {
        Physique.keep(player, Attributes.MAX_HEALTH, HEART_COST, -2.0 * hearts(),
                AttributeModifier.Operation.ADD_VALUE, deity != null && hearts() > 0);
        if (deity != null) deity.blessing().apply(player, rank());
    }

    private int earn(int points, int space) {
        int earned = Math.min(points, space);
        if (earned <= 0) return 0;
        if (exalted()) data.putInt("favour", favour() + earned);
        else data.putInt("friendship", friendship() + earned);
        save();
        applyBlessing();
        return earned;
    }

    /** The admin grant honours heart gates. Only a confirmed player sacrifice changes max health. */
    int grant(int amount) {
        if (deity == null) return 0;
        return earn(amount, exalted() ? MAX_FAVOUR - favour() : gate() - friendship());
    }

    boolean offer(BlockPos pos, RitualDeity.Offering offering) {
        int room = room(RitualAltar.inspect(player.level(), pos), false);
        boolean heldVial = player.getOffhandItem().is(ModItems.RITUAL_BLOOD_VIAL.get());
        if (room <= 0 || waitSeconds("offering_ready") > 0 || !deity.offerings().contains(offering)
                || !RitualDeity.pay(player, offering.item(), offering.count())) return false;
        if (offering.item().is(ModItems.RITUAL_BLOOD_VIAL.getKey())) {
            var empties = new ItemStack(Items.GLASS_BOTTLE, offering.count());
            if (heldVial && player.getOffhandItem().isEmpty()) player.setItemInHand(InteractionHand.OFF_HAND, empties);
            else player.getInventory().placeItemBackInInventory(empties);
        }
        data.putLong("offering_ready", time() + 20 * 45);
        earn(offering.points(), room);
        return true;
    }

    boolean donateBlood(BlockPos pos) {
        int room = room(RitualAltar.inspect(player.level(), pos), false);
        float before = player.getHealth();
        var knife = player.getMainHandItem();
        if (!knife.is(ModItems.SACRIFICIAL_KNIFE.get()) || room <= 0 || waitSeconds("blood_ready") > 0
                || before <= 2 || player.isInvulnerable()) return false;
        player.hurtServer(player.level(), player.damageSources().magic(), 2);
        if (!player.isAlive() || player.getHealth() >= before) return false;
        knife.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        data.putLong("blood_ready", time() + 20 * 90);
        earn(BLOOD_POINTS, room);
        return true;
    }

    boolean pray(BlockPos pos) {
        int room = room(RitualAltar.inspect(player.level(), pos), false);
        if (room <= 0 || waitSeconds("prayer_ready") > 0 || player.experienceLevel < 1) return false;
        player.giveExperienceLevels(-1);
        data.putLong("prayer_ready", time() + 20 * 300);
        earn(6, room);
        return true;
    }

    RitualTask task() { return deity.taskAt(data.getIntOr("task_index", 0)); }

    boolean boundHere(BlockPos pos) {
        return data.contains("altar_pos") && data.getLongOr("altar_pos", 0) == pos.asLong()
                && player.level().dimension().identifier().toString().equals(data.getStringOr("altar_dimension", ""));
    }

    boolean hasAssignment() { return data.contains("altar_pos"); }

    void bind(BlockPos pos) {
        if (deity == null || waitSeconds("task_ready") > 0 || boundHere(pos)) return;
        data.putLong("altar_pos", pos.asLong());
        data.putString("altar_dimension", player.level().dimension().identifier().toString());
        data.putInt("task_start", task().total(player));
        data.putInt("kills", 0);
        data.remove("quarry");
        save();
    }

    int taskProgress(BlockPos pos) {
        if (!boundHere(pos) || waitSeconds("task_ready") > 0) return 0;
        return task().progress(player, pos, data.getIntOr("task_start", 0), data.getIntOr("kills", 0));
    }

    boolean claimTask(BlockPos pos) {
        int room = room(RitualAltar.inspect(player.level(), pos), true);
        if (room <= 0 || taskProgress(pos) < task().count()) return false;
        RitualTask done = task();
        if (done.kind() == RitualTask.Kind.DELIVERY && !RitualDeity.pay(player, done.item().orElseThrow(), done.count())) return false;
        data.putLong("task_ready", time() + 20L * done.cooldown());
        data.putInt("task_index", (data.getIntOr("task_index", 0) + 1) % (deity.rotation().size() + 1));
        data.remove("altar_pos");
        data.remove("quarry");
        earn(done.reward(), room);
        return true;
    }

    /** Remember eligible creatures seen outside the ring during this assignment, bounded per player. */
    void observeQuarry() {
        if (!hasAssignment() || task().kind() != RitualTask.Kind.PROCESSION || data.getIntOr("kills", 0) >= task().count()) return;
        BlockPos altar = BlockPos.of(data.getLongOr("altar_pos", 0));
        Vec3 center = Vec3.atCenterOf(altar);
        if (!boundHere(altar) || player.distanceToSqr(center) > 32 * 32 || !player.level().hasChunkAt(altar)
                || !player.level().getBlockState(altar).is(RitualTableBlock.TABLE.get())) return;
        CompoundTag marked = data.getCompoundOrEmpty("quarry");
        long now = time();
        for (String id : List.copyOf(marked.keySet())) if (marked.getLongOr(id, 0) <= now) marked.remove(id);
        for (var entity : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(altar).inflate(24),
                e -> e.isAlive() && task().accepts(e) && e.distanceToSqr(player) <= 16 * 16
                        && e.distanceToSqr(center) <= 24 * 24 && e.distanceToSqr(center) > RitualTask.RADIUS * RitualTask.RADIUS)) {
            if (marked.size() < 64 || marked.contains(entity.getStringUUID())) marked.putLong(entity.getStringUUID(), now + 6000);
        }
        data.put("quarry", marked);
    }

    private void recordKill(LivingEntity victim) {
        if (!hasAssignment() || deity == null || task().kind() != RitualTask.Kind.PROCESSION
                || !task().accepts(victim) || data.getIntOr("kills", 0) >= task().count()) return;
        BlockPos altar = BlockPos.of(data.getLongOr("altar_pos", 0));
        Vec3 center = Vec3.atCenterOf(altar);
        var marked = data.getCompoundOrEmpty("quarry");
        if (!boundHere(altar) || player.distanceToSqr(center) > 64 || victim.distanceToSqr(center) > 64
                || !player.level().getBlockState(altar).is(RitualTableBlock.TABLE.get())
                || marked.getLongOr(victim.getStringUUID(), 0) <= time()) return;
        marked.remove(victim.getStringUUID());
        data.putInt("kills", data.getIntOr("kills", 0) + 1);
        save();
        var bottle = player.getOffhandItem();
        if (bottle.is(Items.GLASS_BOTTLE)) {
            var filled = new ItemStack(ModItems.RITUAL_BLOOD_VIAL.get());
            player.setItemInHand(InteractionHand.OFF_HAND, ItemUtils.createFilledResult(bottle, player, filled));
        }
    }

    boolean canSacrifice(RitualAltar altar) {
        return deity != null && !exalted() && friendship() >= gate() && altar.limit() >= gate() && player.getMaxHealth() > 2;
    }

    boolean sacrifice(BlockPos pos) {
        if (!canSacrifice(RitualAltar.inspect(player.level(), pos))) return false;
        data.putInt("hearts", hearts() + 1);
        save();
        applyBlessing();
        return true;
    }

    boolean canArm(RitualAltar altar) {
        return deity != null && deity.revival() && exalted() && favour() == MAX_FAVOUR && altar.rank() == 3
                && !armed() && revivalWait() == 0;
    }

    boolean arm(BlockPos pos) {
        if (!canArm(RitualAltar.inspect(player.level(), pos)) || Decree.violate(player, Decree.Rule.RESURRECTION)) return false;
        data.putBoolean("miracle", true);
        save();
        return true;
    }

    void onlineSecond() {
        if (deity == null) return;
        observeQuarry();
        if (favour() > 0) {
            int elapsed = data.getIntOr("decay_seconds", 0) + 1;
            if (elapsed >= SECONDS_PER_FAVOUR) {
                data.putInt("favour", favour() - 1);
                if (favour() == 0) data.remove("miracle");
                if (favour() == EXALTED_FAVOUR - 1) applyBlessing();
                elapsed = 0;
            }
            data.putInt("decay_seconds", elapsed);
        }
        if (rank() > 0 && time() >= data.getLongOr("pulse_ready", 0)) {
            boolean triggered = deity.blessing().pulse(player, data, rank());
            data.putLong("pulse_ready", time() + 20L * (triggered ? 45 : 5));
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer p && p.tickCount % 20 == 0 && p.isAlive() && !p.isSpectator()) {
            new Worship(p).onlineSecond();
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (SacrificialKnifeItem.isStrike(event.getSource()) && event.getSource().getEntity() instanceof ServerPlayer p) {
            new Worship(p).recordKill(event.getEntity());
        }
    }

    @SubscribeEvent(priority = Priority.LOW)
    public static boolean miracle(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return false;
        Worship faith = new Worship(p);
        if (!faith.armed() || faith.deity == null || !faith.deity.revival() || !faith.exalted() || faith.favour() <= 0
                || faith.revivalWait() > 0 || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || Spells.isPiercing(p) || Decree.violate(p, Decree.Rule.RESURRECTION)) return false;
        faith.data.remove("miracle");
        faith.data.putInt("favour", 0);
        faith.data.putInt("decay_seconds", 0);
        faith.data.putLong("miracle_ready_at", System.currentTimeMillis() + REVIVAL_COOLDOWN_MS);
        faith.save();
        faith.applyBlessing();
        p.setHealth(Math.max(1, p.getMaxHealth() * 0.5f));
        p.clearFire();
        p.invulnerableTime = 20;
        p.sendSystemMessage(Component.literal("A miracle from the gray fog returns you to life."));
        return true;
    }

    private static void restore(ServerPlayer player) {
        Worship worship = new Worship(player);
        if (!worship.data.contains("deity")) return;
        if (worship.deity == null) {
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
