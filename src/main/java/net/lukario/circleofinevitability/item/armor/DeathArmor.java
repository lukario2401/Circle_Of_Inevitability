package net.lukario.circleofinevitability.item.armor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.PaleEmperorFx;
import net.lukario.circleofinevitability.combat.DamageLimit;
import net.lukario.circleofinevitability.combat.DragUnder;
import net.lukario.circleofinevitability.combat.FollowerSummon;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.KillToll;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.MaxHealthDrain;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

/**
 * Vestments of Death (Death pathway). Power with a price on every side.
 * <ul>
 *   <li>Reaping (every piece): every pulse, hostiles nearby are struck for the same damage and marked; the closer they
 *       stand, the more marks a pulse gives. Each piece worn reaches further, hits harder and pulses sooner
 *       ({@link #reach}, {@link #pulseDamage}, {@link #pulseTicks}). Past {@link #MARK_LIMIT} marks the
 *       underworld drags them under ({@link DragUnder}): crushed down there, flung out again if they live.</li>
 *   <li>The curse (any piece, worse with every piece): endless Hunger; sudden pangs that hurt but never kill; and the
 *       toll ({@link KillToll}): it must be fed a kill in time ({@link #tollRate}). It warns, and then it takes the
 *       wearer instead.</li>
 *   <li>Full set: a Spirit Guide ({@link FollowerSummon}) fights beside the wearer, hurling soul skulls that mark too, but
 *       the wearer's max health ebbs away ({@link MaxHealthDrain}) until they die or take a piece off. Off, it
 *       slowly comes back.</li>
 * </ul>
 * Every number is a public constant here; the reusable parts live in combat/ and status/ for other sets and abilities.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathArmor {
    public static final String SET = "death";
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(42,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            18, SoundEvents.ARMOR_EQUIP_NETHERITE, 3f, .1f,
            TagKey.create(Registries.ITEM, InevitabilityArmor.id("repairs_death_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, InevitabilityArmor.id(SET)));

    // ---------- Reaping ----------
    /** Game time of the last Reaping pulse that hit something, kept on every worn piece: the models react to it. */
    public static final String REAP_KEY = "coi_death_reap";
    public static final String MARK = "circle_of_inevitability_death_mark";
    /** Every piece pulses; each one worn adds to the pulse. 1 piece: 4.5 blocks, 1.5 damage, every 2.5 s. 4: 9, 3, 1 s. */
    public static final double REACH = 3, REACH_PER_PIECE = 1.5;
    /** Every mob in reach takes the same; distance only changes how many marks it gets. */
    public static final float PULSE_DAMAGE = 1, PULSE_DAMAGE_PER_PIECE = .5f;
    public static final int PULSE_TICKS = 60, PULSE_TICKS_PER_PIECE = 10;
    /** Marks a pulse gives within each distance, nearest ring first. Anything further in reach gets one. */
    public record Ring(double within, int marks) {}
    public static final List<Ring> RINGS = List.of(new Ring(2.5, 3), new Ring(5, 2));
    /** More marks than this and the underworld claims it. */
    public static final int MARK_LIMIT = 6;
    /** One mark wears off every this many ticks while the wearer is not marking it. */
    public static final int MARK_DROP = 60;
    public static final DragUnder CLAIM = DragUnder.of(30f).sink(14).hold(36).throwsUp(1.05).arms(4);
    private static final ModelFx MARK_SKULL = PaleEmperorFx.SKULL.noKeys().scale(.09f).fade(2, 6);

    // ---------- The curse ----------
    /** The toll's clock: it runs {@link #tollRate} times as fast as time, so 1 piece wants a kill every 10 minutes
     *  and the full set every 4. */
    public static final int TOLL_TICKS = 24000;
    public static final KillToll TOLL = KillToll.of("death_armor_toll", TOLL_TICKS)
            .warnAt(1200, 600, 200, 100, 80, 60, 40, 20)
            .hud("Toll", 0x9CFFD2)
            .onWarning(DeathArmor::warn)
            .onFed(DeathArmor::fed)
            .onExpired(DeathArmor::takeWearer);
    /** Chance each second of a sudden pang, per piece worn, and how much it can hurt: 1 up to 1 + pieces. A pang
     *  never takes the last half heart. 1 piece: about every 40 s, up to 2 damage; full set: about every 10 s, up to 5. */
    public static final float PANG_CHANCE_PER_PIECE = 1 / 40f, PANG_MIN = 1, PANG_MAX_PER_PIECE = 1;
    /** Hunger amplifier by pieces worn (index = pieces): Hunger I, I, II, III. */
    public static final int[] HUNGER = {0, 0, 0, 1, 2};
    public static final ResourceKey<DamageType> TOLL_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, InevitabilityArmor.id("death_toll"));
    public static final ResourceKey<DamageType> CURSE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, InevitabilityArmor.id("death_curse"));

    // ---------- Full set ----------
    public static final String GUIDE = "death_armor_spirit_guide";
    public static final FollowerSummon SPIRIT_GUIDE = FollowerSummon.hover(
                    PaleEmperorFx.WRAITH.noKeys().scale(.4f).fade(12, 10), new Vec3(1.1, .75, .3))
            .strikes(7, 25, 6f).lasts(20 * 60 * 60)
            .targets(TentacleCombat::hostile)
            .onStrike(DeathArmor::soulSkull)
            .onHit((owner, target) -> mark(owner, target, 2));
    /** The Spirit Guide's soul skull: flies from the guide to its target in this many ticks. */
    public static final int SKULL_FLIGHT = 4;
    private static final ModelFx SOUL_SKULL = PaleEmperorFx.SKULL.noKeys().scale(.16f).glow().fade(0, 2);
    public static final Identifier EBB = InevitabilityArmor.id("death_armor_ebb");
    /** Full set: one point of max health every EBB_TICKS; off, one point back every RECOVER_TICKS. */
    public static final int EBB_TICKS = 300, RECOVER_TICKS = 200;
    public static final double EBB_STEP = 1;

    private DeathArmor() {}

    public static boolean wearing(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).getItem() instanceof TentacleArmorItem item
                && item.setId().equals(SET) && item.slot() == slot;
    }
    public static int pieces(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : InevitabilityArmor.SLOTS) if (wearing(entity, slot)) count++;
        return count;
    }
    public static boolean fullSet(LivingEntity entity) { return pieces(entity) == InevitabilityArmor.SLOTS.length; }

    /** Creative and spectator players carry no curse; armor stands are only displaying it. */
    static boolean cursable(LivingEntity wearer) {
        return !(wearer instanceof ArmorStand) && !(wearer instanceof Player player && (player.isSpectator() || player.getAbilities().invulnerable));
    }

    // =========================
    // Tick
    // =========================

    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity wearer = event.getEntity();
        if (!(wearer.level() instanceof ServerLevel level) || wearer instanceof ArmorStand) return;
        int pieces = wearer.isAlive() ? pieces(wearer) : 0;
        boolean full = pieces == InevitabilityArmor.SLOTS.length;
        if (pieces == 0) {
            if ((wearer.tickCount + wearer.getId()) % 20 == 0) offTick(wearer);
            return;
        }
        long now = level.getGameTime();
        if (!Root.has(wearer, RootRestriction.ABILITIES) && !Root.has(wearer, RootRestriction.ATTACK)
                && (wearer.tickCount + wearer.getId()) % pulseTicks(pieces) == 0)
            reap(wearer, level, pieces, now);
        if (cursable(wearer)) curse(wearer, level, pieces);
        else TOLL.pause(wearer);
        companion(wearer, full && cursable(wearer));
        ebb(wearer, level, full && cursable(wearer));
    }

    /** No piece worn: the toll waits, the Spirit Guide leaves, and drained life slowly comes back. */
    static void offTick(LivingEntity wearer) {
        TOLL.pause(wearer);
        if (FollowerSummon.isActive(wearer, GUIDE)) FollowerSummon.dismiss(wearer, GUIDE);
        if (MaxHealthDrain.drained(wearer, EBB) > 0 && (wearer.tickCount + wearer.getId()) % RECOVER_TICKS == 0)
            MaxHealthDrain.recover(wearer, EBB, EBB_STEP);
    }

    // =========================
    // Reaping
    // =========================

    /** One pulse: everything hostile in reach is struck for the same damage and marked by distance. */
    static void reap(LivingEntity wearer, ServerLevel level, int pieces, long now) {
        double reach = reach(pieces);
        float damage = pulseDamage(pieces);
        boolean any = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(reach),
                other -> reapable(wearer, other, reach))) {
            any = true;
            Vec3 from = target.position().add(0, target.getBbHeight() * .6, 0);
            ParticleShapes.line(level, ParticleTypes.SOUL, from, wearer.position().add(0, 1.1, 0), 1.2);
            level.sendParticles(ParticleTypes.SCULK_SOUL, from.x, from.y, from.z, 3, .2, .25, .2, .02);
            Spells.strike(wearer, target, damage);
            if (target.isAlive()) mark(wearer, target, marksAt(Math.sqrt(wearer.distanceToSqr(target))));
        }
        if (!any) return;
        for (EquipmentSlot slot : InevitabilityArmor.SLOTS) if (wearing(wearer, slot))
            CustomData.update(DataComponents.CUSTOM_DATA, wearer.getItemBySlot(slot), tag -> tag.putLong(REAP_KEY, now));
        ParticleShapes.ring(level, ParticleTypes.SOUL_FIRE_FLAME, wearer.position().add(0, .15, 0), .6, 12 + pieces * 6, .12 + pieces * .05);
        level.playSound(null, wearer.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, .9f, .7f);
    }

    /** Blocks the pulse reaches with this many pieces worn. */
    public static double reach(int pieces) { return REACH + pieces * REACH_PER_PIECE; }
    /** What every mob in reach takes from one pulse with this many pieces worn. */
    public static float pulseDamage(int pieces) { return PULSE_DAMAGE + pieces * PULSE_DAMAGE_PER_PIECE; }
    /** Ticks between pulses with this many pieces worn. */
    public static int pulseTicks(int pieces) { return Math.max(1, PULSE_TICKS - pieces * PULSE_TICKS_PER_PIECE); }

    /** Hostile species and anything hunting the wearer, never allies, in sight and in reach. */
    static boolean reapable(LivingEntity wearer, LivingEntity target, double reach) {
        return target != wearer && target.isAlive() && !target.isSpectator() && !target.isInvulnerable()
                && !(target instanceof ArmorStand) && !DragUnder.isDragging(target)
                && TentacleCombat.hostile(wearer, target)
                && wearer.distanceToSqr(target) <= reach * reach && wearer.hasLineOfSight(target);
    }

    /** Marks one pulse gives at this distance. */
    public static int marksAt(double distance) {
        for (Ring ring : RINGS) if (distance <= ring.within()) return ring.marks();
        return 1;
    }

    /** Marks the target; past the limit the underworld takes it. Returns its marks now (0 once it is claimed). */
    public static int mark(LivingEntity wearer, LivingEntity target, int amount) {
        if (!(wearer.level() instanceof ServerLevel level) || DragUnder.isDragging(target)) return 0;
        int marks = Marks.add(target, wearer, MARK, amount, MARK_LIMIT * 2, MARK_DROP);
        if (marks > MARK_LIMIT) {
            Marks.clear(target, wearer, MARK);
            ParticleShapes.clearModels(level, target, MARK);
            if (CLAIM.drag(wearer, target) && wearer instanceof ServerPlayer player)
                player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability." + SET + ".claimed",
                        target.getDisplayName()));
            return 0;
        }
        SpellFx.overHead(level, target, MARK_SKULL.lifetime(MARK_DROP + 10), marks, MARK);
        return marks;
    }

    /** The Spirit Guide hurls a glowing soul skull, jaw open, along a trail of soul fire; it bursts on its target. */
    static void soulSkull(LivingEntity owner, LivingEntity target, Vec3 from) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        Vec3 to = target.position().add(0, target.getBbHeight() * .6, 0);
        Vec3 path = to.subtract(from);
        float distance = (float)path.length();
        ModelFx skull = SOUL_SKULL.lifetime(SKULL_FLIGHT + 2)
                .key(0, ModelFx.pose().frame(2).scale(.6f))
                .key(SKULL_FLIGHT, ModelFx.pose().frame(2).forward(distance).scale(1.1f), ModelFx.Ease.IN);
        ParticleShapes.modelFacing(level, skull, from, path);
        ParticleShapes.line(level, ParticleTypes.SOUL_FIRE_FLAME, from, to, 2.5);
        level.sendParticles(ParticleTypes.SOUL, to.x, to.y, to.z, 10, .25, .3, .25, .05);
        level.sendParticles(ParticleTypes.SCULK_SOUL, to.x, to.y, to.z, 4, .2, .2, .2, .03);
        level.playSound(null, owner.blockPosition(), SoundEvents.VEX_CHARGE, SoundSource.PLAYERS, .7f, .6f);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1f, 1.2f);
    }

    // =========================
    // The curse
    // =========================

    /** Every piece worn deepens the curse: more hunger, more and harder pangs, a hungrier toll. */
    static void curse(LivingEntity wearer, ServerLevel level, int pieces) {
        Spells.keepEffect(wearer, MobEffects.HUNGER, hungerAmplifier(pieces), 80);
        if (level.getDifficulty() != Difficulty.PEACEFUL) TOLL.tick(wearer, tollRate(pieces));
        else TOLL.pause(wearer);
        if ((wearer.tickCount + wearer.getId()) % 20 == 0 && wearer.getRandom().nextFloat() < pangChance(pieces))
            pang(wearer, level, pangMax(pieces));
    }

    public static int hungerAmplifier(int pieces) { return HUNGER[Math.clamp(pieces, 0, HUNGER.length - 1)]; }
    /** Chance each second of a pang with this many pieces worn. */
    public static float pangChance(int pieces) { return pieces * PANG_CHANCE_PER_PIECE; }
    /** The most one pang can hurt with this many pieces worn. */
    public static float pangMax(int pieces) { return PANG_MIN + pieces * PANG_MAX_PER_PIECE; }
    /** How fast the toll's clock runs: 1 piece 10 minutes, 2 about 6.7, 3 five, the full set 4. */
    public static double tollRate(int pieces) { return pieces + 1; }
    /** Real ticks the toll allows with this many pieces worn, from full. */
    public static int tollTicks(int pieces) { return (int)Math.ceil(TOLL_TICKS / tollRate(pieces)); }

    /** A sudden pain of up to 'max' that cannot kill: capped so at least half a heart is always left. */
    public static boolean pang(LivingEntity wearer, ServerLevel level, float max) {
        float spare = wearer.getHealth() + wearer.getAbsorptionAmount() - 1;
        if (spare <= 0) return false;
        float amount = Math.min(spare, PANG_MIN + wearer.getRandom().nextFloat() * Math.max(0, max - PANG_MIN));
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(CURSE_DAMAGE));
        wearer.invulnerableTime = 0;
        boolean[] hurt = {false};
        DamageLimit.run(wearer, spare, () -> hurt[0] = wearer.hurtServer(level, source, amount));
        if (hurt[0]) level.sendParticles(ParticleTypes.SCULK_SOUL, wearer.getX(), wearer.getY() + 1, wearer.getZ(), 5, .25, .4, .25, .02);
        return hurt[0];
    }

    static void warn(LivingEntity wearer, int ticksLeft) {
        if (!(wearer instanceof ServerPlayer player)) return;
        int seconds = Math.max(1, ticksLeft / 20);
        String key = ticksLeft > 200 ? "hungry" : "starving";
        player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability." + SET + "." + key, seconds + "s"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                ticksLeft > 200 ? .8f : 1.4f, ticksLeft > 200 ? .6f : 1f);
    }

    static void fed(LivingEntity wearer) {
        if (wearer.level() instanceof ServerLevel level)
            level.sendParticles(ParticleTypes.SOUL, wearer.getX(), wearer.getY() + 1, wearer.getZ(), 8, .3, .5, .3, .03);
        if (wearer instanceof ServerPlayer player && TOLL.binds(wearer))
            player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability." + SET + ".fed"));
    }

    /** The toll is not paid: the Vestments take their wearer. Nothing saves them, not even a Totem. */
    public static void takeWearer(LivingEntity wearer) {
        if (!(wearer.level() instanceof ServerLevel level) || !cursable(wearer)) return;
        level.sendParticles(ParticleTypes.SCULK_SOUL, wearer.getX(), wearer.getY() + 1, wearer.getZ(), 40, .4, .8, .4, .08);
        level.playSound(null, wearer.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 2f, .5f);
        PaleEmperorFx.underworldArms(level, wearer.position(), wearer.getYRot(), 6);
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TOLL_DAMAGE));
        wearer.invulnerableTime = 0;
        wearer.hurtServer(level, source, Float.MAX_VALUE);
    }

    // =========================
    // Full set
    // =========================

    static void companion(LivingEntity wearer, boolean wanted) {
        boolean out = FollowerSummon.isActive(wearer, GUIDE);
        if (wanted && !out) SPIRIT_GUIDE.summon(wearer, GUIDE);
        else if (!wanted && out) FollowerSummon.dismiss(wearer, GUIDE);
    }

    /** Full set: life ebbs into the armor; past the last of it, the armor takes them. Otherwise it flows back. */
    static void ebb(LivingEntity wearer, ServerLevel level, boolean full) {
        if (!full) {
            if (MaxHealthDrain.drained(wearer, EBB) > 0 && (wearer.tickCount + wearer.getId()) % RECOVER_TICKS == 0)
                MaxHealthDrain.recover(wearer, EBB, EBB_STEP);
            return;
        }
        if ((wearer.tickCount + wearer.getId()) % EBB_TICKS == 0) ebbStep(wearer, level);
    }

    /** One step of the ebb: a point of max health into the armor, or, with nothing left to give, the wearer. */
    static void ebbStep(LivingEntity wearer, ServerLevel level) {
        if (MaxHealthDrain.wouldEmpty(wearer, EBB_STEP)) {
            takeWearer(wearer);
            return;
        }
        boolean first = MaxHealthDrain.drained(wearer, EBB) <= 0;
        MaxHealthDrain.drain(wearer, EBB, EBB_STEP);
        level.sendParticles(ParticleTypes.SOUL, wearer.getX(), wearer.getY() + 1.2, wearer.getZ(), 4, .3, .3, .3, .01);
        if (first && wearer instanceof ServerPlayer player)
            player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability." + SET + ".ebbing"));
    }
}
