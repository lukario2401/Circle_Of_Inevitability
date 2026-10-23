package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.Group;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Status;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Appraisal;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Gravity;
import net.lukario.circleofinevitability.status.Study;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Illuminator, key 7: the Civilization wheel (Knowledge Magister, Sequence 2; Illuminator, 1; Paragon, 0).
 * docs/ILLUMINATOR.md, "Key 7 wheel".
 *
 *   Rewrite Principles       the earth bears no weight: enemies float off the ground; combustion makes no flame
 *   Colossus                 a mech of brass and steam round you; cast again while it stands for the Fusion Beam
 *   Scrolls of Civilization  murals of history in a ring round you stop projectiles; then they fly out and immerse
 *   Mathematical Maze        an enemy is held in a lattice of equations and cannot use abilities; an ally is
 *                            Enlightened instead
 *   Masterwork               a symphony: allies stronger, enemies slower, until anything else is cast
 *   Reality Knowledge        knowledge rejects an enemy: no abilities (a mob fights only in melee)
 *   Illusory Book            an enemy and two near it are written into a book, gone from the world for a while
 *   Mechanical Heaven        a Divine Kingdom: constructs stronger and endless, allies' abilities cheaper, enemies
 *                            slowed, their gear worn, their cooldowns longer, their projectiles turned to static
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). Every entry's cost goes through
 * Illuminator.costFor (Quick Study). The looks are IlluminatorFx's; every number is right below.
 */
public final class IlluminatorCivilization {

    private IlluminatorCivilization() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int CIVILIZATION_SEQUENCE = 2;                 // the key
    private static final float QUICK_CAST_COST = 2f;            // a sneak recast: no cooldown, this many times the cost

    // Rewrite Principles
    private static final int PRINCIPLES_SEQUENCE = 2;
    private static final int PRINCIPLES_COST = 15000;           // Sequence 2 holds 78400
    private static final int PRINCIPLES_COOLDOWN = 800;
    private static final double PRINCIPLES_RANGE = 28.0;
    private static final double PRINCIPLES_RADIUS = 14.0;
    private static final int PRINCIPLES_TICKS = 240;
    private static final double LIFT = 0.12;                    // an enemy on the ground is lifted this fast (no gravity: status/Gravity)

    // Colossus
    private static final int COLOSSUS_SEQUENCE = 2;
    private static final int COLOSSUS_COST = 20000;
    private static final int COLOSSUS_COOLDOWN = 1800;
    private static final int COLOSSUS_TICKS = 800;
    private static final double COLOSSUS_SIZE = 1.5;            // the body grows this much (status/GiantForm)...
    private static final double COLOSSUS_REACH = 2.5;           // ...and reaches this much further
    private static final float COLOSSUS_ABSORPTION = 40f;       // its plating (status/ConjuredArmor): gone, the Colossus falls
    private static final double STEP_SHAKE = 6.0;              // its steps slow enemies this close
    private static final int FUSION_COOLDOWN = 160;             // the Fusion Beam, cast again while it stands
    private static final double FUSION_RANGE = 40.0;
    private static final double FUSION_WIDTH = 4.0;
    private static final float FUSION_DAMAGE = 120f;

    // Scrolls of Civilization
    private static final int SCROLLS_SEQUENCE = 1;
    private static final int SCROLLS_COST = 24000;
    private static final int SCROLLS_COOLDOWN = 600;
    private static final double SCROLLS_RADIUS = 5.0;
    private static final int SCROLLS_TICKS = 200;
    private static final float SCROLLS_OUTSIDE_CUT = 0.5f;      // hits from outside the ring do this much less
    private static final double SCROLLS_FLY = 12.0;             // then they fly this far...
    private static final int IMMERSED_TICKS = 60;               // ...and every enemy they reach is held this long...
    private static final float IMMERSED_DAMAGE = 40f;           // ...and hurt this much

    // Mathematical Maze / Enlightenment
    private static final int MAZE_SEQUENCE = 1;
    private static final int MAZE_COST = 28000;
    private static final int MAZE_COOLDOWN = 600;
    private static final double MAZE_RANGE = 24.0;
    private static final int MAZE_TICKS = 160;
    private static final double MAZE_HALF = 1.5;                // the cube is 3 blocks: held within this of its middle
    private static final float MAZE_TOUCH_DAMAGE = 10f;
    private static final int MAZE_TOUCH_EVERY = 10;
    private static final int ENLIGHTENED_TICKS = 600;
    private static final float ENLIGHTENED_COST = 0.8f;         // an Enlightened ally's abilities cost this much

    // Masterwork
    private static final int MASTERWORK_SEQUENCE = 1;
    private static final int MASTERWORK_COST = 30000;
    private static final int MASTERWORK_COOLDOWN = 1200;
    private static final double MASTERWORK_RADIUS = 20.0;
    private static final int MASTERWORK_TICKS = 200;

    // Reality Knowledge
    private static final int REALITY_SEQUENCE = 0;
    private static final int REALITY_COST = 40000;
    private static final int REALITY_COOLDOWN = 600;
    private static final double REALITY_RANGE = 32.0;
    private static final int REALITY_TICKS = 200;
    private static final int REALITY_BOSS_TICKS = 100;

