package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.camera.DetachedCamera;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.Enfeebled;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.Madness;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Key 3, the Canvas wheel (docs/pathways/observer/OBSERVER.md, "Key 3: Canvas"). Every entry is a Painting
 * (combat/Painting): painted in the air where you aim (PAINT_RANGE, on the block face it meets or in the air), it hangs
 * and waits; an enemy that keeps it in view for the stare takes its effect. Golden Sun, Boat in a Storm and Self
 * Portrait are field Paintings that work from the moment they hang. Every number is a fresh canvas: a Painting fades to
 * two thirds over its life (Painting.power), and from Literature Enthusiast (6) Paintings are fully real (REAL_*).
 */
final class ObserverCanvas {

    private ObserverCanvas() {}

    // =========================
    // Every Painting
    // =========================
    static final double PAINT_RANGE = 20.0;
    static final double PAINT_IN_AIR = 8.0;                 // nothing in the way: it hangs this far ahead
    static final int HANG_TICKS = 900;                      // 45 seconds...
    static final int HANG_TICKS_REAL = 1800;                // ...90 from Literature Enthusiast (6)
    static final int STARE_TICKS = 60;                      // 3 seconds in view...
    static final int STARE_TICKS_REAL = 40;                 // ...2 from Literature Enthusiast (6)
    static final double STARE_RANGE = 20.0;
    static final double STARE_ANGLE = 55.0;
    static final float REAL_STRENGTH = 1.3f;                // effects 30% stronger from Literature Enthusiast (6)
    static final int MAX_EACH = 3;                          // of each kind at once; a fourth replaces the oldest

    // Riot of Colours (Painter 7): Nausea II, Falter, Madness; it keeps hanging, and every second a struck enemy keeps
    // looking adds a Madness; at its height (status/Madness, 10) the enemy collapses into Slumber
    static final int RIOT_SEQUENCE = Observer.PAINTER;
    static final int RIOT_COST = 1250;
    static final int RIOT_TICKS = 120;
    static final float RIOT_FALTER = 0.4f;
    static final int RIOT_MADNESS = 2;
    static final int RIOT_SLUMBER = 100;
    static final String RIOT = "observer_riot";            // status/Marks on the struck

    // Itching House (Painter 7): Enfeebled, Falter, Slowness I, no sprinting
    static final int ITCHING_SEQUENCE = Observer.PAINTER;
    static final int ITCHING_COST = 1250;
    static final int ITCHING_TICKS = 160;
    static final float ITCHING_ENFEEBLE = 0.4f;
    static final float ITCHING_FALTER = 0.25f;
    static final String ITCHING = "observer_itching";

    // Golden Sun (Painter 7), a field: allies warmed and cleansed, the evil and undead burned and unable to heal,
    // the summons and marionettes of enemies unmade once
    static final int SUN_SEQUENCE = Observer.PAINTER;
    static final int SUN_COST = 1250;
    static final double SUN_RADIUS = 7.0;
    static final int SUN_TICKS = 240;
    static final float SUN_BURN = 5f;

    // Boat in a Storm (Literature Enthusiast 6), a field: fire out, allies fire resistant, enemies slowed and shoved
    static final int STORM_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int STORM_COST = 2450;
    static final double STORM_RADIUS = 9.0;
    static final int STORM_TICKS = 200;
    static final double STORM_SHOVE = 0.8;

    // Self Portrait (Literature Enthusiast 6), a field, one at a time: cast again to look through it for PORTRAIT_LOOK;
    // while it hangs, what you cast is repeated from it at PORTRAIT_ECHO (combat/Echo)
    static final int PORTRAIT_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int PORTRAIT_COST = 2450;
    static final int PORTRAIT_TICKS = 1800;
    static final float PORTRAIT_ECHO = 0.5f;
    static final double PORTRAIT_REACH = 64.0;              // you echo from it only this close
    static final String PORTRAIT_VIEW = "observer_self_portrait";        // data/.../camera_presets
    // what the portrait does not repeat: your own sight and fiction, and the Paintings themselves
    static final Set<String> NOT_ECHOED = Set.of("totem_territory", "true_sight", "many_angles", "read_intent",
            "riot_of_colours", "itching_house", "golden_sun", "boat_in_a_storm", "self_portrait", "giants_fist",
            "painted_mark", "hide_in_painting", "painted_face", "manifest_knight", "manifest_archer");

