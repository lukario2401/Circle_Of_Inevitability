package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationFx.Status;
import net.lukario.circleofinevitability.combat.Contagion;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Connection;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.Nocturnality;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Abomination, key 7: the Curses wheel (Disciple of Silence, Sequence 3; Ancient Bane, 2; Abomination, 1).
 * docs/ABOMINATION.md, "Key 7 wheel".
 *
 *   Curse by Connection (Disciple)  an enemy you are connected to (status/Connection: hit, chained, cursed lately)
 *                                   is cursed wherever it is: Misfortune and more damage taken
 *   Cursed Artifact     (Disciple)  a relic only you see, waiting on the ground (combat/Trap): the first enemy near
 *                                   it is cursed, hushed and becomes your connection. Three at once
 *   Rite of Silence     (Disciple)  a ring where enemies cannot use abilities and mobs cannot attack (status/Silence)
 *   Bane Mist           (Bane)      a pale bloody mist: mobs in it fight each other, players flinch, and everyone's
 *                                   luck drains the longer they stay
 *   Ancient Blight      (Bane)      a curse that jumps from enemy to enemy, one level of Misfortune deeper each jump
 *                                   (combat/Contagion)
 *   Abomination Form    (Abomination) the Mythical Creature Form: you shrink into a doll bound by thorns (status/
 *                                   GiantForm), strong and hard to hurt; near you luck drains, whoever hurts you is
 *                                   cursed, and at the end every curse you laid comes due. Chained (0): no timer
 *
 * Every curse here is Misfortune laid with Abomination.curse, so Collection of Curses (Sequence 1) makes all of
 * them last longer. Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 */
public final class AbominationCurses {

    private AbominationCurses() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int CURSES_SEQUENCE = 3;                        // the key
    private static final float QUICK_CAST_COST = 2f;

    // Curse by Connection
    private static final int CONNECTION_SEQUENCE = 3;
    private static final int CONNECTION_COST = 8000;
    private static final int CONNECTION_COOLDOWN = 600;
    private static final double CONNECTION_SIGHT = 64.0;         // the one you look at, this far...
    private static final int CONNECTION_WINDOW = 12000;          // ...if you touched it in the last 10 minutes
    private static final int CONNECTION_CURSE_TICKS = 600;
    private static final int CONNECTION_CURSE = 2;               // Misfortune II...
    private static final float CONNECTION_VULNERABLE = 0.15f;    // ...and 15% more damage taken
    private static final int CONNECTION_BANE_SEQUENCE = 2;       // Ancient Bane: 30 minutes, and twice as long a curse
    private static final int CONNECTION_BANE_WINDOW = 36000;

    // Cursed Artifact
    private static final int RELIC_SEQUENCE = 3;
    private static final int RELIC_COST = 6000;
    private static final int RELIC_COOLDOWN = 100;
    private static final double RELIC_RANGE = 24.0;
    private static final double RELIC_RADIUS = 1.5;
    private static final int RELIC_MOST = 3;
    private static final int RELIC_TICKS = 6000;                 // each waits 5 minutes
    private static final int RELIC_ARMS = 20;
    private static final int RELIC_CURSE = 3;                    // heavy Misfortune...
    private static final int RELIC_CURSE_TICKS = 300;
    private static final int RELIC_HUSH = 100;                   // ...and hushed 5 seconds

    // Rite of Silence
    private static final int RITE_SEQUENCE = 3;
    private static final int RITE_COST = 7000;
    private static final int RITE_COOLDOWN = 600;
    private static final double RITE_RANGE = 32.0;
    private static final double RITE_RADIUS = 8.0;               // 16 blocks across
    private static final int RITE_TICKS = 160;

