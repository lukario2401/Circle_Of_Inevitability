package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimePowers;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimeFx;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.MeleeCombo;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.ParasiteControl;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class WormOfTime {

    private static final String PATHWAY = "Worm Of Time";

    static {
        // Single-key casts are limited by spirituality; AbilityWheel owns its separate cooldowns.
        Cooldowns.addFree(Cooldowns.keysStartingWith("worm_of_time_"));
    }

    private static final int COMBO_HEAD = 0xF2F8FFFF;
    private static final int COMBO_MID  = 0xEE1BB8E8;
    private static final int COMBO_TAIL = 0xA00A1022;

    // Shared by the theft and parasite transitions; melee impacts live in WormOfTimeFx.
    private static final SlashFx COMBO_RING = SlashFx.arc("slash/band")
            .color(COMBO_MID).tailColor(COMBO_TAIL).headColor(COMBO_HEAD)
            .translucent().radius(3.6f).arc(320f).width(1f).taper(SlashFx.Taper.COMET)
            .layers(4).spread(0.28f).lifetime(12).sweep(5).spin(10f);
    private static final ParticleFx COMBO_STAR = ParticleFx.of("fx/spark")
            .color(0xFFB8FFEE).fadeOut().size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(28, 14).friction(0.96f).glow().spin(12f).randomRotation();
    private static final ParticleFx COMBO_STAR_WHITE = COMBO_STAR.color(0xFFFFFFFF).fadeOut();

    // =========================
    // Ability 2 - Theft: the numbers and the looks
    // =========================
    private static final int THEFT_SEQUENCE = 6;
    private static final int THEFT_COST = 1500;                 // for stealing. Releasing costs nothing
    private static final double THEFT_RANGE = 32;
    private static final int THEFT_HELD_TICKS = 1200*20;          // a stolen ability is held for twenty minutes at most
    private static final int THEFT_MAX_HELD = 5;               // how many stolen abilities can be held at once (0 = no limit).
                                                               // When full, a new theft is refused until one is released
    private static final int THEFT_MOB_EMPTY_TICKS = 600;      // a mob that was stolen from has nothing more to give for this long (0 = no limit)
    private static final int THEFT_FLIGHT_TICKS = 10;          // how long the stolen light takes to reach the thief
    private static final float THEFT_ORB_SCALE = 0.28f;        // the lights circling over the thief's head: their size,
    private static final double THEFT_ORB_RADIUS = 0.5;        // how far from the middle they circle (blocks),
    private static final float THEFT_ORB_SPIN = 6f;            // and how fast (degrees per tick)
    private static final String THEFT_EMPTY = "worm_of_time_emptied";    // the mark (status/Marks) on a mob that was stolen from
    private static final String THEFT_ORB_TAG = "worm_of_time_stolen_";  // + the slot: the tag of one light over the head

    private static final SlashFx THEFT_THREAD = SlashFx.line("slash/smooth")
            .color(COMBO_MID).core(0xE0FFFFFF)
            .width(0.09f).taper(SlashFx.Taper.UNIFORM);
    private static final ModelFx THEFT_LIGHT = ModelFx.of("orb")
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .color(0xF08CFFE6);

    // =========================
    // Ability 3 - Parasite: the numbers and the looks
    // (what it DOES to the host is further down: "Parasite: what it does")
    // =========================
    private static final String PARASITE_INITIAL = "worm_of_time_initial_parasitism";
    private static final int PARASITE_KEY = 3;                    // J
    private static final int PARASITE_SEQUENCE = 4;
    private static final int PARASITE_COST = 6000;                // for going in. Leaving costs nothing
    private static final int PARASITE_KEY_REST = 20;              // the key does nothing for this long after going in and after coming out:
                                                                  // a held key repeats itself, and would go in and straight back out
    private static final double PARASITE_RANGE = 6;               // how near the host has to be (blocks)
    private static final int PARASITE_MAX_TICKS = 2400;           // two minutes inside at most (0 = no limit)
    private static final int PARASITE_BEAT = 20;                  // ticks between two beats: each beat costs the upkeep and drains the host
    private static final int PARASITE_UPKEEP = 2600;              // spirituality at each beat (0 = free). Sequence 4 regains 2160 a second,
                                                                  // so it slowly runs dry; from Sequence 3 on only the time limit ends it
    private static final float PARASITE_DRAIN = 2f;               // health the host loses at each beat (2 = one heart, 0 = no drain)
    private static final float PARASITE_DRAIN_GAIN = 1f;          // how much of that the parasite gains (1 = all of it)
    private static final float PARASITE_DRAIN_LEAVES = 2f;        // the drain never takes the host below this (0 = it can kill its host)
    private static final float PARASITE_THROWN_OUT_DAMAGE = 8f;   // what the parasite takes when its host dies (0 = nothing)
    private static final boolean PARASITE_HOST_IS_TOLD = true;    // a host that is a player gets a chat line and a clock
    private static final int PARASITE_FLIGHT_TICKS = 8;           // looks only: how long the light takes to reach the host
    private static final int PARASITE_COLOR = 0x52E3C4;           // the clocks by the hotbar
    private static final String PARASITE_CLOCK = "worm_of_time_parasite";          // the parasite's clock (combat/HudTimer)
    private static final String PARASITE_HOST_CLOCK = "worm_of_time_parasitized";  // the host's clock
    private static final String PARASITE_RESTING = "worm_of_time_parasite_rest";   // the mark (status/Marks) of someone who just came out
    // steering the host (status/ParasiteControl): mobs only, a player host just carries the parasite
    private static final boolean PARASITE_STEERS_MOBS = true;     // false = every host only carries the parasite
    private static final int PARASITE_STEER_IDLE_TICKS = 40;      // two seconds without input: the host acts on its own again
    private static final double PARASITE_WALK_SPEED = 1.0;        // 1 = the host's normal walking speed
    private static final double PARASITE_SPRINT_SPEED = 1.4;      // while sprint is held
    private static final double PARASITE_STRIKE_REACH = 3.0;      // left click: how far a hit reaches (blocks from its eyes)
    private static final int PARASITE_STRIKE_REST = 20;           // ticks between two hits
    private static final float PARASITE_BARE_DAMAGE = 2f;         // a hit of a host that has no attack of its own (a cow)
    private static final double PARASITE_SHOT_RANGE = 24;         // left click with a host that shoots (skeleton, pillager, witch)
    private static final int PARASITE_SHOT_REST = 30;             // ticks between two shots
    private static final double PARASITE_HUNT_RANGE = 24;         // right click: how far away it can be told to hunt something
    private static final ParasiteControl.Settings PARASITE_STEERING = new ParasiteControl.Settings(
            PARASITE_STEER_IDLE_TICKS, PARASITE_WALK_SPEED, PARASITE_SPRINT_SPEED, PARASITE_STRIKE_REACH,
            PARASITE_STRIKE_REST, PARASITE_BARE_DAMAGE, PARASITE_SHOT_RANGE, PARASITE_SHOT_REST, PARASITE_HUNT_RANGE);

    /** Hands status/Parasite the methods under "Parasite: what it does". */
    private static final Parasite.Hooks PARASITE_HOOKS = new Parasite.Hooks() {
        @Override
        public void entered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {
            parasiteEntered(parasite, host, sl);
        }

        @Override
        public void tick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {
            parasiteTick(parasite, host, sl, ticksInside);
        }

        @Override
        public void left(ServerPlayer parasite, LivingEntity host, ServerLevel sl, Parasite.End why) {
            parasiteLeft(parasite, host, sl, why);
        }

        @Override
        public boolean allowsKey(int key) {
            return key == PARASITE_KEY;               // the only ability key that works from inside a host
        }

        @Override
        public ParasiteControl.Settings control(LivingEntity host) {
            ServerPlayer inside = Parasite.parasiteIn(host);
            return PARASITE_STEERS_MOBS && inside != null
                    && !inside.getPersistentData().getBooleanOr(PARASITE_INITIAL, false) ? PARASITE_STEERING : null;     // (only asked for a mob)
        }

        @Override
        public void struck(ServerPlayer parasite, LivingEntity host, LivingEntity target, ServerLevel sl) {
            parasiteStruck(parasite, host, target, sl);
        }
    };

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step(WormOfTimeFx::openingCut)
            .step(WormOfTimeFx::returnCut)
            .step(WormOfTimeFx::risingCut)
            .step(WormOfTimeFx::fallingCut)
            .step(WormOfTimeFx::crossCut)
            .randomStep(WormOfTimeFx::clockBreak, WormOfTimeFx::timeFracture);

    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!WormOfTimePowers.ready(player, bypassClassCheck, 9)) return;
        WormOfTimePowers.marauder(player, sl);
    }

    // =========================
    // Ability 2 - Theft
    // Normal: steals an ability from what you look at and holds it. From a player with a Pathway: one of the
    //         abilities their Sequence gives them, and they cannot use it while you hold it. From a Beyonder mob
    //         (blasphemy_slate/beyonder_mobs): one of its Pathway's, up to its Sequence. Other mobs have nothing.
    // Sneak:  releases the newest one you hold: it is cast once, whatever its Sequence, at no cost.
    // Sequence 6 unlocks power theft; Sequence 3 takes up to three different powers in one attempt.
    // Who holds what is kept by StolenAbilities; the list of abilities is PathwayAbilities.
    // =========================
    public static void wormOfTimeAbility2(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (!Beyonder.hasSequence(player, THEFT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) releaseStolen(player, level, sl);
        else steal(player, sl);
    }

    private static void steal(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, "worm_of_time_power_theft", "Power Theft")) return;
        LivingEntity target = Spells.aimEnemy(player, sl, Beyonder.hasSequence(player, 5) ? 48 : THEFT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return;                                                  // nothing spent
        }
        if (StolenAbilities.isFull(player, THEFT_MAX_HELD)) {
            player.sendSystemMessage(Component.literal("You cannot hold more than " + THEFT_MAX_HELD
                    + " stolen abilities. Release one first (sneak)."));
            return;
        }
        PathwayAbilities.Ability ability = pickToSteal(player, target, Set.of());
        if (ability == null) {
            String why;
            if (hasPathway(target)) why = "They have nothing you can steal right now.";
            else if (!BeyonderMobs.isBeyonder(target)) why = "It is no Beyonder. There is no power in it to steal.";
            else if (Marks.count(target, THEFT_EMPTY) > 0) why = "It has nothing left to give. Look for another.";
            else why = "Its powers cannot be stolen.";
            player.sendSystemMessage(Component.literal(why));
            return;
        }
        if (!Spells.paySpirituality(player, THEFT_COST)) return;
        Cooldowns.start(player, "worm_of_time_power_theft", 60);
        int gap = Math.max(0, SoulCore.getSequence(player) - Beyonders.sequenceOf(target));
        boolean studied = Marks.count(target, player, WormOfTimePowers.DECRYPTED) > 0;
        float chance = Math.max(0.15f, 1f - gap * 0.18f + (studied ? 0.25f : 0));
        if (player.getRandom().nextFloat() > chance) {
            player.sendSystemMessage(Component.literal("The stronger spirit resists your theft. Decrypt it first."));
            return;
        }

        Player victim = hasPathway(target) ? (Player) target : null;
        Set<PathwayAbilities.Ability> taken = new HashSet<>();
        int count = Beyonder.hasSequence(player, 3) ? 3 : 1;
        for (int i = 0; i < count && ability != null && !StolenAbilities.isFull(player, THEFT_MAX_HELD); i++) {
            boolean sneakForm = ability.sneakForm() && player.getRandom().nextBoolean();
            StolenAbilities.Held held = StolenAbilities.hold(player, ability, sneakForm, victim, THEFT_HELD_TICKS, THEFT_MAX_HELD);
            if (held == null) break;
            taken.add(ability);
            player.sendSystemMessage(Component.literal("You stole " + held.label() + " of the " + ability.pathway()
                    + " Pathway. Sneak and press the key again to release it."));
            if (victim != null) victim.sendSystemMessage(Component.literal("Your " + ability.name() + " was stolen."));
            theftShows(player, sl, target, held, victim == null);
            ability = pickToSteal(player, target, taken);
        }
        if (!taken.isEmpty() && victim == null && THEFT_MOB_EMPTY_TICKS > 0)
            Marks.add(target, THEFT_EMPTY, 1, THEFT_MOB_EMPTY_TICKS);
    }

    /** A player who follows a Pathway that has abilities: only those lose one of their own. */
    private static boolean hasPathway(LivingEntity target) {
        return target instanceof Player other && !PathwayAbilities.of(SoulCore.getPathway(other)).isEmpty();
    }

    /** What the theft takes from this target, or null if there is nothing to take right now. */
    private static PathwayAbilities.Ability pickToSteal(Player thief, LivingEntity target, Set<PathwayAbilities.Ability> excluded) {
        List<PathwayAbilities.Ability> choices = new ArrayList<>();
        if (hasPathway(target)) {
            Player victim = (Player) target;
            // what their Sequence gives them, and what nobody holds already
            for (PathwayAbilities.Ability ability : PathwayAbilities.of(SoulCore.getPathway(victim))) {
                if (ability.stealable() && Beyonder.hasSequence(victim, ability.sequence())
                        && !StolenAbilities.isTaken(victim, ability)) {
                    choices.add(ability);
                }
            }
        } else {
            // only a Beyonder mob has anything: what its Pathway gives at its Sequence (blasphemy_slate/beyonder_mobs)
            if (!BeyonderMobs.isBeyonder(target) || Marks.count(target, THEFT_EMPTY) > 0) return null;
            choices.addAll(Beyonders.takeableFrom(target));
        }
        choices.removeAll(excluded);
        if (choices.isEmpty()) return null;
        Casts.Cast recent = Casts.last(target);
        if (recent != null && recent.ability() != null && choices.contains(recent.ability())
                && (Beyonder.hasSequence(thief, 3) || Marks.count(target, thief, WormOfTimePowers.DECRYPTED) > 0))
            return recent.ability();
        return choices.get(thief.getRandom().nextInt(choices.size()));
    }

    private static void releaseStolen(Player player, Level level, ServerLevel sl) {
        StolenAbilities.Held held = StolenAbilities.newest(player);
        if (held == null) {
            player.sendSystemMessage(Component.literal("You hold nothing stolen."));
            return;
        }
        if (!castStolen(player, level, sl, held)) {
            // it refused (no target, no room...) and has said why: it is not used up
            player.sendSystemMessage(Component.literal(held.label() + " did not go off. You still hold it."));
            return;
        }
        StolenAbilities.spend(player, held);
        releaseShows(player, sl, held);

        StolenAbilities.Held next = StolenAbilities.newest(player);
        player.sendSystemMessage(Component.literal("Released " + held.label() + "."
                + (next == null ? "" : " Next: " + next.label() + ".")));
    }

    /**
     * Casts a held ability for the thief: no Pathway check, no Sequence gate, no cost (Beyonder.castUnbound).
     * True if it went off. The thief is sneaking to release it, so for that moment they are made to stand or
     * sneak as the stolen form needs.
     */
    private static boolean castStolen(Player player, Level level, ServerLevel sl, StolenAbilities.Held held) {
        boolean wasSneaking = player.isShiftKeyDown();
        player.setShiftKeyDown(held.sneakForm());
        try {
            return Beyonder.castUnbound(player, () -> held.ability().cast().run(player, level, sl));
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A stolen ability failed: {}", held.label(), e);
            return true;                                             // used up all the same, so it cannot fail again and again
        } finally {
            player.setShiftKeyDown(wasSneaking);
        }
    }

    // =========================
    // Ability 3 - Parasite
    // Normal: deep parasitism, with mob steering and life drain. Sneak: initial parasitism,
    //         a lower-upkeep hiding mode that heals without draining or steering the host.
    // Outside a host: you leave your body and go into what you look at (a mob or a player). It carries you,
    //         you see through its eyes, and nothing can see or hurt you. While you are inside: an upkeep and a
    //         life drain at each beat, two minutes at most, and you are thrown out hurt if it dies.
    // Inside a host: the same key puts you back out, standing behind it. Sneaking makes no difference.
    //         A mob host can be steered (status/ParasiteControl): the walk keys, jump and sprint walk it where you
    //         look, left click makes it hit (or shoot) what you look at, right click tells it what to hunt. With no
    //         input for PARASITE_STEER_IDLE_TICKS it acts on its own again. A player host only carries you.
    // Going in, being carried and coming out is status/Parasite. What the parasite does to its host is the
    // four methods under "Parasite: what it does": that is where new effects go.
    // =========================
    public static void wormOfTimeAbility3(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (!Beyonder.hasSequence(player, PARASITE_SEQUENCE)) return;

        if (Parasite.isInside(player)) {
            if (Parasite.ticksInside(player) < PARASITE_KEY_REST) return;    // still the press that took them in
            Parasite.leave(player, Parasite.End.LEFT);
            return;
        }
        if (Marks.count(player, PARASITE_RESTING) > 0) return;               // still the press that took them out
        parasitize(player, sl);
    }

    private static void parasitize(Player player, ServerLevel sl) {
        if (!(player instanceof ServerPlayer parasite)) return;
        LivingEntity host = Spells.aimEnemy(player, sl, PARASITE_RANGE);
        if (host == null) {
            player.sendSystemMessage(Component.literal("There is no host close enough."));
            return;                                                  // nothing spent
        }
        String refusal = parasiteRefusal(player, host);
        if (refusal != null) {
            player.sendSystemMessage(Component.literal(refusal));
            return;
        }
        if (!Spells.paySpirituality(player, PARASITE_COST)) return;

        Vec3 chest = player.position().add(0, 1.1, 0);              // where the body was, for the looks
        parasite.getPersistentData().putBoolean(PARASITE_INITIAL, player.isShiftKeyDown());
        if (!Parasite.enter(parasite, host, PARASITE_MAX_TICKS, PARASITE_HOOKS)) {
            parasite.getPersistentData().remove(PARASITE_INITIAL);
            // (refused after all, e.g. by another mod: checked above, so this should not happen)
            SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + PARASITE_COST);
            player.sendSystemMessage(Component.literal("You cannot enter that."));
            return;
        }
        enterShows(sl, chest, host);
    }

    /**
     * Why this player cannot take this host, as the text they are told, or null if they can.
     * Stronger Beyonders and creative/spectator players cannot be used as hosts.
     */
    private static String parasiteRefusal(Player player, LivingEntity host) {
        if (host instanceof Player p && (p.isCreative() || p.isSpectator())) return "That body cannot be parasitized.";
        if (Beyonders.isBeyonder(host) && Beyonders.sequenceOf(host) < SoulCore.getSequence(player))
            return "That spirit is too strong to parasitize.";
        if (Marks.count(host, PARASITE_RESTING) > 0) return "That host is still resisting a previous parasite.";
        if (Parasite.isHost(host)) return "Something lives in it already.";
        if (!Parasite.canEnter(player, host)) return "You cannot enter that.";
        return null;
    }

    // ---------- Parasite: what it does. New effects go into these four methods ----------

    /** Once, when the parasite has just gone in. */
    private static void parasiteEntered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {
        boolean steers = PARASITE_STEERS_MOBS && host instanceof Mob
                && !parasite.getPersistentData().getBooleanOr(PARASITE_INITIAL, false);
        parasite.sendSystemMessage(Component.literal("You are inside " + host.getName().getString() + "."
                + (steers ? " Move and look to steer it, left click to strike, right click to pick what it hunts." : "")
                + " Press the key again to leave it."));
        if (PARASITE_MAX_TICKS > 0) HudTimer.show(parasite, PARASITE_CLOCK, "Parasite", PARASITE_MAX_TICKS, PARASITE_COLOR);
        if (PARASITE_HOST_IS_TOLD && host instanceof Player carrier) {
            carrier.sendSystemMessage(Component.literal("Something has crawled into you."));
            if (PARASITE_MAX_TICKS > 0) HudTimer.show(carrier, PARASITE_HOST_CLOCK, "Parasitized", PARASITE_MAX_TICKS, 0xFF7A6A);
        }
        if (!parasite.getPersistentData().getBooleanOr(PARASITE_INITIAL, false)) Exposed.apply(host, 40);
    }

    /** Every tick while inside (ticksInside starts at 1). Parasite.leave(parasite, ...) from here ends it. */
    private static void parasiteTick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {
        if (ticksInside % PARASITE_BEAT != 0) return;               // everything below happens once a beat

        if (!PATHWAY.equals(SoulCore.getPathway(parasite)) || !Beyonder.hasSequence(parasite, 4)) {
            Parasite.leave(parasite, Parasite.End.LEFT);
            return;
        }
        boolean initial = parasite.getPersistentData().getBooleanOr(PARASITE_INITIAL, false);
        int upkeep = initial ? 900 : PARASITE_UPKEEP;
        if (!Spells.paySpirituality(parasite, upkeep)) {
            Parasite.leave(parasite, Parasite.End.OUT_OF_POWER);
            return;
        }
        if (initial) parasite.heal(1f);
        else {
            parasiteDrain(parasite, host, sl);
            Exposed.apply(host, 40);
        }
    }

    /**
     * Once, when it is over, whatever ended it ('why'). The parasite has its body back and stands outside.
     * 'host' is null if it is no longer in this world, and dead when why is HOST_DIED.
     */
    private static void parasiteLeft(ServerPlayer parasite, LivingEntity host, ServerLevel sl, Parasite.End why) {
        HudTimer.hide(parasite, PARASITE_CLOCK);
        parasite.sendSystemMessage(Component.literal(parasiteEndText(why)));
        if (PARASITE_HOST_IS_TOLD && host instanceof Player carrier) {
            HudTimer.hide(carrier, PARASITE_HOST_CLOCK);
            if (why != Parasite.End.HOST_DIED) carrier.sendSystemMessage(Component.literal("Whatever was inside you is gone."));
        }
        parasite.getPersistentData().remove(PARASITE_INITIAL);
        if (why == Parasite.End.PARASITE_GONE) return;              // died or logged out: nothing more to do or show

        if (PARASITE_KEY_REST > 0) Marks.add(parasite, PARASITE_RESTING, 1, PARASITE_KEY_REST);
        leaveShows(parasite, sl, host);
        if (why == Parasite.End.HOST_DIED && PARASITE_THROWN_OUT_DAMAGE > 0) {
            parasite.invulnerableTime = 0;
            parasite.hurt(sl.damageSources().magic(), PARASITE_THROWN_OUT_DAMAGE);
        }
        if (host != null && host.isAlive()) Marks.add(host, PARASITE_RESTING, 1, 100);
    }

    /**
     * The life drain of one beat: the host loses health, the parasite gains it. It takes health directly, as
     * Life Theft does (no hurt flash, no knockback, armor does not help, the host does not turn on anyone).
     */
    private static void parasiteDrain(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {
        float take = Math.min(PARASITE_DRAIN, host.getHealth() - PARASITE_DRAIN_LEAVES);
        if (take <= 0) return;
        if (host.getHealth() > take) host.setHealth(host.getHealth() - take);
        else host.kill(sl);                                          // only with PARASITE_DRAIN_LEAVES = 0
        if (PARASITE_DRAIN_GAIN > 0) parasite.heal(take * PARASITE_DRAIN_GAIN);
    }

    /**
     * Right after the parasite made its mob host hit (or shoot at) 'target' with left click.
     * A steered strike exposes its victim briefly; all actual strike damage is still ParasiteControl's.
     */
    private static void parasiteStruck(ServerPlayer parasite, LivingEntity host, LivingEntity target, ServerLevel sl) {
        Exposed.apply(target, 40);
    }

    private static String parasiteEndText(Parasite.End why) {
        switch (why) {
            case LEFT:          return "You leave your host.";
            case TIME_UP:       return "You cannot stay inside any longer.";
            case OUT_OF_POWER:  return "Your spirituality runs dry and your host casts you out.";
            case HOST_DIED:     return "Your host died. You are thrown out of it.";
            case HOST_GONE:     return "Your host is gone.";
            default:            return "You are in no host any more.";
        }
    }

    // ---------- what is seen and heard (nothing below changes what happens) ----------

    /** The theft: a ring closes on the target, a thread joins it to the thief's hand and a light flies along it. */
    private static void theftShows(Player player, ServerLevel sl, LivingEntity target, StolenAbilities.Held held, boolean emptied) {
        Vec3 heart = target.getBoundingBox().getCenter();
        SpellFx.closingRing(sl, target.position(), Math.max(1.2, target.getBbWidth() * 1.6), COMBO_MID, 8, 0);
        ParticleShapes.burst(sl, COMBO_STAR_WHITE, heart, 18, 0.05, 0.22);
        SpellFx.tether(sl, THEFT_THREAD, () -> SpellFx.middleOf(target, sl), () -> SpellFx.handOf(player, sl),
                THEFT_FLIGHT_TICKS + 4);
        flyInto(sl, THEFT_LIGHT.scale(0.5f).spin(24f).lifetime(THEFT_FLIGHT_TICKS + 3).fade(1, 3),
                heart, player, 1.1, THEFT_FLIGHT_TICKS);
        Spells.sound(sl, target.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.9f, 1.7f);
        Spells.sound(sl, player.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f, 1.4f);

        // a mob that has nothing more to give carries a mark for as long as that lasts
        if (emptied && THEFT_MOB_EMPTY_TICKS > 0) {
            SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x9052E3C4, THEFT_MOB_EMPTY_TICKS, THEFT_EMPTY);
        }

        // the light arrives: it joins the ones circling over the thief's head
        Later.run(sl, THEFT_FLIGHT_TICKS, () -> {
            if (!Spells.casterStillHere(player, sl) || !StolenAbilities.holds(player, held)) return;
            float outPixels = (float) (THEFT_ORB_RADIUS * 16.0 / THEFT_ORB_SCALE);   // a pivot this far from its middle makes it circle
            ModelFx light = THEFT_LIGHT.scale(THEFT_ORB_SCALE).pivot(8 + outPixels, 8, 8).spin(THEFT_ORB_SPIN)
                    .lifetime(THEFT_HELD_TICKS - THEFT_FLIGHT_TICKS).fade(6, 20)
                    .tag(THEFT_ORB_TAG + held.slot());
            ParticleShapes.modelOn(sl, light, player, new Vec3(0, player.getBbHeight() + 0.55, 0),
                    orbAngle(held.slot()), 0f, 0f);
            SpellFx.risingRings(sl, player.position(), 0.9, 1.8, 3, COMBO_HEAD);
            Spells.sound(sl, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        });
    }

    /**
     * Where a held ability's light circles over the head (degrees). With a limit the lights share the circle
     * evenly; with no limit each new one goes 137.5 degrees further on, so any number of them stay apart.
     */
    private static float orbAngle(int slot) {
        return THEFT_MAX_HELD > 0 ? slot * 360f / THEFT_MAX_HELD : slot * 137.5f;
    }

    /** The release: its light over the head goes out, and a ring and a ribbon of the Pathway's colors burst from the thief. */
    private static void releaseShows(Player player, ServerLevel sl, StolenAbilities.Held held) {
        ParticleShapes.clearModels(sl, player, THEFT_ORB_TAG + held.slot());
        Vec3 chest = player.position().add(0, 1.1, 0);
        SpellFx.shockRing(sl, player.position(), 2.4, COMBO_HEAD, 8, 0);
        ParticleShapes.slash(sl, COMBO_RING.radius(1.6f).lifetime(8).sweep(3), chest, player.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, COMBO_STAR, chest.add(0, 0.9, 0), 22, 0.08, 0.3);
        Spells.sound(sl, player.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
    }

    /** Going in: the body comes apart where it stood, and a thread and a light run from there into the host. */
    private static void enterShows(ServerLevel sl, Vec3 chest, LivingEntity host) {
        Vec3 heart = host.getBoundingBox().getCenter();
        ParticleShapes.burst(sl, COMBO_STAR_WHITE, chest, 24, 0.06, 0.25);
        SpellFx.closingRing(sl, chest.add(0, -1.1, 0), 1.4, COMBO_MID, 8, 0);
        ParticleShapes.slashBetween(sl, THEFT_THREAD.lifetime(PARASITE_FLIGHT_TICKS + 4).sweep(3), chest, heart);
        flyInto(sl, THEFT_LIGHT.scale(0.5f).spin(24f).lifetime(PARASITE_FLIGHT_TICKS + 3).fade(1, 3),
                chest, host, host.getBbHeight() * 0.5, PARASITE_FLIGHT_TICKS);
        SpellFx.closingRing(sl, host.position(), Math.max(1.0, host.getBbWidth() * 1.4), COMBO_MID, 8, PARASITE_FLIGHT_TICKS);
        Spells.sound(sl, chest, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f, 0.8f);
        Spells.sound(sl, host.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 0.7f);
    }

    /** Coming out: sparks leave the host (if it is still there) and a ring bursts where the parasite stands again. */
    private static void leaveShows(ServerPlayer parasite, ServerLevel sl, LivingEntity host) {
        if (host != null) ParticleShapes.burst(sl, COMBO_STAR_WHITE, host.getBoundingBox().getCenter(), 18, 0.05, 0.22);
        SpellFx.shockRing(sl, parasite.position(), 1.8, COMBO_HEAD, 8, 0);
        ParticleShapes.burst(sl, COMBO_STAR, parasite.position().add(0, 1.1, 0), 22, 0.08, 0.3);
        Spells.sound(sl, parasite.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.2f);
    }

    /** Like SpellFx.homing, but the model ends 'height' blocks above the entity's feet instead of at them. */
    private static void flyInto(ServerLevel sl, ModelFx model, Vec3 from, Entity target, double height, int ticks) {
        Vec3 way = target.position().add(0, height, 0).subtract(from);
        float away = (float) Math.sqrt(way.x * way.x + way.z * way.z);
        float yaw = away < 0.1f ? target.getYRot() : Spells.yawOf(way);
        ModelFx flying = model
                .during(0, 0, ModelFx.pose().forward(-away).up((float) -way.y))                     // where it starts, seen from where it ends
                .during(0, Math.max(1, ticks), ModelFx.pose().forward(0f).up(0f), ModelFx.Ease.IN);
        ParticleShapes.modelOn(sl, flying, target, new Vec3(0, height, 0), yaw, 0f, 0f);
    }


    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getPathway(player));
    }
}
