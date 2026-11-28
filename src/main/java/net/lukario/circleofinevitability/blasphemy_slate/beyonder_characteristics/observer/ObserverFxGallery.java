package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Craft;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Figure;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Painting;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.*;

/**
 * /observerfx: every Observer look and sound played in front of you, so they can be checked before (and after) the
 * ability code exists. Game masters only.
 *
 *   /observerfx all            every display one after another (/observerfx all 2 = twice over)
 *   /observerfx giants_fist    just that one (TAB lists the names)
 *
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none. Each display is a
 * Case: a name, how long it runs, whether it is shown on you, and what it plays. To add one, add a Case to CASES.
 * The same calls are what the ability code will make, so each Case is also a worked example of one ability.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class ObserverFxGallery {

    private ObserverFxGallery() {}

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    /** Where a canvas hangs for a display: 2 blocks up at the spot, its front toward the viewer. */
    private static Vec3 canvasAt(Vec3 spot) {
        return spot.add(0, 2, 0);
    }

    private static Vec3 toward(Vec3 from, LivingEntity viewer) {
        return viewer.getEyePosition().subtract(from).multiply(1, 0, 1).normalize();
    }

    /** A Painting painted, hung, stared at, taking hold, then spent. */
    private static Case painting(Painting painting) {
        return new Case("painting_" + painting.name().toLowerCase(Locale.ROOT), 110, false, (sl, v, t, spot) -> {
            Vec3 canvas = canvasAt(spot);
            String tag = tagFor("canvas");
            paint(sl, v, canvas, painting, PAINT_TICKS);
            Later.run(sl, PAINT_TICKS, () -> hang(sl, painting, canvas, toward(canvas, v), 80, tag));
            LivingEntity looker = t == v ? v : t;
            for (int i = 0; i < 8; i++) {
                float progress = i / 7f;
                Later.run(sl, PAINT_TICKS + 20 + i * 5, () -> paintingStare(sl, painting, canvas, looker, progress));
            }
            Later.run(sl, PAINT_TICKS + 62, () -> paintingSeen(sl, painting, canvas, looker));
            Later.run(sl, PAINT_TICKS + 80, () -> paintingEnd(sl, painting, canvas, tag, true));
        });
    }

    private static List<LivingEntity> nearby(ServerLevel sl, Player viewer, double radius) {
        List<LivingEntity> list = new ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(radius))) {
            if (e != viewer && e.isAlive()) list.add(e);
        }
        if (list.isEmpty()) list.add(viewer);
        return list;
    }

    private static final List<Case> CASES = buildCases();

    private static List<Case> buildCases() {
        List<Case> cases = new ArrayList<>();
        for (Painting p : Painting.values()) {
            if (p != Painting.HIDE_IN_PAINTING && p != Painting.HIDE_IN_PAINTING_EMPTY) cases.add(painting(p));
        }
        cases.addAll(List.of(
                new Case("gallery_fold", 50, false, (sl, v, t, spot) -> {
                    Vec3 canvas = canvasAt(spot);
                    String tag = tagFor("fold");
                    hang(sl, Painting.GOLDEN_SUN, canvas, toward(canvas, v), 60, tag);
                    Later.run(sl, 30, () -> foldPainting(sl, Painting.GOLDEN_SUN, canvas, v, tag));
                }),

                // ---------- Observation
                new Case("observation", 120, false, (sl, v, t, spot) -> {
                    if (!(v instanceof ServerPlayer viewer) || t == v) return;
                    observed(viewer, t, 1, 110);
                    for (int i = 0; i < 10; i++) Later.run(sl, i * 10, () -> staring(viewer, t));
                    Later.run(sl, 40, () -> observationRises(viewer, t, 2));
                    Later.run(sl, 80, () -> observationRises(viewer, t, 3));
                    Later.run(sl, 100, () -> heldInSight(sl, t));
                }),
                new Case("not_watched_enough", 20, true, (sl, v, t, spot) -> {
                    if (v instanceof ServerPlayer viewer) notWatchedEnough(viewer);
                }),

                // ---------- Key 1 Territory
                new Case("totem_territory", 160, false, (sl, v, t, spot) -> {
                    String ritual = tagFor("ritual");
                    String tag = tagFor("totem");
                    totemRitual(sl, v, 40, ritual);
                    Later.run(sl, 40, () -> totem(sl, spot, 8, 110, tag));
                    Later.run(sl, 40 + TOTEM_RISEN, () -> territoryShell(sl, spot, 8, 90, tag));
                    for (int i = 0; i < 4; i++) Later.run(sl, 60 + i * 20, () -> territoryPulse(sl, spot, 8));
                    Later.run(sl, 90, () -> territoryShellBlock(sl, spot.add(8, 1, 0)));
                    if (v instanceof ServerPlayer owner && t != v) Later.run(sl, 100, () -> intruder(owner, t));
                    Later.run(sl, 150, () -> territoryEnd(sl, spot, tag));
                }),
                new Case("totem_ritual_cancel", 40, true, (sl, v, t, spot) -> {
                    String tag = tagFor("ritual");
                    totemRitual(sl, v, 60, tag);
                    Later.run(sl, 25, () -> totemRitualCancel(sl, v, tag));
                }),
                new Case("borrowed_power", 130, false, (sl, v, t, spot) -> {
                    Vec3 look = Spells.flatLook(v);
                    borrow(sl, v, spot, Craft.GALE, false);
                    Later.run(sl, 10, () -> gale(sl, v, 10));
                    Later.run(sl, 30, () -> borrow(sl, v, spot, Craft.EARTH_SPIKE, true));
                    Later.run(sl, 40, () -> earthSpikes(sl, v.position(), look, 10));
                    Later.run(sl, 60, () -> borrow(sl, v, spot, Craft.WATER_LASH, false));
                    Later.run(sl, 68, () -> waterLash(sl, v, t == v ? spot.add(0, 1, 0) : chestOf(t)));
                    String thorns = tagFor("thorns");
                    Later.run(sl, 85, () -> borrow(sl, v, spot, Craft.THORN_SNARE, false));
                    Later.run(sl, 92, () -> thornSnare(sl, spot, 3, 30, thorns));
                    Later.run(sl, 122, () -> thornSnareEnd(sl, spot, thorns));
                }),
                new Case("beast_call", 80, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("beast");
                    borrow(sl, v, chestOf(t), Craft.BEAST_CALL, false);
                    beastCall(sl, v, t, 70, tag);
                    Later.run(sl, 70, () -> beastCallEnd(sl, t, tag));
                }),
                new Case("ink_servants", 90, true, (sl, v, t, spot) -> {
                    inkServantsArrive(sl, v);
                    String tag = tagFor("servants");
                    inkServantLooks(sl, v, 80, tag);
                    if (t != v) for (int i = 0; i < 3; i++) {
                        Vec3 from = v.position().add(servantOffset(i));
                        Later.run(sl, 30 + i * 12, () -> servantStrike(sl, from, t));
                    }
                    Later.run(sl, 80, () -> inkServantsLeave(sl, v));
                }),
                new Case("painting_world", 160, false, (sl, v, t, spot) -> {
                    String tag = tagFor("world");
                    paintingWorld(sl, spot, 10, 150, true, tag);
                    Later.run(sl, WORLD_OPEN + 30, () -> worldBarrierHit(sl, spot.add(10, 1.5, 0)));
                    Later.run(sl, WORLD_OPEN + 60, () -> worldBarrierHit(sl, spot.add(0, 1.5, -10)));
                    Later.run(sl, 150, () -> paintingWorldEnd(sl, spot, 10, tag));
                }),

                // ---------- Key 2 Sight
                new Case("true_sight", 100, true, (sl, v, t, spot) -> {
                    if (!(v instanceof ServerPlayer viewer)) return;
                    String tag = tagFor("sight");
                    trueSight(sl, viewer, 90, tag);
                    if (t != v) Later.run(sl, 15, () -> trueSightSees(viewer, t));
                    Later.run(sl, 30, () -> trueSightThread(viewer, viewer.getEyePosition(), spot.add(0, 1, 0)));
                    if (t != v) Later.run(sl, 45, () -> trueSightWard(viewer, t));
                    Later.run(sl, 60, () -> trueSightTrap(viewer, spot));
                    Later.run(sl, 90, () -> trueSightEnd(sl, viewer, tag));
                }),
                new Case("many_angles", 100, true, (sl, v, t, spot) -> {
                    if (!(v instanceof ServerPlayer viewer)) return;
                    String tag = tagFor("angles");
                    manyAngles(sl, viewer, 16, 90, tag);
                    if (t != v) for (int i = 0; i < 4; i++) {
                        boolean atViewer = i % 2 == 0;
                        Later.run(sl, 20 + i * 15, () -> manyAnglesLine(viewer, t, atViewer ? viewer.getEyePosition() : spot, atViewer));
                    }
                    Later.run(sl, 90, () -> manyAnglesEnd(sl, viewer, tag));
                }),
                new Case("read_intent", 40, false, (sl, v, t, spot) -> readIntent(sl, v, t == v ? spot.add(0, 1, 0) : chestOf(t))),

                // ---------- Key 3 Canvas
                new Case("riot", 110, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("riot");
                    riot(sl, t, 100, tag);
                    for (int i = 1; i <= 4; i++) {
                        int m = i;
                        Later.run(sl, i * 20, () -> madness(sl, t, m));
                    }
                    Later.run(sl, 95, () -> madnessCollapse(sl, t, tag));
                    Later.run(sl, 100, () -> riotEnd(sl, t, tag));
                }),
                new Case("itching", 90, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("itch");
                    itching(sl, t, 80, tag);
                    Later.run(sl, 80, () -> itchingEnd(sl, t, tag));
                }),
                new Case("golden_sun", 120, false, (sl, v, t, spot) -> {
                    String tag = tagFor("sun");
                    goldenSun(sl, spot, 6, 110, tag);
                    for (int i = 0; i < 4; i++) Later.run(sl, 20 + i * 20, () -> sunWarm(sl, v));
                    if (t != v) for (int i = 0; i < 3; i++) Later.run(sl, 30 + i * 25, () -> sunBurn(sl, t));
                    Later.run(sl, 70, () -> exorcise(sl, spot.add(1, 1, 1)));
                    Later.run(sl, 110, () -> goldenSunEnd(sl, spot, tag));
                }),
                new Case("boat_in_a_storm", 110, false, (sl, v, t, spot) -> {
                    String tag = tagFor("storm");
                    boatInAStorm(sl, spot, 6, 100, tag);
                    for (int i = 0; i < 4; i++) {
                        float yaw = i * 90f;
                        Later.run(sl, 15 + i * 20, () -> stormWave(sl, spot, 6, yaw));
                    }
                    Later.run(sl, 100, () -> boatInAStormEnd(sl, spot, tag));
                }),
                new Case("self_portrait", 90, false, (sl, v, t, spot) -> {
                    Vec3 canvas = canvasAt(spot);
                    String tag = tagFor("portrait");
                    hang(sl, Painting.SELF_PORTRAIT, canvas, toward(canvas, v), 80, tag);
                    Later.run(sl, 20, () -> selfPortraitLook(sl, canvas, toward(canvas, v)));
                    Later.run(sl, 45, () -> portraitEcho(sl, canvas, t == v ? spot.add(2, 1, 0) : chestOf(t)));
                    Later.run(sl, 80, () -> paintingEnd(sl, Painting.SELF_PORTRAIT, canvas, tag, false));
                }),
                new Case("giants_fist", 60, false, (sl, v, t, spot) -> giantsFist(sl, spot, 4)),

                // ---------- Key 4 Fiction
                new Case("painted_mark", 90, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("mark");
                    paintedMark(sl, v, t, 80, tag);
                    Later.run(sl, 40, () -> markDrive(sl, t, t.position().add(Spells.flatLook(v).scale(3))));
                    Later.run(sl, 80, () -> paintedMarkEnd(sl, t, tag));
                }),
                new Case("hide_in_painting", 90, false, (sl, v, t, spot) -> {
                    Vec3 canvas = canvasAt(spot);
                    String tag = tagFor("hide");
                    hideInPainting(sl, v, canvas, toward(canvas, v), 70, tag);
                    Later.run(sl, 70, () -> hideInPaintingEnd(sl, v, canvas, toward(canvas, v), tag));
                }),
                new Case("painted_face", 70, true, (sl, v, t, spot) -> {
                    String tag = tagFor("face");
                    paintedFace(sl, v, 60, tag);
                    Later.run(sl, 60, () -> paintedFaceEnd(sl, v, tag));
                }),
                new Case("manifest_knight", 110, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("knight");
                    manifest(sl, t, Figure.KNIGHT, 1f, 100, tag);
                    for (int i = 0; i < 3; i++) {
                        int left = 100 - MANIFEST_READY - 10 - i * 20;
                        Later.run(sl, MANIFEST_READY + 10 + i * 20, () -> knightStrike(sl, t, v, tag, left));
                    }
                    Later.run(sl, 100, () -> manifestFade(sl, t, tag));
                }),
                new Case("manifest_archer", 110, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("archer");
                    manifest(sl, t, Figure.ARCHER, 0.5f, 100, tag);
                    for (int i = 0; i < 3; i++) {
                        int left = 100 - MANIFEST_READY - 10 - i * 20;
                        Later.run(sl, MANIFEST_READY + 10 + i * 20, () -> archerShot(sl, t, chestOf(v), tag, left));
                    }
                    Later.run(sl, 100, () -> manifestFade(sl, t, tag));
                }),
                new Case("fantasy_double", 90, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("double");
                    fantasyDouble(sl, v, t, 80, tag);
                    for (int i = 0; i < 4; i++) Later.run(sl, 15 + i * 15, () -> fantasyLink(sl, t, v));
                    Later.run(sl, 80, () -> fantasyDoubleEnd(sl, t, tag));
                }),

                // ---------- Key 5 Dimension
                new Case("phase", 60, true, (sl, v, t, spot) -> {
                    phaseIn(sl, v);
                    if (v instanceof ServerPlayer viewer) for (int i = 1; i < 10; i++) Later.run(sl, i * 5, () -> phaseTick(viewer));
                    Later.run(sl, 50, () -> phaseOut(sl, v));
                }),
                new Case("pixie_wings", WINGS_KEPT + 10, true, (sl, v, t, spot) -> {
                    String tag = tagFor("wings");
                    pixieWings(sl, v, WINGS_KEPT, tag);
                    Later.run(sl, WINGS_KEPT - 4, () -> pixieWingsEnd(sl, v, tag));
                }),
                new Case("painted_lightning", 50, false, (sl, v, t, spot) -> {
                    List<Vec3> to = new ArrayList<>();
                    for (LivingEntity e : nearby(sl, v, 12)) if (e != v) to.add(chestOf(e));
                    if (to.isEmpty()) to.add(spot.add(0, 1, 0));
                    paintedLightning(sl, v, to);
                    if (to.size() > 1) Later.run(sl, 6, () -> boltJump(sl, to.get(0), to.get(1)));
                }),
                new Case("passage", 40, false, (sl, v, t, spot) -> {
                    LivingEntity who = t == v ? v : t;
                    Vec3 from = who.position();
                    passage(sl, who, from, from.add(Spells.rightOf(v).scale(4)));
                }),
                new Case("hostel", 70, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    hostelLodge(sl, v, t);
                    Later.run(sl, 50, () -> hostelRelease(sl, v, spot));
                }),
                new Case("shadow_form", 150, true, (sl, v, t, spot) -> {
                    String tag = tagFor("shadow");
                    shadowForm(sl, v, 120, true, tag);
                    for (int i = 0; i < 5; i++) Later.run(sl, SHADOW_READY + i * 15, () -> shadowAfterimage(sl, v));
                    Later.run(sl, 70, () -> ascension(sl, v, 2));
                    Later.run(sl, 120, () -> shadowFormEnd(sl, v, tag));
                    String weak = tagFor("weak");
                    Later.run(sl, 120, () -> shadowWeakness(sl, v, 25, weak));
                }),
                new Case("observer_form", 110, true, (sl, v, t, spot) -> {
                    String tag = tagFor("observer");
                    observerForm(sl, v, 100, tag);
                    Later.run(sl, 100, () -> observerFormEnd(sl, v, tag));
                }),

                // ---------- Key 6 Strings
                new Case("taut_chord", TAUT_PLAYS + 20, false, (sl, v, t, spot) -> {
                    List<LivingEntity> caught = nearby(sl, v, 10);
                    caught.remove(v);
                    if (caught.isEmpty()) return;
                    tautChord(sl, v, caught);
                    Later.run(sl, TAUT_PLAYS, () -> tautChordPlay(sl, v, caught));
                }),
                new Case("resonance", 90, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("resonance");
                    resonance(sl, v, t, 80, tag);
                    for (int i = 2; i <= 5; i++) {
                        int count = i;
                        Later.run(sl, (i - 1) * 12, () -> resonanceAdd(sl, t, count, 80, tag));
                    }
                    Later.run(sl, 70, () -> resonanceBurst(sl, t, 4, 5, tag));
                }),
                new Case("harmony", 80, true, (sl, v, t, spot) -> {
                    List<LivingEntity> allies = nearby(sl, v, 8);
                    harmony(sl, v, 8, allies);
                    String tag = tagFor("harmony");
                    for (LivingEntity a : allies) harmonyArmed(sl, a, 60, tag);
                    Later.run(sl, 60, () -> {
                        for (LivingEntity a : allies) harmonySpent(sl, a, tag);
                    });
                }),
                new Case("dissonance", 50, true, (sl, v, t, spot) -> {
                    dissonance(sl, v, 8);
                    if (t != v) Later.run(sl, 6, () -> dissonant(sl, t));
                }),
                new Case("shadow_from_above", 50, false, (sl, v, t, spot) -> {
                    if (t != v) shadowFromAbove(sl, t);
                    Later.run(sl, 15, () -> shadowBladesRise(sl, spot, 3));
                }),
                new Case("fold_space", 40, false, (sl, v, t, spot) -> {
                    Vec3 to = t == v ? spot : t.position().subtract(Spells.flatLook(v));
                    foldSpace(sl, v, v.position(), to);
                    if (t != v) Later.run(sl, FOLD_ARRIVES + 2, () -> foldStruck(sl, t));
                }),

                // ---------- Key 7 Sublunary
                new Case("foregone_loss", 90, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("loss");
                    foregoneLoss(sl, v, t, 80, tag);
                    Later.run(sl, 60, () -> foregoneFall(sl, t, tag));
                }),
                new Case("foregone_spared", 60, false, (sl, v, t, spot) -> {
                    if (t == v) return;
                    String tag = tagFor("spared");
                    foregoneLoss(sl, v, t, 50, tag);
                    Later.run(sl, 40, () -> foregoneSpared(sl, t, tag));
                }),
                new Case("foregone_survival", 70, true, (sl, v, t, spot) -> {
                    String tag = tagFor("survival");
                    foregoneSurvival(sl, v, v, 60, tag);
                    Later.run(sl, 45, () -> wardSpent(sl, v, tag));
                }),
                new Case("revival", 120, true, (sl, v, t, spot) -> {
                    String tag = tagFor("revival");
                    revivalRitual(sl, v, 60, tag);
                    Later.run(sl, 60, () -> revivalArmed(sl, v, tag));
                    Later.run(sl, 90, () -> higherRevival(sl, v, v.position()));
                }),
                new Case("bloom", 110, true, (sl, v, t, spot) -> {
                    String tag = tagFor("bloom");
                    bloom(sl, v, 8, 100, tag);
                    for (int i = 0; i < 4; i++) Later.run(sl, EYE_OPEN + i * 20, () -> {
                        for (LivingEntity a : nearby(sl, v, 8)) bloomOn(sl, a);
                    });
                    Later.run(sl, 100, () -> bloomEnd(sl, v, tag));
                }),
                new Case("fall_silent", 110, true, (sl, v, t, spot) -> {
                    String tag = tagFor("silent");
                    fallSilent(sl, v, 8, 100, tag);
                    String hushed = tagFor("hushed");
                    if (t != v) {
                        Later.run(sl, EYE_OPEN, () -> silenced(sl, t, 60, hushed));
                        Later.run(sl, EYE_OPEN + 60, () -> woken(sl, t, hushed));
                    }
                    Later.run(sl, 100, () -> fallSilentEnd(sl, v, tag));
                }),
                new Case("erase", 80, false, (sl, v, t, spot) -> {
                    if (t != v) {
                        erase(sl, v, t);
                        Vec3 at = t.position();
                        Later.run(sl, ERASE_GONE + 40, () -> erasedReturn(sl, at));
                    }
                }),
                new Case("erase_area", 60, false, (sl, v, t, spot) -> {
                    eraseArea(sl, spot, 5);
                    Later.run(sl, 30, () -> unmade(sl, spot.add(0, 1, 0)));
                }),

                // ---------- Passives
                new Case("passives", 90, true, (sl, v, t, spot) -> {
                    if (v instanceof ServerPlayer viewer) {
                        spiritChime(viewer, spot.add(0, 1, 0));
                        Later.run(sl, 15, () -> painterEye(viewer, spot.add(0, 1, 0)));
                        if (t != v) {
                            Later.run(sl, 30, () -> prayer(viewer, t));
                            Later.run(sl, 45, () -> eyesEverywhere(viewer, t));
                            Later.run(sl, 60, () -> sublunarySight(viewer, t));
                        }
                    }
                    Later.run(sl, 70, () -> featherfall(sl, v));
                    Later.run(sl, 80, () -> foreseen(sl, v, spot.add(0, 1, 0)));
                }),
                new Case("statuses", 100, true, (sl, v, t, spot) -> {
                    for (Status s : Status.values()) status(v, s, 90);
                })));
        return List.copyOf(cases);
    }

    /** The names /observerfx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** The nearest living thing within 12 blocks that is not the viewer, or the viewer. */
    private static LivingEntity nearestTarget(ServerLevel sl, Player viewer) {
        LivingEntity best = viewer;
        double bestDistance = 12 * 12;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(12))) {
            if (e == viewer || !e.isAlive()) continue;
            double d = e.distanceToSqr(viewer);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    /** One display, 6 blocks in front of the viewer. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            Vec3 spot = viewer.position().add(ahead(viewer.getYRot()).scale(6));
            c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
            return true;
        }
        return false;
    }

    /** Every display one after the other (those on a spot in a ring 8 blocks out), 'rounds' times over. */
    public static void gallery(ServerLevel sl, Player viewer, int rounds) {
        List<Case> placed = new ArrayList<>();
        for (Case c : CASES) if (!c.onViewer()) placed.add(c);
        int at = 0;
        for (int round = 0; round < rounds; round++) {
            for (Case c : CASES) {
                int place = placed.indexOf(c);
                Later.run(sl, at, () -> {
                    if (!viewer.isAlive() || viewer.level() != sl) return;
                    float out = viewer.getYRot() + Math.max(0, place) * 360f / Math.max(1, placed.size());
                    Vec3 spot = viewer.position().add(ahead(out).scale(8));
                    c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
                });
                at += c.ticks() + 10;
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("observerfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all")
                        .executes(ctx -> showAll(ctx.getSource(), 1))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> showAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "rounds")))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            if (!show(ctx.getSource().getLevel(), player, name)) {
                                ctx.getSource().sendFailure(Component.literal("No display called " + name + ". There are: " + String.join(", ", names())));
                                return 0;
                            }
                            return 1;
                        })));
    }

    private static int showAll(CommandSourceStack source, int rounds) throws CommandSyntaxException {
        gallery(source.getLevel(), source.getPlayerOrException(), rounds);
        return 1;
    }
}
