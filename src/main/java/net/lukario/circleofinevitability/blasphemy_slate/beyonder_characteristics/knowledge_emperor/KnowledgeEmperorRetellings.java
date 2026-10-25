package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Misfortune;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Tale;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Tellings;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Scrolls;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Study;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Knowledge Emperor, key 6: Retellings and Prophecy (Mysticologist, Sequence 4; the last three from Clairvoyant, 3).
 * docs/KNOWLEDGE_EMPEROR.md, "Key 6 wheel".
 *
 *   Sleeping Beauty   a golden spindle turns: enemies near fall asleep; the blow that wakes one does half again as
 *                     much. Bosses and players are only slowed hard. Scrollable
 *   Avalon            the Isle of the Blessed rises as mist round you: allies heal and are cleansed, enemies slowed
 *                     and cannot heal
 *   Achilles' Heel    the single weak spot: the enemy looked at is Understood, takes more damage and your hits on it
 *                     go through armor
 *   Pandora's Box     a different misfortune on every enemy near, then hope for you and your allies. Scrollable
 *   Foresight         ghosts of the blows coming, and the next few hits dodged. From Knowledge Emperor (1) longer, more
 *   Prophecy of Ruin  a fate pronounced: after a countdown, ruin that nothing stops; half against the stronger, and a
 *                     tenth comes back on you
 *
 * The first four are Retellings: Obscurity (combat/Tellings) makes each weaker the more often it was told in the last
 * five minutes, and all four share a short wait. Sneak casts the last pick again without the wheel (no cooldown,
 * double the cost). Every cost goes through KnowledgeEmperor.costFor, every cooldown through cooldownFor. The looks are
 * KnowledgeEmperorFx's; every number is right below.
 */
public final class KnowledgeEmperorRetellings {

    private KnowledgeEmperorRetellings() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int RETELLINGS_SEQUENCE = 4;                    // the key
    static final float QUICK_CAST_COST = 2f;

    // Obscurity (combat/Tellings): every telling in the last OBSCURITY_WINDOW takes OBSCURITY_STEP off, never under OBSCURITY_FLOOR
    static final int OBSCURITY_WINDOW = 6000;
    static final float OBSCURITY_STEP = 0.2f;
    static final float OBSCURITY_FLOOR = 0.4f;
    static final int TALE_WAIT = 200;                    // the four Retellings share this wait

    // Sleeping Beauty
    static final int BEAUTY_SEQUENCE = 4;
    static final int BEAUTY_COST = 6000;                 // Sequence 4 holds 32400
    static final int BEAUTY_COOLDOWN = 800;
    static final double BEAUTY_RADIUS = 8.0;
    static final int BEAUTY_SLEEP = 120;
    static final int BEAUTY_SLOW = 60;                   // bosses and players: Slowness IV this long instead
    static final float BEAUTY_WAKING_BLOW = 1.5f;

    // Avalon
    static final int AVALON_SEQUENCE = 4;
    static final int AVALON_COST = 6000;
    static final int AVALON_COOLDOWN = 800;
    static final double AVALON_RADIUS = 10.0;
    static final int AVALON_TICKS = 240;
    static final float AVALON_HEAL = 2f;                 // a heart a second

    // Achilles' Heel
    static final int HEEL_SEQUENCE = 4;
    static final int HEEL_COST = 5000;
    static final int HEEL_COOLDOWN = 800;
    static final double HEEL_RANGE = 24.0;
    static final int HEEL_TICKS = 160;
    static final float HEEL_VULNERABLE = 0.4f;

    // Pandora's Box
    static final int BOX_SEQUENCE = 3;
    static final int BOX_COST = 9000;
    static final int BOX_COOLDOWN = 900;
    static final double BOX_RADIUS = 10.0;
    static final int BOX_TICKS = 160;