    // Illusory Book
    private static final int BOOK_SEQUENCE = 0;
    private static final int BOOK_COST = 45000;
    private static final int BOOK_COOLDOWN = 900;
    private static final double BOOK_RANGE = 32.0;
    private static final double BOOK_GATHERS = 4.0;             // up to two more within this of the target
    private static final int BOOK_MOST = 3;
    private static final int BOOK_TICKS = 160;
    private static final int BOOK_PLAYER_TICKS = 100;
    private static final float BOOK_TOLL = 0.2f;                // they step out with this share of their health lost

    // Mechanical Heaven
    private static final int HEAVEN_SEQUENCE = 0;
    private static final int HEAVEN_COST = 60000;
    private static final int HEAVEN_COOLDOWN = 2400;
    private static final double HEAVEN_RADIUS = 40.0;
    private static final int HEAVEN_TICKS = 1200;
    private static final float HEAVEN_ALLY_COST = 0.7f;         // allies' abilities cost this much inside
    private static final float HEAVEN_ENEMY_COOLDOWN = 1.5f;    // enemies' cooldowns are this much longer inside
    private static final float HEAVEN_CONSTRUCTS = 1.5f;        // your constructs inside hit this much harder (and do not run out)
    private static final int HEAVEN_STATIC_EVERY = 40;          // one enemy projectile inside turns to static this often

    // =========================
    // Names and what is kept
    // =========================
    private static final String PRINCIPLES = "illuminator_principles";            // the Zones
    private static final String MASTERWORK = "illuminator_masterwork";
    static final String HEAVEN = "illuminator_heaven";
    private static final String LIFTED = "illuminator_lifted";                    // its Gravity
    private static final String COLOSSUS_ARMOR = "illuminator_colossus";
    private static final String COLOSSUS_UNTIL = "illuminator_colossus_until";
    private static final String SCROLLS_UNTIL = "illuminator_scrolls_until";
    static final String ENLIGHTENED_UNTIL = "illuminator_enlightened_until";
    private static final String CD_FUSION = "illuminator_fusion_beam";

    /** Who holds a Rewrite Principles or a Mechanical Heaven now (for the questions asked outside the zones). Not saved. */
    private static final Set<UUID> PRINCIPLE_OWNERS = new HashSet<>();
    private static final Set<UUID> HEAVEN_OWNERS = new HashSet<>();
    /** Where each Colossus stood a second ago (it strides when it moved). */
    private static final Map<UUID, Vec3> COLOSSUS_WAS = new HashMap<>();

    /** One held in a maze: its cube's middle, until when, whose, and under which tag. Not saved. */
    private static final class Maze {
        final LivingEntity target;
        final UUID caster;
        final Vec3 center;
        final long until;
        final String tag;
        long lastTouch;