    // Bane Mist
    private static final int MIST_SEQUENCE = 2;
    private static final int MIST_COST = 20000;
    private static final int MIST_COOLDOWN = 900;
    private static final double MIST_RANGE = 32.0;
    private static final double MIST_RADIUS = 20.0;
    private static final int MIST_TICKS = 300;
    private static final int MIST_DEEPER = 60;                   // one level more Misfortune allowed every 3 seconds inside...
    private static final int MIST_CURSE_TICKS = 100;             // ...each level lasting this long after
    private static final int MIST_BRAWL_EVERY = 60;              // mobs in it turn on each other this often

    // Ancient Blight
    private static final int BLIGHT_SEQUENCE = 2;
    private static final int BLIGHT_COST = 25000;
    private static final int BLIGHT_COOLDOWN = 1200;
    private static final double BLIGHT_RANGE = 32.0;
    private static final int BLIGHT_CARRIED = 100;               // each carries it this long before it jumps
    private static final double BLIGHT_REACH = 8.0;
    private static final int BLIGHT_CHAIN = 5;                   // up to 5 in a chain...
    private static final int BLIGHT_MOST = 30;                   // ...30 in all
    private static final int BLIGHT_CURSE_TICKS = 200;

    // Abomination Form
    private static final int FORM_SEQUENCE = 1;
    private static final int FORM_COST = 30000;
    private static final int FORM_COOLDOWN = 3600;
    private static final int FORM_TICKS = 1200;
    private static final int FORM_CHAINED_SEQUENCE = 0;          // Chained: until cast again
    private static final double FORM_SCALE = 0.5;                // a doll's body: half your size...
    private static final double FORM_REACH = 1.5;                // ...with longer reach to make up for it
    private static final int FORM_STRENGTH = 2;                  // Strength III
    private static final float FORM_CUT = 0.3f;                  // 30% less damage
    private static final double FORM_AURA = 8.0;                 // whoever is this close...
    private static final int FORM_AURA_EVERY = 60;               // ...loses a level of luck this often
    private static final int FORM_CURSE_TICKS = 200;
    private static final double FORM_CALAMITY_RADIUS = 32.0;     // at the end: every enemy this close with your Misfortune

    // =========================
    // Names and what is saved on the player
    // =========================
    private static final String RELIC_TRAP = "abomination_relic";
    private static final String RITE_ZONE = "abomination_rite";
    private static final String MIST_ZONE = "abomination_bane_mist";
    private static final String FORM_ZONE = "abomination_form_aura";
    private static final String FORM_ARMOR = "abomination_vessel";
    private static final String FORM_TAG = "abomination_form_tag";
    private static final String FORM_HEIGHT = "abomination_form_height";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("abomination_curses", "Curses")
            .radius(88)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .castsWhileSilenced(Abomination::castsThroughHush)
            .entry("curse_by_connection", AbominationFx.WHEEL_CURSE_BY_CONNECTION, "Curse by Connection", CONNECTION_SEQUENCE,
                    CONNECTION_COST, CONNECTION_COOLDOWN, AbominationCurses::curseByConnection)
            .entry("cursed_artifact", AbominationFx.WHEEL_CURSED_ARTIFACT, "Cursed Artifact", RELIC_SEQUENCE,
                    RELIC_COST, RELIC_COOLDOWN, AbominationCurses::cursedArtifact)
            .entry("rite_of_silence", AbominationFx.WHEEL_RITE_OF_SILENCE, "Rite of Silence", RITE_SEQUENCE,
                    RITE_COST, RITE_COOLDOWN, AbominationCurses::riteOfSilence)
            .entry("bane_mist", AbominationFx.WHEEL_BANE_MIST, "Bane Mist", MIST_SEQUENCE,
                    MIST_COST, MIST_COOLDOWN, AbominationCurses::baneMist)
            .entry("ancient_blight", AbominationFx.WHEEL_ANCIENT_BLIGHT, "Ancient Blight", BLIGHT_SEQUENCE,
                    BLIGHT_COST, BLIGHT_COOLDOWN, AbominationCurses::ancientBlight)
            .entry("abomination_form", AbominationFx.WHEEL_ABOMINATION_FORM, "Abomination Form", FORM_SEQUENCE,
                    FORM_COST, FORM_COOLDOWN, AbominationCurses::abominationForm)
            .free(AbominationCurses::inForm);                        // in the form, the pick ends it (free)