    // Giant's Fist (Literature Enthusiast 6): the painted giant's fist smashes down on whoever saw it
    static final int FIST_SEQUENCE = Observer.LITERATURE_ENTHUSIAST;
    static final int FIST_COST = 3000;
    static final double FIST_RADIUS = 5.0;
    static final float FIST_DAMAGE = 90f;
    static final int FIST_HEAVY = 2;
    static final int FIST_HEAVY_TICKS = 80;
    static final int FIST_ROOT = 30;

    /** Where a canvas hangs: its middle, and which way its front looks. */
    record Spot(Vec3 at, Vec3 facing) {}

    // =========================
    // Painting and hanging
    // =========================

    /** Where 'player' is aiming a canvas: on the block face it meets (a floor: standing on it), else in the air. */
    static Spot spotFor(LivingEntity player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 flat = Spells.flatLook(player);
        BlockHitResult hit = sl.clip(new ClipContext(eye, eye.add(look.scale(PAINT_RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return new Spot(eye.add(look.scale(PAINT_IN_AIR)), flat.scale(-1));
        Direction face = hit.getDirection();
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        if (face.getAxis().isVertical()) {
            // a floor or a ceiling: the canvas stands upright on it, facing you
            Vec3 at = hit.getLocation().add(0, face == Direction.UP ? 1.2 : -1.2, 0).subtract(flat.scale(0.2));
            return new Spot(at, flat.scale(-1));
        }
        return new Spot(hit.getLocation().add(normal.scale(0.06)), normal);
    }

    static int paintTicks(LivingEntity player) {
        if (Observer.grown(player, Observer.DIMENSIONAL_SHADOW)) return 0;
        return Observer.grown(player, Observer.PIXIE) ? ObserverFx.PAINT_TICKS_PIXIE : ObserverFx.PAINT_TICKS;
    }

    static float strength(LivingEntity player) {
        return Observer.grown(player, Observer.LITERATURE_ENTHUSIAST) ? REAL_STRENGTH : 1f;
    }

    static int hangTicks(LivingEntity player) {
        return Observer.grown(player, Observer.LITERATURE_ENTHUSIAST) ? HANG_TICKS_REAL : HANG_TICKS;
    }

    static int stareTicks(LivingEntity player) {
        return Observer.grown(player, Observer.LITERATURE_ENTHUSIAST) ? STARE_TICKS_REAL : STARE_TICKS;
    }

    /** Paints 'look' at the spot (the strokes in the air), then runs 'hang' once the painting is done. */
    private static void paintThen(LivingEntity player, ServerLevel sl, ObserverFx.Painting look, Spot spot, Runnable hang) {
        int ticks = paintTicks(player);
        ObserverFx.paint(sl, player, spot.at(), look, ticks);
        if (ticks <= 0) {
            hang.run();
            return;
        }
        Later.run(sl, ticks, () -> {
            if (Spells.casterStillHere(player, sl)) hang.run();
        });
    }

    /** A Painting that has to be seen: hangs, waits for the stare, then 'seen' (with the canvas's power by then). */
    private static Painting.Hung hangSeen(LivingEntity player, ServerLevel sl, Spot spot, ObserverFx.Painting look, String kind,
                                          Painting.Seen seen, Painting.Seen keepLooking) {
        String tag = ObserverFx.tagFor(kind);
        int lasts = hangTicks(player);
        ObserverFx.hang(sl, look, spot.at(), spot.facing(), lasts, tag);
        Painting painting = Painting.of(kind)
                .lasts(lasts).stare(stareTicks(player)).range(STARE_RANGE).halfAngle(STARE_ANGLE)
                .onStare((hung, viewer, progress) -> ObserverFx.paintingStare(sl, look, hung.spot(), viewer, progress))
                .onSeen((hung, viewer) -> {
                    ObserverFx.paintingSeen(sl, look, hung.spot(), viewer);
                    seen.run(hung, viewer);
                })
                .onEnd((hung, spent) -> ObserverFx.paintingEnd(sl, look, hung.spot(), tag, spent));
        if (keepLooking != null) painting.keeps().onKeepLooking(keepLooking);
        return painting.hang(player, sl, spot.at(), spot.facing(), MAX_EACH);
    }

    /** A field Painting: hangs for 'lasts', and 'field' every second from the moment it hangs. */
    private static Painting.Hung hangField(LivingEntity player, ServerLevel sl, Spot spot, ObserverFx.Painting look, String kind,
                                           int lasts, int max, Painting.Field field, Runnable ended) {
        String tag = ObserverFx.tagFor(kind);
        ObserverFx.hang(sl, look, spot.at(), spot.facing(), lasts, tag);
        return Painting.of(kind)
                .lasts(lasts)
                .onField(20, field)
                .onEnd((hung, spent) -> {
                    ObserverFx.paintingEnd(sl, look, hung.spot(), tag, spent);
                    if (ended != null) ended.run();
                })
                .hang(player, sl, spot.at(), spot.facing(), max);
    }

    /** One line on what a hung Painting is: who made it, what it does, how long it has left (Read Intent, Painter's Eye). */
    static String describe(Painting.Hung hung) {
        String what = switch (hung.kind()) {
            case "observer_riot_of_colours" -> "Riot of Colours: whoever keeps it in view is struck by nausea and madness";
            case "observer_itching_house" -> "Itching House: whoever keeps it in view itches, weak and slow";
            case "observer_golden_sun" -> "Golden Sun: it warms its maker's side and burns the evil";
            case "observer_boat_in_a_storm" -> "Boat in a Storm: its waves slow and shove its maker's enemies";
            case "observer_self_portrait" -> "a Self Portrait: its maker sees through it, and what they cast is cast again from it";
            case "observer_giants_fist" -> "Giant's Fist: whoever keeps it in view is smashed by the painted giant";
            default -> "a Painting (" + hung.kind() + ")";
        };
        return "Painted by " + hung.owner().getName().getString() + ". " + what + ". "
                + Math.max(1, hung.ticksLeft() / 20) + " s left, " + Math.round(hung.power() * 100) + "% of its power.";
    }

    // =========================
    // Riot of Colours
    // =========================

    static AbilityWheel.Result riotOfColours(ServerPlayer player, ServerLevel sl) {
        Spot spot = spotFor(player, sl);
        float strength = strength(player);
        paintThen(player, sl, ObserverFx.Painting.RIOT_OF_COLOURS, spot, () -> hangSeen(player, sl, spot,
                ObserverFx.Painting.RIOT_OF_COLOURS, "observer_riot_of_colours",
                (hung, victim) -> {
                    float power = hung.power() * strength;
                    String tag = ObserverFx.tagFor("riot");
                    victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, RIOT_TICKS, 1));
                    Falter.apply(victim, RIOT, Math.min(0.9f, RIOT_FALTER * power), RIOT_TICKS);
                    Marks.add(victim, RIOT, 1, RIOT_TICKS);
                    Madness.add(player, victim, Math.max(1, Math.round(RIOT_MADNESS * power)));
                    ObserverFx.riot(sl, victim, RIOT_TICKS, tag);
                    ObserverFx.status(victim, Status.RIOT, RIOT_TICKS);
                    Later.run(sl, RIOT_TICKS, () -> ObserverFx.riotEnd(sl, victim, tag));
                },
                (hung, victim) -> {
                    if (Marks.count(victim, RIOT) <= 0) return;                  // the riot has passed
                    int madness = Madness.add(player, victim, 1);
                    ObserverFx.madness(sl, victim, madness);
                }));
        return AbilityWheel.Result.DONE;
    }

    /** Madness at its height (status/Madness) on one struck by the Riot: it collapses into Slumber. */
    static void riotBreaks(LivingEntity source, LivingEntity target) {
        if (Marks.count(target, RIOT) <= 0 || !(target.level() instanceof ServerLevel sl)) return;
        Marks.clear(target, RIOT);
        Slumber.put(target, RIOT_SLUMBER, Slumber.Kind.SLEEP);
        ObserverFx.madnessCollapse(sl, target, ObserverFx.tagFor("riot_collapse"));
    }

    // =========================
    // Itching House
    // =========================

    static AbilityWheel.Result itchingHouse(ServerPlayer player, ServerLevel sl) {
        Spot spot = spotFor(player, sl);
        float strength = strength(player);
        paintThen(player, sl, ObserverFx.Painting.ITCHING_HOUSE, spot, () -> hangSeen(player, sl, spot,
                ObserverFx.Painting.ITCHING_HOUSE, "observer_itching_house",
                (hung, victim) -> {
                    float power = hung.power() * strength;
                    String tag = ObserverFx.tagFor("itching");
                    Enfeebled.apply(victim, ITCHING, Math.min(0.8f, ITCHING_ENFEEBLE * power), ITCHING_TICKS);
                    Falter.apply(victim, ITCHING, Math.min(0.9f, ITCHING_FALTER * power), ITCHING_TICKS);
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ITCHING_TICKS, 0));
                    Marks.add(victim, ITCHING, 1, ITCHING_TICKS);
                    victim.setSprinting(false);
                    ObserverFx.itching(sl, victim, ITCHING_TICKS, tag);
                    ObserverFx.status(victim, Status.ITCHING, ITCHING_TICKS);
                    Later.run(sl, ITCHING_TICKS, () -> ObserverFx.itchingEnd(sl, victim, tag));
                }, null));
        return AbilityWheel.Result.DONE;
    }

