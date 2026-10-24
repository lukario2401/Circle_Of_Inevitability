package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class WormOfTime {

    private static final String PATHWAY = "Worm Of Time";

    private static final String LIFE_THEFT_VULNERABILITY = "word_of_time_life_theft_vulnerability";

    private static final int COMBO_HEAD = 0xF2F8FFFF;
    private static final int COMBO_MID  = 0xEE1BB8E8;
    private static final int COMBO_TAIL = 0xA00A1022;

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/band")
            .color(COMBO_MID).tailColor(COMBO_TAIL).headColor(COMBO_HEAD)
            .translucent()
            .radius(3.6f).arc(320f).width(1f).taper(SlashFx.Taper.COMET)
            .layers(4).spread(0.28f)
            .lifetime(12).sweep(5).spin(10f);             // draws itself, keeps spinning, then wipes away



    private static final SlashFx COMBO_TILTED_RING = COMBO_RING
            .radius(2.4f).arc(-270f).width(1.2f)          // negative arc = sweeps the other way
            .sweep(4).spin(-8f)
            .rotation(0f, 0, -45);                     // big arch over and around you, tilted


    private static final SlashFx COMBO_GROUND_SPIRAL = COMBO_RING.flat()
            .radius(1.2f).endRadius(3.6f).arc(540f).width(1.3f)   // lies on the ground, winds outward
            .spread(0.4f)
            .lifetime(14).sweep(6).spin(9f);
    private static final SlashFx COMBO_CRYSTAL = SlashFx.line("slash/crystal")
            .color(0xC89CFFE6).core(0x70FFFFFF)
            .radius(1.9f).width(0.32f).taper(SlashFx.Taper.CRESCENT)   // pointed at both ends
            .lifetime(16).sweep(2).fromCenter();                       // shoots out, holds, fades
    private static final ParticleFx COMBO_STAR = ParticleFx.of("fx/spark")
            .color(0xFFB8FFEE).fadeOut()
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(28, 14).friction(0.96f)
            .glow().spin(12f).randomRotation();
    private static final ParticleFx COMBO_STAR_WHITE = COMBO_STAR.color(0xFFFFFFFF).fadeOut();

    // =========================
    // Ability 2 - Theft: the numbers and the looks
    // =========================
    private static final int THEFT_SEQUENCE = 8;
    private static final int THEFT_COST = 250;                 // for stealing. Releasing costs nothing
    private static final double THEFT_RANGE = 32;
    private static final int THEFT_HELD_TICKS = 1200*20;          // a stolen ability is held for a minute at most
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
            return PARASITE_STEERS_MOBS ? PARASITE_STEERING : null;     // (only asked for a mob)
        }

        @Override
        public void struck(ServerPlayer parasite, LivingEntity host, LivingEntity target, ServerLevel sl) {
            parasiteStruck(parasite, host, target, sl);
        }
    };

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway(PATHWAY)
            .step(WormOfTime::comboRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboTiltedRing)
            .step(WormOfTime::comboCrystalCross)
            .randomStep(WormOfTime::comboGroundSpiral, WormOfTime::comboCrystalCross);

    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSpirituality(player) < 250) return;
        if (!Beyonder.hasSequence(player, 9)) return;

        SoulCore.setSpirituality(player,SoulCore.getSpirituality(player)-250);


        if (!player.isShiftKeyDown()){

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,24);
            if (livingEntity == null) return;
            int hitCount = livingEntity.getPersistentData().getIntOr(LIFE_THEFT_VULNERABILITY, 0);
            if (hitCount==0)hitCount=1;
            int healthStealAmount = 15*hitCount;

            if (livingEntity.getHealth()>healthStealAmount){
                livingEntity.setHealth(livingEntity.getHealth()-healthStealAmount);
                player.sendSystemMessage(Component.literal(""+hitCount));
                player.setHealth(player.getHealth()+healthStealAmount);
            }else{
                player.setHealth((player.getHealth()+livingEntity.getHealth()));
                livingEntity.kill(sl);
            }
            livingEntity.getPersistentData().putInt(LIFE_THEFT_VULNERABILITY,hitCount+1);

        }else{
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You are held in place."));
                return;
            }

            Vec3 there = Spells.blinkSpot(player, sl, 32);

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,32);
            if (livingEntity!=null){
                there=livingEntity.position();
            }

            if (there == null) {
                player.sendSystemMessage(Component.literal("There is no room for you there."));
                return;                                                  // nothing spent
            }
            player.teleportTo(there.x, there.y, there.z);
            FallGuard.protect(player, 200);

        }
    }


    // =========================
    // Ability 2 - Theft
    // Normal: steals an ability from what you look at and holds it. From a player with a Pathway: one of the
    //         abilities their Sequence gives them, and they cannot use it while you hold it. From a Beyonder mob
    //         (blasphemy_slate/beyonder_mobs): one of its Pathway's, up to its Sequence. Other mobs have nothing.
    // Sneak:  releases the newest one you hold: it is cast once, whatever its Sequence, at no cost.
    // Who holds what is kept by StolenAbilities; the list of abilities is PathwayAbilities.
    // =========================
    public static void wormOfTimeAbility2(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (!Beyonder.hasSequence(player, THEFT_SEQUENCE)) return;

        if (player.isShiftKeyDown()) releaseStolen(player, level, sl);
        else steal(player, sl);
    }

    private static void steal(Player player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, THEFT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return;                                                  // nothing spent
        }
        if (StolenAbilities.isFull(player, THEFT_MAX_HELD)) {
            player.sendSystemMessage(Component.literal("You cannot hold more than " + THEFT_MAX_HELD
                    + " stolen abilities. Release one first (sneak)."));
            return;
        }
        PathwayAbilities.Ability ability = pickToSteal(player, target);
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

        boolean sneakForm = ability.sneakForm() && player.getRandom().nextBoolean();
        Player victim = hasPathway(target) ? (Player) target : null;
        StolenAbilities.Held held = StolenAbilities.hold(player, ability, sneakForm, victim, THEFT_HELD_TICKS, THEFT_MAX_HELD);
        if (held == null) return;                                    // (full: checked above)
        if (victim == null && THEFT_MOB_EMPTY_TICKS > 0) Marks.add(target, THEFT_EMPTY, 1, THEFT_MOB_EMPTY_TICKS);

        player.sendSystemMessage(Component.literal("You stole " + held.label() + " of the " + ability.pathway()
                + " Pathway. Sneak and press the key again to release it."));
        if (victim != null) {
            victim.sendSystemMessage(Component.literal("Your " + ability.name() + " was stolen."));
        }
        theftShows(player, sl, target, held, victim == null);
    }

    /** A player who follows a Pathway that has abilities: only those lose one of their own. */
    private static boolean hasPathway(LivingEntity target) {
        return target instanceof Player other && !PathwayAbilities.of(SoulCore.getPathway(other)).isEmpty();
    }

    /** What the theft takes from this target, or null if there is nothing to take right now. */
    private static PathwayAbilities.Ability pickToSteal(Player thief, LivingEntity target) {
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
        return choices.isEmpty() ? null : choices.get(thief.getRandom().nextInt(choices.size()));
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
            // TODO(parasite): sneak + the key from inside does the same as the key alone for now: free for something later
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
        if (!Parasite.enter(parasite, host, PARASITE_MAX_TICKS, PARASITE_HOOKS)) {
            // (refused after all, e.g. by another mod: checked above, so this should not happen)
            SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + PARASITE_COST);
            player.sendSystemMessage(Component.literal("You cannot enter that."));
            return;
        }
        enterShows(sl, chest, host);
    }

    /**
     * Why this player cannot take this host, as the text they are told, or null if they can.
     * TODO(parasite): rules for later go here (a Beyonder of a stronger Sequence, a boss, a host taken too recently...).
     */
    private static String parasiteRefusal(Player player, LivingEntity host) {
        if (Parasite.isHost(host)) return "Something lives in it already.";
        if (!Parasite.canEnter(player, host)) return "You cannot enter that.";
        return null;
    }

    // ---------- Parasite: what it does. New effects go into these four methods ----------

    /** Once, when the parasite has just gone in. */
    private static void parasiteEntered(ServerPlayer parasite, LivingEntity host, ServerLevel sl) {
        boolean steers = PARASITE_STEERS_MOBS && host instanceof Mob;
        parasite.sendSystemMessage(Component.literal("You are inside " + host.getName().getString() + "."
                + (steers ? " Move and look to steer it, left click to strike, right click to pick what it hunts." : "")
                + " Press the key again to leave it."));
        if (PARASITE_MAX_TICKS > 0) HudTimer.show(parasite, PARASITE_CLOCK, "Parasite", PARASITE_MAX_TICKS, PARASITE_COLOR);
        if (PARASITE_HOST_IS_TOLD && host instanceof Player carrier) {
            carrier.sendSystemMessage(Component.literal("Something has crawled into you."));
            if (PARASITE_MAX_TICKS > 0) HudTimer.show(carrier, PARASITE_HOST_CLOCK, "Parasitized", PARASITE_MAX_TICKS, 0xFF7A6A);
        }
        // TODO(parasite): what begins with it (a mark on the host, a first bite, a weakness...)
    }

    /** Every tick while inside (ticksInside starts at 1). Parasite.leave(parasite, ...) from here ends it. */
    private static void parasiteTick(ServerPlayer parasite, LivingEntity host, ServerLevel sl, int ticksInside) {
        if (ticksInside % PARASITE_BEAT != 0) return;               // everything below happens once a beat

        if (PARASITE_UPKEEP > 0 && !Spells.paySpirituality(parasite, PARASITE_UPKEEP)) {
            Parasite.leave(parasite, Parasite.End.OUT_OF_POWER);
            return;
        }
        parasiteDrain(parasite, host, sl);
        // TODO(parasite): more that happens while inside (stealing from the host without aiming, its spirituality,
        //                 slowly taking it over...). 'host' may be a mob or a Player. Steering is status/ParasiteControl.
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
        if (why == Parasite.End.PARASITE_GONE) return;              // died or logged out: nothing more to do or show

        if (PARASITE_KEY_REST > 0) Marks.add(parasite, PARASITE_RESTING, 1, PARASITE_KEY_REST);
        leaveShows(parasite, sl, host);
        if (why == Parasite.End.HOST_DIED && PARASITE_THROWN_OUT_DAMAGE > 0) {
            parasite.invulnerableTime = 0;
            parasite.hurt(sl.damageSources().magic(), PARASITE_THROWN_OUT_DAMAGE);
        }
        // TODO(parasite): what it leaves behind (a lasting mark or wound on the host, a time before the next host...)
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
     * TODO(parasite): what a steered strike adds (the parasite's own power in the blow, a mark, stealing from it...)
     */
    private static void parasiteStruck(ServerPlayer parasite, LivingEntity host, LivingEntity target, ServerLevel sl) {
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


    private static void comboRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.9, 0);
        // sweeps either way, starts from a different side each time, slightly tilted
        SlashFx ring = COMBO_RING.varied(player.getRandom(), 60f, 8f, 12f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    // Hit 2: a tilted, almost upright ring around the player
    private static void comboTiltedRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 1.1, 0);
        // tilted to the left or right side (mirrored), leaning a bit more or less each time
        SlashFx ring = COMBO_TILTED_RING.varied(player.getRandom(), 45f, 45f, 45f);
        ParticleShapes.slash(sl, ring, center, player.getYRot(), 0f, 0f);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.1f);
    }


    // Hit 3 (a): spiral whirling on the ground around the player
    private static void comboGroundSpiral(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 center = player.position().add(0, 0.15, 0);
        // starts at any angle and spins either way (both spirals the same way)
        RandomSource random = player.getRandom();
        SlashFx spiral = random.nextBoolean() ? COMBO_GROUND_SPIRAL : COMBO_GROUND_SPIRAL.mirrored();
        float start = random.nextFloat() * 360f;
        ParticleShapes.slash(sl, spiral, center, start, 0f, 0f);
        ParticleShapes.slash(sl, spiral.layers(2).radius(0.8f).endRadius(2.6f).delay(3),
                center.add(0, 0.05, 0), start + 180f, 0f, 0f);   // second, smaller spiral
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.6f);
    }

    // Hit 3 (b): crossed crystal blades in an X on the target + a swirling galaxy of sparkles under it
    private static void comboCrystalCross(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        Vec3 hit = target.getBoundingBox().getCenter();
        RandomSource random = player.getRandom();
        float xTurn = (random.nextFloat() * 2f - 1f) * 25f;   // the whole X turned a bit
        float xOpen = 35f + random.nextFloat() * 20f;          // wide or narrow X
        for (int bundle = 0; bundle < 2; bundle++) {
            float baseRoll = xTurn + (bundle == 0 ? xOpen : -xOpen);   // the two diagonals
            for (int i = 0; i < 4; i++) {
                Vec3 center = hit.add((random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.4,
                        (random.nextDouble() - 0.5) * 0.4);
                SlashFx blade = COMBO_CRYSTAL
                        .radius(1.5f + random.nextFloat() * 0.7f)
                        .width(0.24f + random.nextFloat() * 0.14f)
                        .delay(bundle * 2 + i / 2);      // blades appear in quick steps
                float yaw = player.getYRot() + (random.nextFloat() - 0.5f) * 20f;
                float roll = baseRoll + (random.nextFloat() - 0.5f) * 16f;
                ParticleShapes.slash(sl, blade, center, yaw, 0f, roll);
            }
        }
        Vec3 ground = target.position().add(0, 0.15, 0);
        ParticleShapes.spiral(sl, COMBO_STAR, ground, 4, 220, 0.6, 4.0, 0.8, 0.06);
        ParticleShapes.spiral(sl, COMBO_STAR_WHITE, ground, 4, 90, 0.6, 4.0, 0.8, 0.06);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 1.2f);
        sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1f);
    }

    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getPathway(player));
    }
}