    // =========================
    // Ability 7 - Curses
    // Normal: the Curses wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void abominationCurses(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Abomination.ready(player, bypassClassCheck, CURSES_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Curse by Connection: an enemy you have a connection to (status/Connection: one you hit, chained, cursed or
    // sentenced in the last CONNECTION_WINDOW) is cursed wherever it is in the world, as of AbominationFx.CURSE_LANDS:
    // Misfortune CONNECTION_CURSE and CONNECTION_VULNERABLE more damage taken for CONNECTION_CURSE_TICKS. The one you
    // look at (CONNECTION_SIGHT) is picked; with none in sight, the one you connected with last. Ancient Bane (2):
    // CONNECTION_BANE_WINDOW, and the curse lasts twice as long.
    // =========================
    private static AbilityWheel.Result curseByConnection(ServerPlayer player, ServerLevel sl) {
        boolean bane = Beyonder.hasSequence(player, CONNECTION_BANE_SEQUENCE);
        int window = bane ? CONNECTION_BANE_WINDOW : CONNECTION_WINDOW;
        LivingEntity target = Spells.aimEnemy(player, sl, CONNECTION_SIGHT);
        if (target != null && !Connection.has(player, target, window)) {
            player.sendSystemMessage(Component.literal("You have no connection to that one."));
            return AbilityWheel.Result.REFUSED;
        }
        if (target == null) target = Connection.latest(player, window);
        if (target == null) {
            player.sendSystemMessage(Component.literal("You have no connection to curse through."));
            return AbilityWheel.Result.REFUSED;
        }

        LivingEntity cursed = target;
        int ticks = Abomination.curseTicks(player, bane ? CONNECTION_CURSE_TICKS * 2 : CONNECTION_CURSE_TICKS);
        String tag = "ab_curse_" + cursed.getId();
        AbominationFx.curseByConnection(sl, player, cursed, ticks, tag);
        AbominationFx.status(player, Status.CURSE, ticks);
        if (cursed.level() != sl) {
            player.sendSystemMessage(Component.literal("Your curse goes far, to " + cursed.getName().getString() + "."));
        }
        Later.run(sl, AbominationFx.CURSE_LANDS, () -> {
            if (!cursed.isAlive()) return;
            Abomination.curse(cursed, player, CONNECTION_CURSE, CONNECTION_CURSE, ticks, false);
            Vulnerable.add(cursed, CONNECTION_VULNERABLE, CONNECTION_VULNERABLE, ticks);
            Connection.touch(player, cursed);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Cursed Artifact: a relic is set on the ground where you aim (RELIC_RANGE) that only you can see (combat/Trap).
    // After RELIC_ARMS, the first enemy to step within RELIC_RADIUS of it is cursed: Misfortune RELIC_CURSE for
    // RELIC_CURSE_TICKS, hushed for RELIC_HUSH (status/Silence), and it becomes your connection. RELIC_MOST relics at
    // once (one more takes the place of the oldest); each waits RELIC_TICKS.
    // =========================
    private static AbilityWheel.Result cursedArtifact(ServerPlayer player, ServerLevel sl) {
        Vec3 ground = Spells.aimGround(player, sl, RELIC_RANGE);
        Trap.at(ground, RELIC_RADIUS)
                .armsAfter(RELIC_ARMS)
                .lasts(RELIC_TICKS)
                .onWait(left -> {
                    if (Spells.casterStillHere(player, sl)) AbominationFx.relicTick(player, ground);
                })
                .onSpring(victim -> {
                    int ticks = Abomination.curseTicks(player, RELIC_CURSE_TICKS);
                    AbominationFx.relicTriggered(sl, victim, ground, ticks, "ab_relic_" + victim.getId());
                    Abomination.curse(victim, player, RELIC_CURSE, RELIC_CURSE, ticks, false);
                    Silence.apply(victim, RELIC_HUSH);
                    Connection.touch(player, victim);
                    player.sendSystemMessage(Component.literal("A relic of yours has claimed " + victim.getName().getString() + "."));
                })
                .set(player, sl, RELIC_TRAP, RELIC_MOST);
        AbominationFx.relicPlaced(player, ground);
        AbominationFx.status(player, Status.RELIC, RELIC_TICKS);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Rite of Silence: a rite of silence over the spot you aim at (RITE_RANGE), RITE_RADIUS round, for RITE_TICKS from
    // the moment the hush has fallen (AbominationFx.RITE_FALLS). Enemies in it cannot use abilities, and mobs cannot
    // attack (status/Silence, renewed while they stay and for a second after).
    // =========================
    private static AbilityWheel.Result riteOfSilence(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, RITE_RANGE);
        String tag = "ab_rite_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, RITE_ZONE);                                     // one rite at a time
        AbominationFx.riteOfSilence(sl, center, RITE_RADIUS, RITE_TICKS, tag);
        AbominationFx.status(player, Status.SILENCE, RITE_TICKS);
        Later.run(sl, AbominationFx.RITE_FALLS, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                AbominationFx.riteEnd(sl, center, tag);
                return;
            }
            Zone.at(center, RITE_RADIUS)
                    .lasts(RITE_TICKS - AbominationFx.RITE_FALLS)
                    .every(10)
                    .onEnter(enemy -> AbominationFx.silenced(sl, enemy))
                    .onInside((enemy, inside) -> Silence.apply(enemy, 20))
                    .onEnd(() -> {
                        AbominationFx.riteEnd(sl, center, tag);
                        AbominationFx.statusEnd(player, Status.SILENCE);
                    })
                    .open(player, sl, RITE_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Bane Mist: a pale mist stained with blood and black spots spreads MIST_RADIUS round where you aim (MIST_RANGE) for
    // MIST_TICKS, darkening the sky over it (status/Nocturnality). Everything evil in it goes mad: mobs fight each
    // other (status/Taunt, every MIST_BRAWL_EVERY), players flinch and slow (status/Fear), and every second in it takes
    // more of their luck: one level of Misfortune more is allowed every MIST_DEEPER they stay, up to 5.
    // =========================
    private static AbilityWheel.Result baneMist(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, MIST_RANGE);
        String tag = "ab_mist_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, MIST_ZONE);                                     // one mist at a time
        AbominationFx.baneMist(sl, center, MIST_RADIUS, MIST_TICKS, tag);
        AbominationFx.status(player, Status.BANE_MIST, MIST_TICKS);
        Nocturnality.forceNight(sl, center, MIST_RADIUS, MIST_TICKS);
        Zone.at(center, MIST_RADIUS)
                .height(6)
                .lasts(MIST_TICKS)
                .every(10)
                .onInside((enemy, inside) -> {
                    if (inside % 20 != 0) return;
                    int most = Math.min(Misfortune.MAX_LEVEL, 1 + inside / MIST_DEEPER);
                    int level = Abomination.curse(enemy, player, 1, most, Abomination.curseTicks(player, MIST_CURSE_TICKS), false);
                    if (level > 0) AbominationFx.baneMistOn(sl, enemy, level);
                    if (enemy instanceof Player victim) {
                        Fear.apply(victim, player, 30);
                        Spells.keepEffect(victim, MobEffects.SLOWNESS, 0, 30);
                    }
                })
                .onPulse(left -> {
                    AbominationFx.baneMistTick(sl, center, MIST_RADIUS);
                    if ((MIST_TICKS - left) % MIST_BRAWL_EVERY != 0) return;
                    List<Mob> mad = new ArrayList<>();
                    for (LivingEntity one : Spells.enemiesAround(player, sl, center, MIST_RADIUS)) {
                        if (one instanceof Mob mob && Spells.canBeChosen(player, mob)) mad.add(mob);
                    }
                    if (mad.size() >= 2) Taunt.brawl(mad, MIST_BRAWL_EVERY + 10, sl.getRandom());
                })
                .onEnd(() -> {
                    AbominationFx.baneMistEnd(sl, center, tag);
                    AbominationFx.statusEnd(player, Status.BANE_MIST);
                })
                .open(player, sl, MIST_ZONE);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Ancient Blight: the enemy you look at (BLIGHT_RANGE) carries an ancient curse (combat/Contagion). Every
    // BLIGHT_CARRIED it jumps to the 2 or 3 nearest enemies within BLIGHT_REACH, up to BLIGHT_CHAIN in a chain and
    // BLIGHT_MOST in all. What it carries is Misfortune of its stage: I on the first, one level deeper at every jump.
    // =========================
    private static AbilityWheel.Result ancientBlight(ServerPlayer player, ServerLevel sl) {
        LivingEntity first = Spells.aimEnemy(player, sl, BLIGHT_RANGE);
        if (first == null) {
            player.sendSystemMessage(Component.literal("There is no one before you to carry the blight."));
            return AbilityWheel.Result.REFUSED;
        }
        String tag = "ab_blight_" + player.getId() + "_" + sl.getGameTime();
        int ticks = Abomination.curseTicks(player, BLIGHT_CURSE_TICKS);
        Contagion.spread()
                .lasts(BLIGHT_CARRIED)
                .passesTo(2, 3)
                .reach(BLIGHT_REACH)
                .generations(BLIGHT_CHAIN)
                .maxVictims(BLIGHT_MOST)
                .onCatch((victim, stage) -> {
                    int level = Math.min(Misfortune.MAX_LEVEL, stage);
                    Abomination.curse(victim, player, level, level, ticks, false);
                    AbominationFx.blight(sl, victim, stage - 1, BLIGHT_CARRIED + 10, tag + "_" + victim.getId());
                })
                .onPass((from, to, stage) -> AbominationFx.blightJump(sl, from, to, stage - 1))
                .start(player, sl, first);
        AbominationFx.status(player, Status.BLIGHT, BLIGHT_CARRIED * BLIGHT_CHAIN);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Abomination Form: your Mythical Creature Form, for FORM_TICKS. You shrink into the vessel, a doll bound by thorny
    // vines (status/GiantForm at FORM_SCALE, FORM_REACH more reach), that holds a collection of curses: Strength III
    // and FORM_CUT less damage (status/ConjuredArmor). Whoever is within FORM_AURA loses a level of luck every
    // FORM_AURA_EVERY, and whoever hurts you is cursed (Abomination.onAbominationHurt). When the form ends by time or by
    // your own hand, every curse you laid comes due: each enemy within FORM_CALAMITY_RADIUS that carries your
    // Misfortune suffers a calamity. Chained (0): no timer, it stays until you pick it again (free). Only for an
    // Abomination's own body.
    // =========================
    private static AbilityWheel.Result abominationForm(ServerPlayer player, ServerLevel sl) {
        if (inForm(player)) {                                               // the pick again: the vessel opens
            GiantForm.end(player);
            return AbilityWheel.Result.FREE;
        }
        if (!SoulCore.getPathway(player).equals(Abomination.PATHWAY)) {
            player.sendSystemMessage(Component.literal("Only an Abomination's own body can become the vessel."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean forever = Beyonder.hasSequence(player, FORM_CHAINED_SEQUENCE);
        int ticks = forever ? 0 : FORM_TICKS;                               // 0: until picked again
        double height = player.getBbHeight() / GiantForm.scale(player) * FORM_SCALE * 1.2;
        String tag = "ab_form_" + player.getId() + "_" + sl.getGameTime();
        AbominationFx.abominationForm(sl, player, height, forever ? AbominationFx.FOREVER : ticks, tag);
        GiantForm.of(FORM_SCALE)
                .reach(FORM_REACH)
                .lasts(ticks)
                .everySecond((giant, mad) -> formSecond(player))
                .onEnd((giant, why) -> formGone(player, why))
                .grow(player);                                              // (Werewolf Form ends first, if it was on)
        CompoundTag data = player.getPersistentData();
        data.putString(FORM_TAG, tag);
        data.putDouble(FORM_HEIGHT, height);
        ConjuredArmor.of(FORM_ARMOR).lasts(ticks).cut(FORM_CUT).wear(player);
        Zone.around(player, FORM_AURA)
                .lasts(forever ? AbominationFx.FOREVER : ticks)
                .every(20)
                .onInside((enemy, inside) -> {
                    if (inside == 0 || inside % FORM_AURA_EVERY != 0) return;
                    int level = Abomination.curse(enemy, player, 1, Misfortune.MAX_LEVEL, Abomination.curseTicks(player, FORM_CURSE_TICKS), false);
                    if (level > 0) AbominationFx.baneMistOn(sl, enemy, level);
                })
                .open(player, sl, FORM_ZONE);
        if (forever) AbominationFx.statusForever(player, Status.ABOMINATION);
        else AbominationFx.status(player, Status.ABOMINATION, ticks);
        formSecond(player);
        return AbilityWheel.Result.DONE;
    }

    /** True while the player is the vessel. */
    static boolean inForm(Player player) {
        return GiantForm.isGiant(player) && player.getPersistentData().contains(FORM_TAG);
    }

    /** Once a second in the form: its strength, and the curses drifting off it. */
    private static void formSecond(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Spells.keepEffect(player, MobEffects.STRENGTH, FORM_STRENGTH, 40);
        AbominationFx.abominationTick(sl, player, FORM_AURA);
    }

    /** Someone hurt the vessel: it is cursed. */
    static void hurtInForm(Player player, ServerLevel sl, LivingEntity attacker) {
        int level = Abomination.curse(attacker, player, 1, Misfortune.MAX_LEVEL, Abomination.curseTicks(player, FORM_CURSE_TICKS), false);
        if (level > 0) AbominationFx.abominationCursed(sl, attacker);
    }

    private static void formGone(Player player, GiantForm.Ending why) {
        CompoundTag data = player.getPersistentData();
        ConjuredArmor.takeOff(player, FORM_ARMOR);
        Zone.close(player, FORM_ZONE);
        AbominationFx.statusEnd(player, Status.ABOMINATION);
        Abomination.removeOwn(player, MobEffects.STRENGTH, FORM_STRENGTH);
        if (player.level() instanceof ServerLevel sl && player.isAlive()) {
            AbominationFx.abominationFormEnd(sl, player, data.getDoubleOr(FORM_HEIGHT, player.getBbHeight()), data.getStringOr(FORM_TAG, ""));
            if (why == GiantForm.Ending.TIME || why == GiantForm.Ending.ENDED) calamity(player, sl);
        }
        data.remove(FORM_TAG);
        data.remove(FORM_HEIGHT);
    }

    /** Every curse comes due: each enemy near the vessel that carries this player's Misfortune suffers a calamity. */
    private static void calamity(Player player, ServerLevel sl) {
        UUID mine = player.getUUID();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), FORM_CALAMITY_RADIUS)) {
            if (!mine.equals(Misfortune.sourceId(enemy))) continue;
            AbominationFx.calamity(sl, enemy);
            Misfortune.befall(enemy, Misfortune.Mishap.CALAMITY);
        }
    }

    // =========================
    // Called from Abomination
    // =========================

    /** After a relog or a death: start clean (the form and its aura did not survive either). */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(FORM_TAG);
        data.remove(FORM_HEIGHT);
    }
}