    /** Every tick, for every player: one who itches cannot sprint. */
    static void itchTick(Player player) {
        if (player.isSprinting() && Marks.count(player, ITCHING) > 0) player.setSprinting(false);
    }

    // =========================
    // Golden Sun
    // =========================

    static AbilityWheel.Result goldenSun(ServerPlayer player, ServerLevel sl) {
        Spot spot = spotFor(player, sl);
        float strength = strength(player);
        paintThen(player, sl, ObserverFx.Painting.GOLDEN_SUN, spot, () -> {
            Vec3 ground = Spells.groundAt(player, sl, spot.at());
            String tag = ObserverFx.tagFor("golden_sun_field");
            ObserverFx.goldenSun(sl, ground, SUN_RADIUS, SUN_TICKS, tag);
            Set<UUID> unmade = new HashSet<>();
            hangField(player, sl, spot, ObserverFx.Painting.GOLDEN_SUN, "observer_golden_sun", SUN_TICKS, MAX_EACH,
                    hung -> sunPulse(player, sl, ground, hung.power() * strength, unmade),
                    () -> ObserverFx.goldenSunEnd(sl, ground, tag));
        });
        return AbilityWheel.Result.DONE;
    }

    private static void sunPulse(LivingEntity player, ServerLevel sl, Vec3 ground, float power, Set<UUID> unmade) {
        List<LivingEntity> allies = Spells.alliesAround(player, sl, ground, SUN_RADIUS);
        if (player.position().distanceTo(ground) <= SUN_RADIUS) allies.add(player);
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0));
            Spells.cleanse(ally);
            ObserverFx.sunWarm(sl, ally);
        }
        for (LivingEntity one : Spells.livingAround(player, sl, ground, SUN_RADIUS)) {
            if (unmade.contains(one.getUUID())) continue;
            LivingEntity master = masterOf(sl, one);
            if (master == null || !Spells.isEnemy(player, master)) continue;
            unmade.add(one.getUUID());                                            // the simple exorcism: once each
            ObserverFx.exorcise(sl, one.position());
            if (Marionette.isSummoned(one) || Construct.isBuilt(one)) Dispel.unmake(one);
            else if (one instanceof Mob mob) Marionette.release(mob);
        }
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, SUN_RADIUS)) {
            if (!Spells.isEvil(enemy)) continue;                             // the evil: undead and monsters
            Spells.strike(player, enemy, SUN_BURN * power);
            NoHeal.apply(enemy, 30);
            ObserverFx.sunBurn(sl, enemy);
        }
    }

    /** Who holds a summon, a construct or a marionette, or null for a free creature. */
    private static LivingEntity masterOf(ServerLevel sl, LivingEntity one) {
        UUID id = Marionette.isMarionette(one) || Marionette.isSummoned(one) ? Marionette.masterId(one)
                : Construct.isBuilt(one) ? Construct.ownerOf(one) : null;
        Entity master = id == null ? null : sl.getEntity(id);
        return master instanceof LivingEntity living ? living : null;
    }

    // =========================
    // Boat in a Storm
    // =========================

    static AbilityWheel.Result boatInAStorm(ServerPlayer player, ServerLevel sl) {
        Spot spot = spotFor(player, sl);
        float strength = strength(player);
        paintThen(player, sl, ObserverFx.Painting.BOAT_IN_A_STORM, spot, () -> {
            Vec3 ground = Spells.groundAt(player, sl, spot.at());
            String tag = ObserverFx.tagFor("storm_field");
            ObserverFx.boatInAStorm(sl, ground, STORM_RADIUS, STORM_TICKS, tag);
            int[] wave = {0};
            hangField(player, sl, spot, ObserverFx.Painting.BOAT_IN_A_STORM, "observer_boat_in_a_storm", STORM_TICKS, MAX_EACH,
                    hung -> {
                        ObserverFx.stormWave(sl, ground, STORM_RADIUS, wave[0]++ * 90f);
                        float power = hung.power() * strength;
                        for (LivingEntity one : Spells.livingAround(player, sl, ground, STORM_RADIUS)) one.clearFire();
                        if (player.position().distanceTo(ground) <= STORM_RADIUS) {
                            player.clearFire();
                            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50, 0));
                        }
                        for (LivingEntity ally : Spells.alliesAround(player, sl, ground, STORM_RADIUS)) {
                            ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50, 0));
                        }
                        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, STORM_RADIUS)) {
                            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1));
                            Spells.push(enemy, ground, STORM_SHOVE * power, 0.25);
                        }
                    },
                    () -> ObserverFx.boatInAStormEnd(sl, ground, tag));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Self Portrait
    // =========================

    static boolean hasPortrait(Player player) {
        return !Painting.of(player, "observer_self_portrait").isEmpty();
    }

    static AbilityWheel.Result selfPortrait(ServerPlayer player, ServerLevel sl) {
        List<Painting.Hung> mine = Painting.of(player, "observer_self_portrait");
        if (!mine.isEmpty()) {
            Painting.Hung portrait = mine.getFirst();
            if (portrait.level() != sl || DetachedCamera.active(player)) return AbilityWheel.Result.REFUSED;
            Vec3 eye = portrait.spot().add(portrait.facing().scale(0.7));
            if (!DetachedCamera.at(player, PORTRAIT_VIEW, eye)) {
                Spells.tell(player, Component.literal("Your portrait cannot be looked through from here."));
                return AbilityWheel.Result.REFUSED;
            }
            ObserverFx.selfPortraitLook(sl, portrait.spot(), portrait.facing());
            return AbilityWheel.Result.FREE;
        }
        Spot spot = spotFor(player, sl);
        paintThen(player, sl, ObserverFx.Painting.SELF_PORTRAIT, spot, () -> {
            ObserverFx.status(player, Status.SELF_PORTRAIT, PORTRAIT_TICKS);
            hangField(player, sl, spot, ObserverFx.Painting.SELF_PORTRAIT, "observer_self_portrait", PORTRAIT_TICKS, 1,
                    hung -> { },
                    () -> ObserverFx.statusEnd(player, Status.SELF_PORTRAIT));
        });
        return AbilityWheel.Result.DONE;
    }

    /** After every cast of a player with a Self Portrait hung near: it is cast again from the portrait, at half. */
    static void portraitEcho(ServerPlayer caster, ServerLevel sl, Casts.Cast cast) {
        if (!SoulCore.getPathway(caster).equals(Observer.PATHWAY) || cast.entry() != null && NOT_ECHOED.contains(cast.entry().key())) return;
        List<Painting.Hung> mine = Painting.of(caster, "observer_self_portrait");
        if (mine.isEmpty()) return;
        Painting.Hung portrait = mine.getFirst();
        if (portrait.level() != sl || portrait.spot().distanceTo(caster.position()) > PORTRAIT_REACH) return;
        Vec3 feet = Spells.groundAt(caster, sl, portrait.spot().add(portrait.facing().scale(1.0)));
        Vec3 aim = Spells.aimPoint(caster, sl, 32);
        Echo.repeat(caster, cast, List.of(feet), PORTRAIT_ECHO, i -> ObserverFx.portraitEcho(sl, portrait.spot(), aim));
    }

    // =========================
    // Giant's Fist
    // =========================

    static AbilityWheel.Result giantsFist(ServerPlayer player, ServerLevel sl) {
        Spot spot = spotFor(player, sl);
        float strength = strength(player);
        paintThen(player, sl, ObserverFx.Painting.GIANTS_FIST, spot, () -> hangSeen(player, sl, spot,
                ObserverFx.Painting.GIANTS_FIST, "observer_giants_fist",
                (hung, victim) -> {
                    float power = hung.power() * strength;
                    Vec3 ground = Spells.groundAt(player, sl, victim.position().add(0, 0.5, 0));
                    ObserverFx.giantsFist(sl, ground, FIST_RADIUS - 1);
                    Later.run(sl, ObserverFx.GIANT_FIST_LANDS, () -> {
                        if (!Spells.casterStillHere(player, sl)) return;
                        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, FIST_RADIUS)) {
                            Spells.strike(player, enemy, FIST_DAMAGE * power);
                            Weight.put(enemy, FIST_HEAVY, FIST_HEAVY_TICKS);
                            Root.apply(enemy, FIST_ROOT, RootRestriction.MOVE);
                        }
                    });
                }, null));
        return AbilityWheel.Result.DONE;
    }
}