        Maze(LivingEntity target, UUID caster, Vec3 center, long until, String tag) {
            this.target = target;
            this.caster = caster;
            this.center = center;
            this.until = until;
            this.tag = tag;
        }
    }
    private static final Map<UUID, Maze> MAZES = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("illuminator_civilization", "Civilization")
            .radius(92)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("rewrite_principles", IlluminatorFx.WHEEL_REWRITE_PRINCIPLES, "Rewrite Principles", PRINCIPLES_SEQUENCE,
                    PRINCIPLES_COST, PRINCIPLES_COOLDOWN, IlluminatorCivilization::rewritePrinciples)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "rewrite_principles", Group.CIVILIZATION))
            .entry("colossus", IlluminatorFx.WHEEL_COLOSSUS, "Colossus", COLOSSUS_SEQUENCE,
                    COLOSSUS_COST, COLOSSUS_COOLDOWN, IlluminatorCivilization::colossus)
            .free(IlluminatorCivilization::colossusStands)                        // the Fusion Beam: free, its own cooldown
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "colossus", Group.CIVILIZATION))
            .entry("scrolls_of_civilization", IlluminatorFx.WHEEL_SCROLLS_OF_CIVILIZATION, "Scrolls of Civilization", SCROLLS_SEQUENCE,
                    SCROLLS_COST, SCROLLS_COOLDOWN, IlluminatorCivilization::scrollsOfCivilization)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "scrolls_of_civilization", Group.CIVILIZATION))
            .entry("mathematical_maze", IlluminatorFx.WHEEL_MATHEMATICAL_MAZE, "Mathematical Maze", MAZE_SEQUENCE,
                    MAZE_COST, MAZE_COOLDOWN, IlluminatorCivilization::mathematicalMaze)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "mathematical_maze", Group.CIVILIZATION))
            .entry("masterwork", IlluminatorFx.WHEEL_MASTERWORK, "Masterwork", MASTERWORK_SEQUENCE,
                    MASTERWORK_COST, MASTERWORK_COOLDOWN, IlluminatorCivilization::masterwork)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "masterwork", Group.CIVILIZATION))
            .entry("reality_knowledge", IlluminatorFx.WHEEL_REALITY_KNOWLEDGE, "Reality Knowledge", REALITY_SEQUENCE,
                    REALITY_COST, REALITY_COOLDOWN, IlluminatorCivilization::realityKnowledge)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "reality_knowledge", Group.CIVILIZATION))
            .entry("illusory_book", IlluminatorFx.WHEEL_ILLUSORY_BOOK, "Illusory Book", BOOK_SEQUENCE,
                    BOOK_COST, BOOK_COOLDOWN, IlluminatorCivilization::illusoryBook)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "illusory_book", Group.CIVILIZATION))
            .entry("mechanical_heaven", IlluminatorFx.WHEEL_MECHANICAL_HEAVEN, "Mechanical Heaven", HEAVEN_SEQUENCE,
                    HEAVEN_COST, HEAVEN_COOLDOWN, IlluminatorCivilization::mechanicalHeaven)
            .free(player -> Zone.isOpen(player, HEAVEN))                         // casting it again ends it: free
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "mechanical_heaven", Group.CIVILIZATION))
            .onCast((player, sl, entry) -> Illuminator.afterCast(player, entry.key()));

    // =========================
    // Ability 7 - Civilization
    // Normal: the wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void illuminatorCivilization(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Illuminator.ready(player, bypassClassCheck, CIVILIZATION_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Rewrite Principles: over PRINCIPLES_RADIUS where you aim (PRINCIPLES_RANGE), from IlluminatorFx.PRINCIPLES_RISEN
    // for the rest of PRINCIPLES_TICKS (a Zone, twice a second): the earth bears no weight. Enemies have no gravity
    // (status/Gravity), are lifted off the ground, slowed (no sprint), and fall without damage when they leave or it
    // ends. Allies, you too: Slow Falling and Speed I. Combustion makes no flame: fire and explosions from enemies (or
    // from nobody) do not hurt your side inside (shelters). Cast again: the old one ends.
    // =========================
    private static AbilityWheel.Result rewritePrinciples(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, PRINCIPLES_RANGE);
        String tag = "il_principles_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, PRINCIPLES);
        IlluminatorFx.rewritePrinciples(sl, center, PRINCIPLES_RADIUS, PRINCIPLES_TICKS, tag);
        Later.run(sl, IlluminatorFx.PRINCIPLES_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                IlluminatorFx.principlesEnd(sl, center, PRINCIPLES_RADIUS, tag);
                return;
            }
            int[] beat = {0};
            PRINCIPLE_OWNERS.add(player.getUUID());
            Zone.at(center, PRINCIPLES_RADIUS)
                    .height(10)
                    .lasts(PRINCIPLES_TICKS - IlluminatorFx.PRINCIPLES_RISEN)
                    .every(10)
                    .affects(one -> true)
                    .onEnter(one -> {
                        if (!Spells.isEnemy(player, one)) return;
                        int left = Zone.ticksLeft(player, PRINCIPLES);
                        IlluminatorFx.lifted(sl, one, left, "il_lifted_" + one.getId());
                        if (one instanceof Player shown) IlluminatorFx.status(shown, Status.LIFTED, left);
                    })
                    .onInside((one, inside) -> {
                        if (Spells.isEnemy(player, one)) lift(one);
                        else weightless(one);
                    })
                    .onLeave(one -> {
                        if (Gravity.ticksLeft(one, LIFTED) <= 0) return;
                        Gravity.clear(one, LIFTED);
                        FallGuard.protect(one, 200);
                        if (one instanceof Player shown) IlluminatorFx.statusEnd(shown, Status.LIFTED);
                    })
                    .onPulse(ticksLeft -> {
                        if (Zone.covers(player, PRINCIPLES, player.position())) weightless(player);
                        if (beat[0]++ % 2 == 0) IlluminatorFx.principlesTick(sl, center, PRINCIPLES_RADIUS);
                    })
                    .onEnd(() -> {
                        PRINCIPLE_OWNERS.remove(player.getUUID());
                        IlluminatorFx.principlesEnd(sl, center, PRINCIPLES_RADIUS, tag);
                    })
                    .open(player, sl, PRINCIPLES);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void lift(LivingEntity enemy) {
        Gravity.set(enemy, LIFTED, 0.0, 15);
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 30);
        if (enemy.onGround()) {
            Vec3 motion = enemy.getDeltaMovement();
            enemy.setDeltaMovement(motion.x, LIFT, motion.z);
            enemy.hurtMarked = true;
        }
    }

    private static void weightless(LivingEntity ally) {
        Spells.keepEffect(ally, MobEffects.SLOW_FALLING, 0, 30);
        Spells.keepEffect(ally, MobEffects.SPEED, 0, 30);
    }

    /** Combustion makes no flame: true when this fire or explosion must not hurt 'victim' (a LivingAttackEvent). */
    static boolean shelters(LivingEntity victim, DamageSource source) {
        if (PRINCIPLE_OWNERS.isEmpty() || !(victim.level() instanceof ServerLevel sl)) return false;
        if (!source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        for (UUID id : PRINCIPLE_OWNERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null || !Spells.isFriend(owner, victim) || !Zone.covers(owner, PRINCIPLES, victim.position())) continue;
            Entity attacker = source.getEntity();
            if (attacker == null || (attacker instanceof LivingEntity living && Spells.isEnemy(owner, living))) return true;
        }
        return false;
    }

    // =========================
    // Colossus: a mech of brass and steam assembles round you (IlluminatorFx.colossus) for COLOSSUS_TICKS: you grow
    // COLOSSUS_SIZE (status/GiantForm) with COLOSSUS_REACH more reach, Strength III, Resistance II and COLOSSUS_ABSORPTION
    // plating (status/ConjuredArmor); every stride shakes the ground and slows enemies within STEP_SHAKE. It falls when
    // its plating is gone. While it stands, casting it again fires the Fusion Beam (free; FUSION_COOLDOWN of its own):
    // at IlluminatorFx.FUSION_FIRES a line FUSION_RANGE long and FUSION_WIDTH wide, FUSION_DAMAGE to every enemy on it.
    // =========================
    private static AbilityWheel.Result colossus(ServerPlayer player, ServerLevel sl) {
        if (colossusStands(player)) return fusionBeam(player, sl);
        int ticks = COLOSSUS_TICKS;
        String tag = colossusTag(player);
        player.getPersistentData().putLong(COLOSSUS_UNTIL, sl.getGameTime() + ticks);
        IlluminatorFx.colossus(sl, player, ticks, tag);
        IlluminatorFx.status(player, Status.COLOSSUS, ticks);
        GiantForm.of(COLOSSUS_SIZE).reach(COLOSSUS_REACH).stepHeight(1.0).lasts(ticks)
                .onEnd((giant, why) -> {
                    if (giant instanceof Player stood) colossusFalls(stood);
                })
                .grow(player);
        ConjuredArmor.of(COLOSSUS_ARMOR)
                .lasts(ticks)
                .absorption(COLOSSUS_ABSORPTION)
                .onHit((hurt, amount) -> Later.run(sl, 1, () -> {
                    if (hurt.getAbsorptionAmount() <= 0.01f && hurt instanceof Player stood) colossusFalls(stood);
                }))
                .onEnd(wearer -> {
                    if (wearer instanceof Player stood) colossusFalls(stood);
                })
                .wear(player);
        COLOSSUS_WAS.put(player.getUUID(), player.position());
        return AbilityWheel.Result.DONE;
    }

    static boolean colossusStands(Player player) {
        return Illuminator.isActive(player, COLOSSUS_UNTIL);
    }

    private static String colossusTag(Player player) {
        return "il_colossus_" + player.getId();
    }

    /** The Colossus comes apart, however it ended (runs once). */
    private static void colossusFalls(Player player) {
        if (!player.getPersistentData().contains(COLOSSUS_UNTIL)) return;
        player.getPersistentData().remove(COLOSSUS_UNTIL);
        COLOSSUS_WAS.remove(player.getUUID());
        if (GiantForm.isGiant(player)) GiantForm.end(player);
        ConjuredArmor.takeOff(player, COLOSSUS_ARMOR);
        player.removeEffect(MobEffects.STRENGTH);
        player.removeEffect(MobEffects.RESISTANCE);
        IlluminatorFx.statusEnd(player, Status.COLOSSUS);
        if (player.level() instanceof ServerLevel sl) IlluminatorFx.colossusEnd(sl, player, colossusTag(player));
    }

    private static AbilityWheel.Result fusionBeam(ServerPlayer player, ServerLevel sl) {
        if (!Cooldowns.ready(player, CD_FUSION, "The Fusion Beam")) return AbilityWheel.Result.REFUSED;
        Cooldowns.start(player, CD_FUSION, FUSION_COOLDOWN);
        int left = (int) Math.max(1, player.getPersistentData().getLongOr(COLOSSUS_UNTIL, 0L) - sl.getGameTime());
        IlluminatorFx.fusionCharge(sl, player, left, colossusTag(player));
        Later.run(sl, IlluminatorFx.FUSION_FIRES, () -> {
            if (!Spells.casterStillHere(player, sl) || !colossusStands(player)) return;
            Vec3 from = player.getEyePosition().add(Spells.flatLook(player).scale(1.2));
            Vec3 to = Spells.aimPoint(player, sl, FUSION_RANGE);
            IlluminatorFx.fusionBeam(sl, from, to, FUSION_WIDTH);
            for (LivingEntity enemy : Spells.enemiesOnLine(player, sl, from, to, FUSION_WIDTH / 2.0)) {
                Illuminator.hit(player, enemy, FUSION_DAMAGE, null);
            }
        });
        return AbilityWheel.Result.FREE;
    }

    /** Every second while the Colossus stands: its strength, and a stride when it moved. */
    private static void colossusTick(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(COLOSSUS_UNTIL)) return;
        if (!colossusStands(player)) {
            colossusFalls(player);
            return;
        }
        if (player.tickCount % 20 != 0) return;
        Spells.keepEffect(player, MobEffects.STRENGTH, 2, 60);
        Spells.keepEffect(player, MobEffects.RESISTANCE, 1, 60);
        Vec3 was = COLOSSUS_WAS.put(player.getUUID(), player.position());
        if (was == null || was.subtract(player.position()).horizontalDistanceSqr() < 1.0) return;
        int left = (int) Math.max(1, player.getPersistentData().getLongOr(COLOSSUS_UNTIL, 0L) - sl.getGameTime());
        IlluminatorFx.colossusStride(sl, player, left, colossusTag(player));
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), STEP_SHAKE)) {
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 30);
        }
    }

    // =========================
    // Scrolls of Civilization: murals unfold in a ring SCROLLS_RADIUS round you for SCROLLS_TICKS. Projectiles of
    // anyone not on your side that come inside it stop there, and hits on you from anyone standing outside it do
    // SCROLLS_OUTSIDE_CUT less. When it ends, the scrolls fly out SCROLLS_FLY: every enemy they reach (at
    // IlluminatorFx.SCROLLS_FLY) is immersed in a vision, held completely for IMMERSED_TICKS and hurt IMMERSED_DAMAGE.
    // =========================
    private static AbilityWheel.Result scrollsOfCivilization(ServerPlayer player, ServerLevel sl) {
        long until = sl.getGameTime() + SCROLLS_TICKS;
        player.getPersistentData().putLong(SCROLLS_UNTIL, until);
        String tag = "il_scrolls_" + player.getId() + "_" + sl.getGameTime();
        IlluminatorFx.scrollsOfCivilization(sl, player, SCROLLS_RADIUS, SCROLLS_TICKS, tag);
        Later.run(sl, SCROLLS_TICKS, () -> {
            if (player.getPersistentData().getLongOr(SCROLLS_UNTIL, 0L) != until) return;      // cast again meanwhile
            player.getPersistentData().remove(SCROLLS_UNTIL);
            if (!Spells.casterStillHere(player, sl)) return;
            IlluminatorFx.scrollsRelease(sl, player, SCROLLS_FLY, tag);
            Vec3 center = player.position();
            Later.run(sl, IlluminatorFx.SCROLLS_FLY, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, center.add(0, 1, 0), SCROLLS_FLY)) {
                    Root.apply(enemy, IMMERSED_TICKS, RootRestriction.EVERYTHING);
                    IlluminatorFx.immersed(sl, enemy, IMMERSED_TICKS, "il_immersed_" + enemy.getId());
                    if (enemy instanceof Player shown) IlluminatorFx.status(shown, Status.IMMERSED, IMMERSED_TICKS);
                    Illuminator.hit(player, enemy, IMMERSED_DAMAGE, null);
                }
            });
        });
        return AbilityWheel.Result.DONE;
    }

    /** Every tick while the murals stand: the projectiles of those not on your side that came inside stop. */
    private static void scrollsTick(Player player, ServerLevel sl) {
        if (!Illuminator.isActive(player, SCROLLS_UNTIL)) return;
        Vec3 middle = player.position().add(0, 1, 0);
        AABB around = new AABB(middle, middle).inflate(SCROLLS_RADIUS);
        for (Projectile projectile : sl.getEntitiesOfClass(Projectile.class, around, Entity::isAlive)) {
            if (projectile.position().distanceToSqr(middle) > SCROLLS_RADIUS * SCROLLS_RADIUS) continue;
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living && Spells.isFriend(player, living)) continue;
            IlluminatorFx.scrollsStop(sl, projectile.position());
            projectile.discard();
        }
    }

    // =========================
    // Mathematical Maze: aimed at an enemy (MAZE_RANGE), a lattice of equations closes round it at
    // IlluminatorFx.MAZE_CLOSED for MAZE_TICKS: it is held inside a 3 block cube (it is put back when it reaches the
    // lattice, and the lattice hurts MAZE_TOUCH_DAMAGE at most every MAZE_TOUCH_EVERY), and lost in thought: no
    // abilities (status/AbilityLock; a mob fights only in melee). Aimed at an ally instead (Spells.aimAlly):
    // Enlightenment for ENLIGHTENED_TICKS: Haste II, every ability ENLIGHTENED_COST as dear (Spells.addCostScale),
    // harmful effects cleansed.
    // =========================
    private static AbilityWheel.Result mathematicalMaze(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, MAZE_RANGE);
        if (target == null) {
            LivingEntity ally = Spells.aimAlly(player, sl, MAZE_RANGE);
            if (ally == null) {
                player.sendSystemMessage(Component.literal("There is no one in sight for the equations."));
                return AbilityWheel.Result.REFUSED;
            }
            enlighten(player, sl, ally);
            return AbilityWheel.Result.DONE;
        }
        String tag = "il_maze_" + target.getId() + "_" + sl.getGameTime();
        IlluminatorFx.mathematicalMaze(sl, player, target, MAZE_TICKS, tag);
        Vec3 center = target.position();
        Later.run(sl, IlluminatorFx.MAZE_CLOSED, () -> {
            if (!target.isAlive() || target.level() != sl) {
                IlluminatorFx.mazeEnd(sl, center, tag);
                return;
            }
            int ticks = MAZE_TICKS - IlluminatorFx.MAZE_CLOSED;
            AbilityLock.apply(target, ticks);
            Maze old = MAZES.put(target.getUUID(), new Maze(target, player.getUUID(), center, sl.getGameTime() + ticks, tag));
            if (old != null) IlluminatorFx.mazeEnd(sl, old.center, old.tag);
            if (target instanceof Player held) IlluminatorFx.status(held, Status.MAZE, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void enlighten(Player player, ServerLevel sl, LivingEntity ally) {
        String tag = "il_enlightened_" + ally.getId();
        ally.addEffect(new MobEffectInstance(MobEffects.HASTE, ENLIGHTENED_TICKS, 1));
        Spells.cleanse(ally);
        ally.getPersistentData().putLong(ENLIGHTENED_UNTIL, sl.getGameTime() + ENLIGHTENED_TICKS);
        IlluminatorFx.enlightenment(sl, player, ally, ENLIGHTENED_TICKS, tag);
        if (ally instanceof Player shown) IlluminatorFx.status(shown, Status.ENLIGHTENED, ENLIGHTENED_TICKS);
    }

    /** What every ability costs an Enlightened player (Spells.addCostScale). */
    static float enlightenedCost(Player player) {
        return Illuminator.isActive(player, ENLIGHTENED_UNTIL) ? ENLIGHTENED_COST : 1f;
    }

    /** Every tick, for the caster: the ones they hold in a maze stay in its cube. */
    private static void mazeTick(Player player, ServerLevel sl) {
        if (MAZES.isEmpty()) return;
        long now = sl.getGameTime();
        for (Maze maze : new ArrayList<>(MAZES.values())) {
            if (!maze.caster.equals(player.getUUID())) continue;
            LivingEntity held = maze.target;
            if (now >= maze.until || !held.isAlive() || held.level() != sl) {
                MAZES.remove(held.getUUID());
                IlluminatorFx.mazeEnd(sl, maze.center, maze.tag);
                if (held instanceof Player shown) IlluminatorFx.statusEnd(shown, Status.MAZE);
                continue;
            }
            Vec3 at = held.position();
            boolean out = Math.abs(at.x - maze.center.x) > MAZE_HALF || Math.abs(at.z - maze.center.z) > MAZE_HALF
                    || at.y - maze.center.y > MAZE_HALF * 2 || maze.center.y - at.y > 1.0;
            if (!out) continue;
            held.teleportTo(maze.center.x, maze.center.y, maze.center.z);
            held.setDeltaMovement(Vec3.ZERO);
            held.hurtMarked = true;
            if (now - maze.lastTouch >= MAZE_TOUCH_EVERY) {
                maze.lastTouch = now;
                IlluminatorFx.mazeTouch(sl, at.add(0, 1, 0));
                Illuminator.hit(player, held, MAZE_TOUCH_DAMAGE, null);
            }
        }
    }

    // =========================
    // Masterwork: you conduct a symphony for MASTERWORK_TICKS (a Zone round you, MASTERWORK_RADIUS, twice a second).
    // Allies, you too: Strength I, Haste I, Regeneration I. Enemies: Slowness II, Weakness I (no sprint). Casting
    // anything else stops it (castSomething, through Illuminator.addAfterCast).
    // =========================
    private static AbilityWheel.Result masterwork(ServerPlayer player, ServerLevel sl) {
        String tag = "il_masterwork_" + player.getId();
        Zone.close(player, MASTERWORK);
        IlluminatorFx.masterwork(sl, player, MASTERWORK_RADIUS, MASTERWORK_TICKS, tag);
        IlluminatorFx.status(player, Status.MASTERWORK, MASTERWORK_TICKS);
        Zone.around(player, MASTERWORK_RADIUS)
                .height(8)
                .lasts(MASTERWORK_TICKS)
                .every(10)
                .affects(one -> true)
                .onInside((one, inside) -> {
                    if (Spells.isEnemy(player, one)) {
                        Spells.keepEffect(one, MobEffects.SLOWNESS, 1, 30);
                        Spells.keepEffect(one, MobEffects.WEAKNESS, 0, 30);
                    } else {
                        inspired(one);
                    }
                })
                .onPulse(ticksLeft -> {
                    inspired(player);
                    IlluminatorFx.masterworkTick(sl, player, MASTERWORK_RADIUS);
                })
                .onEnd(() -> {
                    IlluminatorFx.masterworkEnd(sl, player, tag);
                    IlluminatorFx.statusEnd(player, Status.MASTERWORK);
                })
                .open(player, sl, MASTERWORK);
        return AbilityWheel.Result.DONE;
    }

    private static void inspired(LivingEntity ally) {
        Spells.keepEffect(ally, MobEffects.STRENGTH, 0, 30);
        Spells.keepEffect(ally, MobEffects.HASTE, 0, 30);
        Spells.keepEffect(ally, MobEffects.REGENERATION, 0, 60);
    }

    /** Anything cast that is not Masterwork stops the music (Illuminator.addAfterCast). */
    static void castSomething(Player player, String ability) {
        if (!"masterwork".equals(ability) && Zone.isOpen(player, MASTERWORK)) Zone.close(player, MASTERWORK);
    }

    // =========================
    // Reality Knowledge: aimed at an enemy (REALITY_RANGE), at IlluminatorFx.REALITY_LANDS knowledge rejects it for
    // REALITY_TICKS (REALITY_BOSS_TICKS for a boss): a player cannot use Beyonder abilities, a mob loses its ranged and
    // special attacks and fights only in melee (status/AbilityLock). It also ends the reading done on you: every mark
    // on you is wiped (status/Appraisal) and a player target forgets what they had studied of you (status/Study).
    // =========================
    private static AbilityWheel.Result realityKnowledge(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, REALITY_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one in sight for knowledge to reject."));
            return AbilityWheel.Result.REFUSED;
        }
        if (AbilityLock.isImmune(target)) {
            if (target instanceof Player paragon) IlluminatorFx.realityGuard(sl, paragon);
            player.sendSystemMessage(Component.literal("Its knowledge cannot be touched."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks = Illuminator.isBoss(target) ? REALITY_BOSS_TICKS : REALITY_TICKS;
        String tag = "il_forgotten_" + target.getId();
        IlluminatorFx.realityKnowledge(sl, player, target, ticks, tag);
        Appraisal.wipe(player);
        if (target instanceof Player reader) Study.forget(reader, player);
        Later.run(sl, IlluminatorFx.REALITY_LANDS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            int left = ticks - IlluminatorFx.REALITY_LANDS;
            AbilityLock.apply(target, left);
            if (target instanceof Player forgot) {
                IlluminatorFx.status(forgot, Status.FORGOTTEN, left);
                forgot.sendSystemMessage(Component.literal("You cannot remember how."));
            }
            Later.run(sl, left, () -> IlluminatorFx.forgottenEnd(sl, target, tag));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Illusory Book: aimed at an enemy (BOOK_RANGE) that is not a boss, a book opens over it; at IlluminatorFx.BOOK_OPEN
    // it and up to BOOK_MOST - 1 more (not bosses) within BOOK_GATHERS are written in: gone from the world (status/Exile:
    // hidden, untouchable, unable to act) for BOOK_TICKS (BOOK_PLAYER_TICKS for players), then they step out under the
    // book, BOOK_TOLL of their health lost (nothing softens it).
    // =========================
    private static AbilityWheel.Result illusoryBook(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, BOOK_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one in sight to write down."));
            return AbilityWheel.Result.REFUSED;
        }
        if (Illuminator.isBoss(target)) {
            player.sendSystemMessage(Component.literal("It is too great to be written."));
            return AbilityWheel.Result.REFUSED;
        }
        List<LivingEntity> victims = new ArrayList<>();
        victims.add(target);
        for (LivingEntity near : Spells.enemiesAround(player, sl, target.position(), BOOK_GATHERS)) {
            if (victims.size() >= BOOK_MOST) break;
            if (near != target && !Illuminator.isBoss(near)) victims.add(near);
        }
        Vec3 book = target.position().add(0, target.getBbHeight() + 1.5, 0);
        Vec3 ground = Spells.groundAt(player, sl, target.position().add(0, 0.5, 0));
        String tag = "il_book_" + player.getId() + "_" + sl.getGameTime();
        IlluminatorFx.illusoryBook(sl, book, player.getYRot() + 180f, BOOK_TICKS, tag);
        Later.run(sl, IlluminatorFx.BOOK_OPEN, () -> {
            for (LivingEntity victim : victims) {
                if (!victim.isAlive() || victim.level() != sl) continue;
                int ticks = victim instanceof Player ? BOOK_PLAYER_TICKS : BOOK_TICKS;
                IlluminatorFx.written(sl, victim, book);
                if (!Exile.banish(victim, ticks, ground, (who, level) -> stepsOut(player, level, who))) continue;
                if (victim instanceof Player written) IlluminatorFx.status(written, Status.WRITTEN, ticks);
            }
            Later.run(sl, BOOK_TICKS, () -> IlluminatorFx.bookRelease(sl, book, tag));
        });
        return AbilityWheel.Result.DONE;
    }

    /** One written into the book is back: the toll. */
    private static void stepsOut(Player player, ServerLevel sl, LivingEntity who) {
        if (who instanceof Player shown) IlluminatorFx.statusEnd(shown, Status.WRITTEN);
        if (!who.isAlive() || !Spells.casterStillHere(player, sl)) return;
        Appraisal.strikeTrue(player, who, who.getHealth() * BOOK_TOLL, null);
    }

    // =========================
    // Mechanical Heaven: a Divine Kingdom HEAVEN_RADIUS round where you stand, for HEAVEN_TICKS (a Zone, once a
    // second), risen at IlluminatorFx.HEAVEN_RISEN. Your constructs inside (status/Construct) hit HEAVEN_CONSTRUCTS
    // harder and do not run out. Allies inside spend HEAVEN_ALLY_COST on abilities (Spells.addCostScale). Enemies
    // inside: Slowness I, their armor and weapons lose 1 durability a second, their cooldowns are HEAVEN_ENEMY_COOLDOWN
    // as long (combat/Cooldowns.addScale), and every HEAVEN_STATIC_EVERY one of their projectiles inside collapses into
    // static. Cast again: it ends early (free).
    // =========================
    private static AbilityWheel.Result mechanicalHeaven(ServerPlayer player, ServerLevel sl) {
        if (Zone.isOpen(player, HEAVEN)) {
            Zone.close(player, HEAVEN);
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = player.position();
        String tag = "il_heaven_" + player.getId() + "_" + sl.getGameTime();
        IlluminatorFx.mechanicalHeaven(sl, player, HEAVEN_RADIUS, HEAVEN_TICKS, tag);
        IlluminatorFx.status(player, Status.HEAVEN, HEAVEN_TICKS);
        HEAVEN_OWNERS.add(player.getUUID());
        int[] beat = {0};
        Zone.at(center, HEAVEN_RADIUS)
                .height(40)
                .lasts(HEAVEN_TICKS)
                .every(20)
                .onInside((enemy, inside) -> {
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 40);
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                        ItemStack worn = enemy.getItemBySlot(slot);
                        if (!worn.isEmpty() && worn.isDamageableItem()) worn.hurtAndBreak(1, enemy, slot);
                    }
                })
                .onPulse(ticksLeft -> {
                    if (beat[0] * 20 >= IlluminatorFx.HEAVEN_RISEN) IlluminatorFx.heavenTick(sl, center, HEAVEN_RADIUS);
                    for (Mob made : Construct.ofOwner(player, null)) {
                        if (Zone.covers(player, HEAVEN, made.position())) Construct.extend(made, 20);
                    }
                    if ((beat[0]++ * 20) % HEAVEN_STATIC_EVERY == 0) heavenStatic(player, sl, center);
                })
                .onEnd(() -> {
                    HEAVEN_OWNERS.remove(player.getUUID());
                    IlluminatorFx.heavenEnd(sl, center, HEAVEN_RADIUS, tag);
                    IlluminatorFx.statusEnd(player, Status.HEAVEN);
                })
                .open(player, sl, HEAVEN);
        return AbilityWheel.Result.DONE;
    }

    /** One projectile of an enemy inside the Kingdom collapses into static. */
    private static void heavenStatic(Player player, ServerLevel sl, Vec3 center) {
        AABB inside = new AABB(center, center).inflate(HEAVEN_RADIUS, 40, HEAVEN_RADIUS);
        for (Projectile projectile : sl.getEntitiesOfClass(Projectile.class, inside, Entity::isAlive)) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity living && Spells.isFriend(player, living)) continue;
            if (!Zone.covers(player, HEAVEN, projectile.position())) continue;
            IlluminatorFx.heavenStatic(sl, projectile.position());
            projectile.discard();
            return;
        }
    }

    /** What every ability costs a player who stands in an ally's Mechanical Heaven (Spells.addCostScale). */
    static float heavenCost(Player player) {
        Player owner = heavenOver(player);
        return owner != null && Spells.isFriend(owner, player) ? HEAVEN_ALLY_COST : 1f;
    }

    /** How long the cooldowns of a player in an enemy's Mechanical Heaven are (Cooldowns.addScale). */
    static float heavenCooldown(Player player) {
        Player owner = heavenOver(player);
        return owner != null && Spells.isEnemy(owner, player) ? HEAVEN_ENEMY_COOLDOWN : 1f;
    }

    /** The owner of a Mechanical Heaven that covers this player, or null. */
    private static Player heavenOver(Player player) {
        if (HEAVEN_OWNERS.isEmpty() || !(player.level() instanceof ServerLevel sl)) return null;
        for (UUID id : HEAVEN_OWNERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner != null && Zone.covers(owner, HEAVEN, player.position())) return owner;
        }
        return null;
    }

    // =========================
    // Kept going and tidy by Illuminator
    // =========================

    /** Every tick, for every player: the Colossus, the murals and the mazes they hold. */
    static void tick(Player player, ServerLevel sl) {
        colossusTick(player, sl);
        scrollsTick(player, sl);
        mazeTick(player, sl);
    }

    /**
     * A hit landed (LivingHurtEvent): hits from outside the murals on their owner are cut; a construct's hit inside its
     * owner's Mechanical Heaven is stronger.
     */
    static void hurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Entity attacker = event.getSource().getEntity();
        if (victim instanceof Player held && Illuminator.isActive(held, SCROLLS_UNTIL) && attacker != null
                && attacker.distanceTo(held) > SCROLLS_RADIUS) {
            event.setAmount(event.getAmount() * (1f - SCROLLS_OUTSIDE_CUT));
        }
        if (HEAVEN_OWNERS.isEmpty() || attacker == null || !Construct.isBuilt(attacker)) return;
        UUID ownerId = Construct.ownerOf(attacker);
        if (ownerId == null || !HEAVEN_OWNERS.contains(ownerId) || !(attacker.level() instanceof ServerLevel sl)) return;
        Player owner = sl.getPlayerByUUID(ownerId);
        if (owner != null && Zone.covers(owner, HEAVEN, attacker.position())) event.setAmount(event.getAmount() * HEAVEN_CONSTRUCTS);
    }

    /** A player left: the mazes they held end where they are. */
    static void forget(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        for (Maze maze : new ArrayList<>(MAZES.values())) {
            if (!maze.caster.equals(player.getUUID())) continue;
            MAZES.remove(maze.target.getUUID());
            IlluminatorFx.mazeEnd(sl, maze.center, maze.tag);
        }
        COLOSSUS_WAS.remove(player.getUUID());
    }

    /** After a relog or a death: no Colossus or murals left over. */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(COLOSSUS_UNTIL)) colossusFalls(player);
        data.remove(SCROLLS_UNTIL);
        forget(player);
    }
}