    // Foresight
    static final int FORESIGHT_SEQUENCE = 3;
    static final int FORESIGHT_COST = 5000;
    static final int FORESIGHT_COOLDOWN = 500;
    static final int FORESIGHT_TICKS = 200;
    static final int FORESIGHT_DODGES = 3;
    static final double FORESIGHT_SLIDE = 2.0;
    static final double FORESIGHT_SEES = 16.0;          // the ghosts of blows from mobs this close
    static final int EMPEROR_FORESIGHT_SEQUENCE = 1;     // Knowledge Emperor:
    static final int EMPEROR_FORESIGHT_TICKS = 400;
    static final int EMPEROR_FORESIGHT_DODGES = 6;

    // Prophecy of Ruin
    static final int PROPHECY_SEQUENCE = 3;
    static final int PROPHECY_COST = 9000;
    static final int PROPHECY_COOLDOWN = 900;
    static final double PROPHECY_RANGE = 24.0;
    static final int PROPHECY_SECONDS = 4;
    static final float PROPHECY_DAMAGE = 80f;
    static final int PROPHECY_UNDERSTOOD = 200;
    static final float PROPHECY_AGAINST_STRONGER = 0.5f;
    static final float PROPHECY_REBOUND = 0.1f;

    // =========================
    // Names and what is kept
    // =========================
    static final String SCROLL_BEAUTY = "knowledge_emperor_sleeping_beauty";
    static final String SCROLL_BOX = "knowledge_emperor_pandoras_box";
    static final String TALE_READY = "knowledge_emperor_tale_ready";
    static final String AVALON = "knowledge_emperor_avalon";
    static final String FORESIGHT_UNTIL = "knowledge_emperor_foresight_until";

    /** Who sleeps under Sleeping Beauty (the blow that wakes them is stronger): sleeper -> its look's tag. Not saved. */
    static final Map<UUID, String> SLEEPERS = new HashMap<>();

    /** Achilles' Heel: target -> whose heel it is and until when (their hits on it go through armor). Not saved. */
    private record Heel(UUID caster, long until, String tag) {}
    static final Map<UUID, Heel> HEELS = new HashMap<>();
    /** A hit on a heel, before armor: what it lands with, and in which tick (only during the hit). */
    private record Through(float amount, long tick) {}
    static final Map<UUID, Through> THROUGH_ARMOR = new HashMap<>();

    static {
        Slumber.addWakeListener((sleeper, kind) -> {
            String tag = SLEEPERS.remove(sleeper.getUUID());
            if (tag != null && sleeper.level() instanceof ServerLevel sl) {
                KnowledgeEmperorFx.awaken(sl, sleeper, tag);
                if (sleeper instanceof Player woken) KnowledgeEmperorFx.statusEnd(woken, Status.ASLEEP);
            }
        });
    }

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("knowledge_emperor_retellings", "Retellings")
            .radius(80)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(KnowledgeEmperor::cooldownFor)
            .entry("sleeping_beauty", KnowledgeEmperorFx.WHEEL_SLEEPING_BEAUTY, "Sleeping Beauty", BEAUTY_SEQUENCE,
                    BEAUTY_COST, BEAUTY_COOLDOWN, KnowledgeEmperorRetellings::sleepingBeauty)
            .costBy(KnowledgeEmperor::costFor)
            .entry("avalon", KnowledgeEmperorFx.WHEEL_AVALON, "Avalon", AVALON_SEQUENCE,
                    AVALON_COST, AVALON_COOLDOWN, KnowledgeEmperorRetellings::avalon)
            .costBy(KnowledgeEmperor::costFor)
            .entry("achilles_heel", KnowledgeEmperorFx.WHEEL_ACHILLES_HEEL, "Achilles' Heel", HEEL_SEQUENCE,
                    HEEL_COST, HEEL_COOLDOWN, KnowledgeEmperorRetellings::achillesHeel)
            .costBy(KnowledgeEmperor::costFor)
            .entry("pandoras_box", KnowledgeEmperorFx.WHEEL_PANDORAS_BOX, "Pandora's Box", BOX_SEQUENCE,
                    BOX_COST, BOX_COOLDOWN, KnowledgeEmperorRetellings::pandorasBox)
            .costBy(KnowledgeEmperor::costFor)
            .entry("foresight", KnowledgeEmperorFx.WHEEL_FORESIGHT, "Foresight", FORESIGHT_SEQUENCE,
                    FORESIGHT_COST, FORESIGHT_COOLDOWN, KnowledgeEmperorRetellings::foresight)
            .costBy(KnowledgeEmperor::costFor)
            .entry("prophecy_of_ruin", KnowledgeEmperorFx.WHEEL_PROPHECY_OF_RUIN, "Prophecy of Ruin", PROPHECY_SEQUENCE,
                    PROPHECY_COST, PROPHECY_COOLDOWN, KnowledgeEmperorRetellings::prophecyOfRuin)
            .costBy(KnowledgeEmperor::costFor)
            .onCast((player, sl, entry) -> KnowledgeEmperor.afterCast(player, entry.cost()));

    static void registerScrolls() {
        Scrolls.register(SCROLL_BEAUTY, "Sleeping Beauty", BEAUTY_COST, (reader, sl, power) -> beauty(reader, sl, power));
        Scrolls.register(SCROLL_BOX, "Pandora's Box", BOX_COST, (reader, sl, power) -> box(reader, sl, power));
    }

    // =========================
    // Ability 6 - Retellings and Prophecy
    // Normal: the wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knowledgeEmperorRetellings(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnowledgeEmperor.ready(player, bypassClassCheck, RETELLINGS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Obscurity: how strong a Retelling is now, and the wait the four share
    // =========================

    /** True (and says so) while the four Retellings' shared wait runs. */
    private static boolean taleWaiting(LivingEntity player, ServerLevel sl) {
        long left = player.getPersistentData().getLongOr(TALE_READY, 0L) - sl.getGameTime();
        if (left <= 0 || left > TALE_WAIT) return false;
        Spells.tell(player, Component.literal("The last tale still hangs in the air (" + ((left + 19) / 20) + "s)."));
        return true;
    }

    /** A Retelling is told now: its strength (Obscurity), the opening of the book, and the shared wait. */
    private static float tell(LivingEntity player, ServerLevel sl, Tale tale) {
        String key = "knowledge_emperor_tale_" + tale.name().toLowerCase();
        float strength = Tellings.strength(player, key, OBSCURITY_WINDOW, OBSCURITY_STEP, OBSCURITY_FLOOR);
        Tellings.tell(player, key);
        player.getPersistentData().putLong(TALE_READY, sl.getGameTime() + TALE_WAIT);
        KnowledgeEmperorFx.tale(sl, player, tale, strength);
        if (strength < 1f) Spells.tell(player, Component.literal("The tale is too well known: it works at " + Math.round(strength * 100) + "%."));
        return strength;
    }

    // =========================
    // Sleeping Beauty: every enemy within BEAUTY_RADIUS falls asleep at KnowledgeEmperorFx.SPINDLE_SLEEPS for
    // BEAUTY_SLEEP (status/Slumber: the first blow wakes it, and that blow does BEAUTY_WAKING_BLOW times as much).
    // Bosses and players are only slowed hard for BEAUTY_SLOW. Scrollable.
    // =========================
    static AbilityWheel.Result sleepingBeauty(LivingEntity player, ServerLevel sl) {
        if (taleWaiting(player, sl)) return AbilityWheel.Result.REFUSED;
        float strength = tell(player, sl, Tale.SLEEPING_BEAUTY);
        beauty(player, sl, strength);
        Scrolls.cast(player, SCROLL_BEAUTY);
        return AbilityWheel.Result.DONE;
    }

    private static boolean beauty(LivingEntity player, ServerLevel sl, float power) {
        Vec3 center = player.position();
        KnowledgeEmperorFx.sleepingBeauty(sl, player, center, BEAUTY_RADIUS);
        Later.run(sl, KnowledgeEmperorFx.SPINDLE_SLEEPS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, BEAUTY_RADIUS)) {
                String tag = "ke_sleep_" + enemy.getId();
                if (KnowledgeEmperor.isBoss(enemy) || enemy instanceof Player) {
                    int ticks = Math.round(BEAUTY_SLOW * power);
                    enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 3));
                    KnowledgeEmperorFx.asleep(sl, enemy, ticks, tag);
                    if (enemy instanceof Player slowed) KnowledgeEmperorFx.status(slowed, Status.ASLEEP, ticks);
                    continue;
                }
                int ticks = Math.round(BEAUTY_SLEEP * power);
                if (!Slumber.put(enemy, ticks, Slumber.Kind.SLEEP)) continue;
                SLEEPERS.put(enemy.getUUID(), tag);
                KnowledgeEmperorFx.asleep(sl, enemy, ticks, tag);
                Study.add(player, enemy, KnowledgeEmperor.ABILITY_STUDY);
            }
        });
        return true;
    }

    // =========================
    // Avalon: the isle stands AVALON_RADIUS round where you tell it, for AVALON_TICKS once risen. Each second allies in
    // it (and you) heal AVALON_HEAL and are cleansed; enemies in it are slowed and cannot heal.
    // =========================
    static AbilityWheel.Result avalon(LivingEntity player, ServerLevel sl) {
        if (taleWaiting(player, sl)) return AbilityWheel.Result.REFUSED;
        float strength = tell(player, sl, Tale.AVALON);
        Vec3 ground = Spells.groundUnder(player, sl, player);
        int ticks = Math.round(AVALON_TICKS * strength);
        String tag = "ke_avalon_" + player.getId() + "_" + sl.getGameTime();
        int risen = KnowledgeEmperorFx.AVALON_RISEN;
        KnowledgeEmperorFx.avalon(sl, ground, AVALON_RADIUS, ticks + risen, tag);
        KnowledgeEmperorFx.status(player, Status.AVALON, ticks + risen);
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                KnowledgeEmperorFx.avalonEnd(sl, ground, AVALON_RADIUS, tag);
                return;
            }
            Zone.at(ground, AVALON_RADIUS).height(5).lasts(ticks).every(20)
                    .affects(one -> true)
                    .onInside((one, inside) -> {
                        if (Spells.isEnemy(player, one)) {
                            Spells.keepEffect(one, MobEffects.SLOWNESS, 1, 40);
                            NoHeal.apply(one, 40);
                        } else {
                            bless(sl, one, strength);
                        }
                    })
                    .onPulse(left -> {
                        KnowledgeEmperorFx.avalonTick(sl, ground, AVALON_RADIUS);
                        if (player.position().distanceTo(ground) <= AVALON_RADIUS) bless(sl, player, strength);
                    })
                    .onEnd(() -> {
                        KnowledgeEmperorFx.avalonEnd(sl, ground, AVALON_RADIUS, tag);
                        KnowledgeEmperorFx.statusEnd(player, Status.AVALON);
                    })
                    .open(player, sl, AVALON);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void bless(ServerLevel sl, LivingEntity ally, float strength) {
        Spells.cleanse(ally);
        if (ally.getHealth() < ally.getMaxHealth()) ally.heal(AVALON_HEAL * strength);
        KnowledgeEmperorFx.avalonBlessed(sl, ally);
    }

    // =========================
    // Achilles' Heel: the enemy you look at (HEEL_RANGE) is Understood for HEEL_TICKS (Study.hold), takes
    // HEEL_VULNERABLE more damage, and your hits on it go through armor and everything else that softens a hit.
    // =========================
    static AbilityWheel.Result achillesHeel(LivingEntity player, ServerLevel sl) {
        if (taleWaiting(player, sl)) return AbilityWheel.Result.REFUSED;
        LivingEntity target = Spells.aimEnemy(player, sl, HEEL_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no one whose weak spot you could name."));
            return AbilityWheel.Result.REFUSED;
        }
        float strength = tell(player, sl, Tale.ACHILLES_HEEL);
        int ticks = Math.round(HEEL_TICKS * strength);
        String tag = "ke_heel_" + target.getId();
        KnowledgeEmperorFx.achillesHeel(sl, player, target, ticks + KnowledgeEmperorFx.ARROW_LANDS, tag);
        Later.run(sl, KnowledgeEmperorFx.ARROW_LANDS, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            Study.hold(player, target, Study.FULL, ticks);
            Vulnerable.add(target, HEEL_VULNERABLE * strength, HEEL_VULNERABLE * strength, ticks);
            HEELS.put(target.getUUID(), new Heel(player.getUUID(), sl.getGameTime() + ticks, tag));
            if (target instanceof Player named) KnowledgeEmperorFx.status(named, Status.ACHILLES, ticks);
            Later.run(sl, ticks, () -> {
                Heel heel = HEELS.get(target.getUUID());
                if (heel == null || !heel.tag().equals(tag) || heel.until() > sl.getGameTime()) return;
                HEELS.remove(target.getUUID());
                KnowledgeEmperorFx.achillesEnd(sl, target, tag);
            });
        });
        return AbilityWheel.Result.DONE;
    }

    /** Is this a hit by the heel's owner on the heel? (while it lasts) */
    private static boolean onTheHeel(LivingEntity target, LivingEntity attacker) {
        Heel heel = HEELS.get(target.getUUID());
        return heel != null && heel.caster().equals(attacker.getUUID()) && heel.until() > target.level().getGameTime();
    }

    // =========================
    // Pandora's Box: at KnowledgeEmperorFx.BOX_OPENS every enemy within BOX_RADIUS gets a different misfortune, in turn
    // (blindness, slowness, weakness, poison, nausea, no healing) for BOX_TICKS; at HOPE_RISES you and your allies
    // within BOX_RADIUS get Regeneration II for as long. Scrollable.
    // =========================
    static AbilityWheel.Result pandorasBox(LivingEntity player, ServerLevel sl) {
        if (taleWaiting(player, sl)) return AbilityWheel.Result.REFUSED;
        float strength = tell(player, sl, Tale.PANDORAS_BOX);
        box(player, sl, strength);
        Scrolls.cast(player, SCROLL_BOX);
        return AbilityWheel.Result.DONE;
    }

    private static boolean box(LivingEntity player, ServerLevel sl, float power) {
        int ticks = Math.round(BOX_TICKS * power);
        KnowledgeEmperorFx.pandorasBox(sl, player, BOX_RADIUS);
        Later.run(sl, KnowledgeEmperorFx.BOX_OPENS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Misfortune[] kinds = Misfortune.values();
            int next = sl.getRandom().nextInt(kinds.length);
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), BOX_RADIUS)) {
                Misfortune kind = kinds[next++ % kinds.length];
                befall(enemy, kind, ticks);
                KnowledgeEmperorFx.misfortune(sl, enemy, kind, ticks, "ke_misfortune_" + enemy.getId());
                if (enemy instanceof Player cursed) KnowledgeEmperorFx.status(cursed, Status.MISFORTUNE, ticks);
                Study.add(player, enemy, KnowledgeEmperor.ABILITY_STUDY);
            }
        });
        Later.run(sl, KnowledgeEmperorFx.HOPE_RISES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            hope(sl, player, ticks);
            for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), BOX_RADIUS)) hope(sl, ally, ticks);
        });
        return true;
    }

    private static void befall(LivingEntity enemy, Misfortune kind, int ticks) {
        switch (kind) {
            case BLINDNESS -> Blind.apply(enemy, ticks);
            case SLOWNESS -> enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 1));
            case WEAKNESS -> enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 0));
            case POISON -> enemy.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0));
            case NAUSEA -> enemy.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ticks, 0));
            case NO_HEALING -> NoHeal.apply(enemy, ticks);
        }
    }

    private static void hope(ServerLevel sl, LivingEntity ally, int ticks) {
        ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
        KnowledgeEmperorFx.hope(sl, ally);
    }

    // =========================
    // Foresight: for FORESIGHT_TICKS the blows coming show as ghosts (mobs within FORESIGHT_SEES that hunt you), and
    // the next FORESIGHT_DODGES hits on you are dodged (status/Substitute), each sliding you FORESIGHT_SLIDE aside.
    // Knowledge Emperor (1): EMPEROR_FORESIGHT_TICKS and EMPEROR_FORESIGHT_DODGES.
    // =========================
    static AbilityWheel.Result foresight(LivingEntity player, ServerLevel sl) {
        boolean emperor = Beyonder.hasSequence(player, EMPEROR_FORESIGHT_SEQUENCE);
        int ticks = emperor ? EMPEROR_FORESIGHT_TICKS : FORESIGHT_TICKS;
        int dodges = emperor ? EMPEROR_FORESIGHT_DODGES : FORESIGHT_DODGES;
        long until = sl.getGameTime() + ticks;
        String tag = "ke_foresight_" + player.getId();
        player.getPersistentData().putLong(FORESIGHT_UNTIL, until);
        KnowledgeEmperorFx.foresightEnd(sl, player, tag);
        KnowledgeEmperorFx.foresight(sl, player, ticks, tag);
        KnowledgeEmperorFx.status(player, Status.FORESIGHT, ticks);
        Substitute.arm(player, ticks, dodges, (saved, source, amount, level) -> {
            Vec3 from = player.position();
            Vec3 aside = Spells.rightOf(player).scale(level.getRandom().nextBoolean() ? 1 : -1);
            Vec3 to = Spells.freeSpotNear(player, level, from, aside, FORESIGHT_SLIDE);
            if (to != null) {
                player.teleportTo(to.x, to.y, to.z);
                player.fallDistance = 0;
            }
            KnowledgeEmperorFx.foresightDodge(level, player, from, to != null ? to : from);
            if (!Substitute.isArmed(player)) endForesight(player, level, tag);           // that was the last dodge
        });
        Later.run(sl, ticks, () -> {
            if (player.getPersistentData().getLongOr(FORESIGHT_UNTIL, 0L) == until) endForesight(player, sl, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void endForesight(LivingEntity player, ServerLevel sl, String tag) {
        player.getPersistentData().remove(FORESIGHT_UNTIL);
        KnowledgeEmperorFx.foresightEnd(sl, player, tag);
        KnowledgeEmperorFx.statusEnd(player, Status.FORESIGHT);
    }

    /** Twice a second while Foresight lasts: the ghost of each blow being readied against you. */
    private static void foresightTick(LivingEntity player, ServerLevel sl) {
        if (!KnowledgeEmperor.isActive(player, FORESIGHT_UNTIL)) return;
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(FORESIGHT_SEES),
                mob -> mob.isAlive() && mob.getTarget() == player)) {
            Vec3 at = player.getBoundingBox().getCenter().add(mob.position().subtract(player.position()).normalize().scale(0.6));
            KnowledgeEmperorFx.foresightGhost(sl, mob, at);
        }
    }

    // =========================
    // Prophecy of Ruin: a fate on the enemy you look at (PROPHECY_RANGE): a PROPHECY_SECONDS countdown, then
    // PROPHECY_DAMAGE that nothing dodges, blocks or softens (Spells.strikeTrue), and it is Understood for
    // PROPHECY_UNDERSTOOD. Against a boss or a player of a stronger Sequence it is PROPHECY_AGAINST_STRONGER as strong
    // and PROPHECY_REBOUND of it comes back on you. It fails if the target dies or gets out of reach.
    // =========================
    static AbilityWheel.Result prophecyOfRuin(LivingEntity player, ServerLevel sl) {
        if (Countdown.isRunning(player)) {
            Spells.tell(player, Component.literal("You are already pronouncing a fate."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, PROPHECY_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no one to pronounce a fate on."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean stronger = KnowledgeEmperor.isBoss(target)
                || (target instanceof Player other && SoulCore.getSequence(other) < Beyonders.sequenceOf(player));
        String tag = "ke_prophecy_" + target.getId();
        KnowledgeEmperorFx.prophecy(sl, player, target, PROPHECY_SECONDS, tag);
        if (target instanceof Player doomed) KnowledgeEmperorFx.status(doomed, Status.DOOMED, PROPHECY_SECONDS * 20);
        Countdown.seconds(PROPHECY_SECONDS)
                .colors(0xE8C4FF, 0xE04050)
                .onSecond(left -> {
                    if (left < PROPHECY_SECONDS) KnowledgeEmperorFx.prophecyTick(sl, target, left);
                })
                .keepWhile(() -> target.isAlive() && target.level() == sl && player.distanceTo(target) <= PROPHECY_RANGE)
                .onFinish(() -> {
                    float damage = PROPHECY_DAMAGE * (stronger ? PROPHECY_AGAINST_STRONGER : 1f);
                    KnowledgeEmperorFx.prophecyFulfilled(sl, target, tag);
                    Spells.strikeTrue(player, target, damage);
                    if (target.isAlive()) Study.hold(player, target, Study.FULL, PROPHECY_UNDERSTOOD);
                    if (stronger) {
                        player.hurt(player.damageSources().magic(), damage * PROPHECY_REBOUND);
                        KnowledgeEmperorFx.prophecyRebound(sl, player);
                    }
                })
                .onCancel(() -> {
                    if (target.level() == sl) KnowledgeEmperorFx.prophecyFails(sl, target, tag);
                    if (target instanceof Player doomed) KnowledgeEmperorFx.statusEnd(doomed, Status.DOOMED);
                })
                .start(player);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Hooks from KnowledgeEmperor's events
    // =========================

    /** Every tick, for every player. */
    static void tick(LivingEntity player, ServerLevel sl) {
        if (player.tickCount % 10 == 0) foresightTick(player, sl);
    }

    /** Before armor (KnowledgeEmperor calls it at HIGH): the blow that wakes a Sleeping Beauty sleeper. */
    static void hurtEarly(LivingHurtEvent event) {
        if (SLEEPERS.isEmpty()) return;
        LivingEntity target = event.getEntity();
        if (!SLEEPERS.containsKey(target.getUUID()) || event.getAmount() <= 0f) return;
        event.setAmount(event.getAmount() * BEAUTY_WAKING_BLOW);
    }

    /** At LOWEST, after every bonus: what a hit on an Achilles' Heel lands with, before armor. */
    static void hurtLate(LivingHurtEvent event) {
        if (HEELS.isEmpty()) return;
        LivingEntity target = event.getEntity();
        if (event.getSource().getEntity() instanceof LivingEntity attacker && onTheHeel(target, attacker)) {   // a player's or a Beyonder mob's
            THROUGH_ARMOR.put(target.getUUID(), new Through(event.getAmount(), target.level().getGameTime()));
        }
    }

    /** After armor (LOW): a hit on the heel loses nothing to it. */
    static void damaged(LivingDamageEvent event) {
        if (THROUGH_ARMOR.isEmpty()) return;
        Through whole = THROUGH_ARMOR.remove(event.getEntity().getUUID());
        if (whole == null || whole.tick() != event.getEntity().level().getGameTime()) return;   // a hit that never landed
        if (whole.amount() > event.getAmount()) event.setAmount(whole.amount());
    }

    static void died(LivingEntity dead, ServerLevel sl) {
        SLEEPERS.remove(dead.getUUID());
        Heel heel = HEELS.remove(dead.getUUID());
        if (heel != null) KnowledgeEmperorFx.achillesEnd(sl, dead, heel.tag());
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        player.getPersistentData().remove(FORESIGHT_UNTIL);
        Zone.close(player, AVALON);
    }
}
